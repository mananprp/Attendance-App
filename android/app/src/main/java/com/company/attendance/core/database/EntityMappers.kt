package com.company.attendance.core.database

import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EntityMappers {
    private val json = Json { ignoreUnknownKeys = true }

    fun UserLocalEntity.toUserSession(token: String): com.company.attendance.core.model.UserSession {
        return com.company.attendance.core.model.UserSession(
            userId = userId,
            displayName = displayName,
            token = token,
            photoChangeUsed = photoChangeUsed,
            hasPhoto = hasPhoto
        )
    }

    fun AppConfigEntity.toDomain(): AppConfig {
        val windows = try {
            json.decodeFromString<List<String>>(syncWindowsJson)
        } catch (_: Exception) {
            listOf("16:00", "00:00")
        }
        return AppConfig(
            officeLat = officeLat,
            officeLng = officeLng,
            radiusMeters = radiusMeters,
            timezone = timezone,
            syncWindows = windows,
            syncJitterMinutes = syncJitterMinutes,
            matchThreshold = matchThreshold,
            lastFetchedAt = lastFetchedAt
        )
    }

    fun AppConfig.toEntity(id: Int = 1): AppConfigEntity {
        val windowsJson = json.encodeToString(syncWindows)
        return AppConfigEntity(
            id = id,
            officeLat = officeLat,
            officeLng = officeLng,
            radiusMeters = radiusMeters,
            timezone = timezone,
            syncWindowsJson = windowsJson,
            syncJitterMinutes = syncJitterMinutes,
            matchThreshold = matchThreshold,
            lastFetchedAt = lastFetchedAt
        )
    }

    fun PunchEntity.toDomain(): Punch {
        return Punch(
            punchUuid = punchUuid,
            userId = userId,
            dateLocal = dateLocal,
            type = try { PunchType.valueOf(type) } catch (_: Exception) { PunchType.IN },
            punchTimeUtcEpochMs = punchTimeUtc,
            timeSource = try { TimeSource.valueOf(timeSource) } catch (_: Exception) { TimeSource.DEVICE },
            timeUnverified = timeUnverified,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            matchScore = matchScore,
            imagePath = imagePath,
            syncStatus = try { SyncStatus.valueOf(syncStatus) } catch (_: Exception) { SyncStatus.PENDING },
            syncAttempts = syncAttempts,
            createdAtEpochMs = createdAt
        )
    }

    fun Punch.toEntity(): PunchEntity {
        return PunchEntity(
            punchUuid = punchUuid,
            userId = userId,
            dateLocal = dateLocal,
            type = type.name,
            punchTimeUtc = punchTimeUtcEpochMs,
            timeSource = timeSource.name,
            timeUnverified = timeUnverified,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            matchScore = matchScore,
            imagePath = imagePath,
            syncStatus = syncStatus.name,
            syncAttempts = syncAttempts,
            createdAt = createdAtEpochMs
        )
    }

    fun DaySummaryEntity.toDomain(): DaySummary {
        return DaySummary(
            userId = userId,
            dateLocal = dateLocal,
            inTimeUtcEpochMs = inTimeUtc,
            outTimeUtcEpochMs = outTimeUtc,
            status = try { DayStatus.valueOf(status) } catch (_: Exception) { DayStatus.NO_RECORD },
            timeUnverified = timeUnverified
        )
    }

    fun DaySummary.toEntity(): DaySummaryEntity {
        return DaySummaryEntity(
            userId = userId,
            dateLocal = dateLocal,
            inTimeUtc = inTimeUtcEpochMs,
            outTimeUtc = outTimeUtcEpochMs,
            status = status.name,
            timeUnverified = timeUnverified
        )
    }
}
