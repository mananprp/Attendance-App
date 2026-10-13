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
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TrustedTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        capturedImagePath: String?
    ): Result<Punch> {
        return try {
            val dateStr = time.localDate.toString()
            val punchUuid = UUID.randomUUID().toString()

            // 1. Create and store PunchEntity
            val punchEntity = PunchEntity(
                punchUuid = punchUuid,
                userId = userId,
                dateLocal = dateStr,
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
                createdAt = System.currentTimeMillis()
            )
            punchDao.insertPunch(punchEntity)

            // 2. Update DaySummary state machine
            val existingSummary = daySummaryDao.getDaySummaryDirect(userId, dateStr)
            val updatedSummary = if (punchType == PunchType.IN) {
                if (existingSummary != null) {
                    existingSummary.copy(
                        inTimeUtc = existingSummary.inTimeUtc ?: time.epochMillis,
                        timeUnverified = existingSummary.timeUnverified || time.isUnverified
                    )
                } else {
                    DaySummaryEntity(
                        userId = userId,
                        dateLocal = dateStr,
                        inTimeUtc = time.epochMillis,
                        outTimeUtc = null,
                        status = DayStatus.NO_RECORD.name,
                        timeUnverified = time.isUnverified
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
                        dateLocal = dateStr,
                        inTimeUtc = null,
                        outTimeUtc = time.epochMillis,
                        status = DayStatus.COMPLETE.name,
                        timeUnverified = time.isUnverified
                    )
                }
            }
            daySummaryDao.insertOrUpdate(updatedSummary)

            // 3. Resolve earlier days missed punches if this is next-day first punch
            resolvePastMissedPunches(userId, dateStr)

            Result.success(punchEntity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resolvePastMissedPunches(userId: String, currentDateLocal: String) {
        val unclosedSummaries = daySummaryDao.getUnclosedSummariesBeforeDate(userId, currentDateLocal)
        for (summary in unclosedSummaries) {
            val resolved = summary.copy(
                outTimeUtc = summary.inTimeUtc,
                status = DayStatus.MISSED_PUNCH.name
            )
            daySummaryDao.insertOrUpdate(resolved)
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
        if (summaries.isNotEmpty()) {
            daySummaryDao.insertAll(summaries.map { it.toEntity() })
        }
    }
}
