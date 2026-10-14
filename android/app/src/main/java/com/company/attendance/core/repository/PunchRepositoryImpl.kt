package com.company.attendance.core.repository

import com.company.attendance.core.database.EntityMappers.toDomain
import com.company.attendance.core.database.EntityMappers.toEntity
import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.ShiftType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TrustedTime
import com.company.attendance.core.util.ShiftEvaluator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PunchRepositoryImpl @Inject constructor(
    private val punchDao: PunchDao,
    private val daySummaryDao: DaySummaryDao
) : PunchRepository {

    override fun observeDaySummary(userId: String, dateLocal: String): Flow<DaySummary?> {
        return daySummaryDao.getDaySummary(userId, dateLocal).map { it?.toDomain() }
    }

    override suspend fun getDaySummary(userId: String, dateLocal: String): DaySummary? {
        return daySummaryDao.getDaySummaryDirect(userId, dateLocal)?.toDomain()
    }

    override fun observeMonthSummaries(userId: String, yearMonth: String): Flow<List<DaySummary>> {
        return daySummaryDao.getMonthSummaries(userId, yearMonth).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPendingPunches(userId: String): List<Punch> {
        return punchDao.getPendingPunches(userId).map { it.toDomain() }
    }

    override suspend fun recordPunch(
        userId: String,
        punchType: PunchType,
        time: TrustedTime,
        latitude: Double,
        longitude: Double,
        distanceMeters: Float,
        matchScore: Float,
        capturedImagePath: String?,
        locationType: String
    ): Result<Punch> {
        return try {
            val zoneId = ZoneId.of(time.timezoneId)
            val currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time.epochMillis), zoneId)

            val todayStr = time.localDate.toString()
            val yesterdayStr = time.localDate.minusDays(1).toString()

            val openYesterday = daySummaryDao.getDaySummaryDirect(userId, yesterdayStr)?.takeIf {
                it.inTimeUtc != null && it.outTimeUtc == null &&
                !ShiftEvaluator.isSessionExpired(
                    try { ShiftType.valueOf(it.shiftType) } catch (_: Exception) { ShiftType.GENERAL },
                    LocalDate.parse(it.dateLocal),
                    currentDateTime
                )
            }
            val openToday = daySummaryDao.getDaySummaryDirect(userId, todayStr)?.takeIf {
                it.inTimeUtc != null && it.outTimeUtc == null
            }

            val (shiftType, workDateStr) = if (punchType == PunchType.OUT) {
                val active = openToday ?: openYesterday
                if (active != null) {
                    Pair(active.shiftType, active.dateLocal)
                } else {
                    val (evalShift, evalDate) = ShiftEvaluator.determineShift(currentDateTime)
                    Pair(evalShift.name, evalDate.toString())
                }
            } else {
                val (evalShift, evalDate) = ShiftEvaluator.determineShift(currentDateTime)
                Pair(evalShift.name, evalDate.toString())
            }

            val punchUuid = UUID.randomUUID().toString()

            // 1. Create and store PunchEntity
            val punchEntity = PunchEntity(
                punchUuid = punchUuid,
                userId = userId,
                dateLocal = workDateStr,
                type = punchType.name,
                punchTimeUtc = time.epochMillis,
                timeSource = time.source.name,
                timeUnverified = time.isUnverified,
                latitude = latitude,
                longitude = longitude,
                distanceMeters = distanceMeters,
                matchScore = matchScore,
                imagePath = capturedImagePath,
                syncStatus = SyncStatus.PENDING.name,
                syncAttempts = 0,
                createdAt = System.currentTimeMillis(),
                locationType = locationType,
                shiftType = shiftType
            )
            punchDao.insertPunch(punchEntity)

            // 2. Update DaySummary state machine
            val existingSummary = daySummaryDao.getDaySummaryDirect(userId, workDateStr)
            val updatedSummary = if (punchType == PunchType.IN) {
                if (existingSummary != null) {
                    existingSummary.copy(
                        inTimeUtc = existingSummary.inTimeUtc ?: time.epochMillis,
                        timeUnverified = existingSummary.timeUnverified || time.isUnverified,
                        anchorLat = if (locationType == "NEW_LOCATION") latitude else existingSummary.anchorLat,
                        anchorLng = if (locationType == "NEW_LOCATION") longitude else existingSummary.anchorLng,
                        locationType = if (locationType == "NEW_LOCATION") "NEW_LOCATION" else existingSummary.locationType,
                        shiftType = shiftType
                    )
                } else {
                    DaySummaryEntity(
                        userId = userId,
                        dateLocal = workDateStr,
                        inTimeUtc = time.epochMillis,
                        outTimeUtc = null,
                        status = DayStatus.NO_RECORD.name,
                        timeUnverified = time.isUnverified,
                        anchorLat = if (locationType == "NEW_LOCATION") latitude else null,
                        anchorLng = if (locationType == "NEW_LOCATION") longitude else null,
                        locationType = locationType,
                        shiftType = shiftType
                    )
                }
            } else {
                // Punch OUT completes the day
                if (existingSummary != null) {
                    existingSummary.copy(
                        outTimeUtc = time.epochMillis,
                        status = DayStatus.COMPLETE.name,
                        timeUnverified = existingSummary.timeUnverified || time.isUnverified
                    )
                } else {
                    DaySummaryEntity(
                        userId = userId,
                        dateLocal = workDateStr,
                        inTimeUtc = null,
                        outTimeUtc = time.epochMillis,
                        status = DayStatus.COMPLETE.name,
                        timeUnverified = time.isUnverified,
                        locationType = locationType,
                        shiftType = shiftType
                    )
                }
            }
            daySummaryDao.insertOrUpdate(updatedSummary)

            // 3. Resolve earlier days missed punches if this is next-day first punch
            resolvePastMissedPunches(userId, workDateStr)

            Result.success(punchEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolvePastMissedPunches(userId: String, currentDateLocal: String) {
        val unclosedSummaries = daySummaryDao.getUnclosedSummariesBeforeDate(userId, currentDateLocal)
        val currentDateTime = LocalDateTime.now()
        for (summary in unclosedSummaries) {
            val shift = try { ShiftType.valueOf(summary.shiftType) } catch (_: Exception) { ShiftType.GENERAL }
            val workDate = try { LocalDate.parse(summary.dateLocal) } catch (_: Exception) { LocalDate.parse(currentDateLocal).minusDays(1) }
            if (ShiftEvaluator.isSessionExpired(shift, workDate, currentDateTime)) {
                val resolved = summary.copy(
                    outTimeUtc = summary.inTimeUtc,
                    status = DayStatus.MISSED_PUNCH.name
                )
                daySummaryDao.insertOrUpdate(resolved)
            }
        }
    }

    override suspend fun markPunchesSynced(punchUuids: List<String>) {
        if (punchUuids.isNotEmpty()) {
            punchDao.markPunchesSynced(punchUuids)
        }
    }

    override suspend fun markPunchSyncFailed(punchUuid: String, attemptCount: Int) {
        punchDao.updateSyncStatus(punchUuid, SyncStatus.FAILED.name, attemptCount)
    }

    override suspend fun upsertDaySummaries(summaries: List<DaySummary>) {
        if (summaries.isEmpty()) return

        val mergedEntities = summaries.map { summary ->
            val existing = daySummaryDao.getDaySummaryDirect(summary.userId, summary.dateLocal)
            val localPunches = punchDao.getPunchesForDateDirect(summary.userId, summary.dateLocal)
            val serverEntity = summary.toEntity()

            val hasLocalOutPunch = localPunches.any { it.type == "OUT" }
            val localOutTime = existing?.outTimeUtc ?: localPunches.lastOrNull { it.type == "OUT" }?.punchTimeUtc

            if (existing != null || hasLocalOutPunch) {
                val resolvedOutTime = if (localOutTime != null && serverEntity.outTimeUtc == null) {
                    localOutTime
                } else {
                    serverEntity.outTimeUtc ?: localOutTime
                }

                val resolvedInTime = existing?.inTimeUtc
                    ?: localPunches.firstOrNull { it.type == "IN" }?.punchTimeUtc
                    ?: serverEntity.inTimeUtc

                val resolvedStatus = if (resolvedInTime != null && resolvedOutTime != null) {
                    DayStatus.COMPLETE.name
                } else if (serverEntity.status == DayStatus.MISSED_PUNCH.name && resolvedOutTime != null) {
                    DayStatus.COMPLETE.name
                } else {
                    serverEntity.status
                }

                DaySummaryEntity(
                    userId = summary.userId,
                    dateLocal = summary.dateLocal,
                    inTimeUtc = resolvedInTime,
                    outTimeUtc = resolvedOutTime,
                    status = resolvedStatus,
                    timeUnverified = (existing?.timeUnverified ?: false) || serverEntity.timeUnverified,
                    anchorLat = existing?.anchorLat ?: serverEntity.anchorLat,
                    anchorLng = existing?.anchorLng ?: serverEntity.anchorLng,
                    locationType = existing?.locationType ?: serverEntity.locationType
                )
            } else {
                serverEntity
            }
        }
        daySummaryDao.insertAll(mergedEntities)
    }
}
