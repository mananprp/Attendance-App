package com.company.attendance.core.model

import java.time.Instant
import java.time.LocalDate

enum class PunchType {
    IN,
    OUT
}

enum class TimeSource {
    NTP,
    DEVICE
}

enum class DayStatus {
    COMPLETE,
    MISSED_PUNCH,
    NO_RECORD
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

data class UserSession(
    val userId: String,
    val displayName: String,
    val token: String,
    val role: String = "EMPLOYEE",
    val siteCode: String = "DEFAULT_OFFICE",
    val photoChangeUsed: Boolean,
    val hasPhoto: Boolean,
    val activePhotoVersion: Int? = null
)

data class AppConfig(
    val officeLat: Double,
    val officeLng: Double,
    val radiusMeters: Int,
    val timezone: String,
    val syncWindows: List<String>,
    val syncJitterMinutes: Int,
    val matchThreshold: Float,
    val lastFetchedAt: Long = System.currentTimeMillis()
)

enum class ShiftType {
    GENERAL, // 09:00 - 18:00 (cutoff 03:00 AM next day)
    EVENING, // 14:00 - 23:00 (cutoff 06:00 AM next day)
    NIGHT    // 21:00 - 06:00 (+1 day) (cutoff 12:00 PM next day)
}

data class Punch(
    val punchUuid: String,
    val userId: String,
    val dateLocal: String, // YYYY-MM-DD
    val type: PunchType,
    val punchTimeUtcEpochMs: Long,
    val timeSource: TimeSource,
    val timeUnverified: Boolean,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float,
    val matchScore: Float,
    val imagePath: String?,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val syncAttempts: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val locationType: String = "OFFICE", // OFFICE, NEW_LOCATION
    val shiftType: ShiftType = ShiftType.GENERAL
)

data class DaySummary(
    val userId: String,
    val dateLocal: String, // YYYY-MM-DD
    val inTimeUtcEpochMs: Long?,
    val outTimeUtcEpochMs: Long?,
    val status: DayStatus,
    val timeUnverified: Boolean = false,
    val anchorLat: Double? = null,
    val anchorLng: Double? = null,
    val locationType: String = "OFFICE", // OFFICE, NEW_LOCATION
    val shiftType: ShiftType = ShiftType.GENERAL
)

data class TrustedTime(
    val epochMillis: Long,
    val source: TimeSource,
    val isUnverified: Boolean,
    val localDate: LocalDate,
    val timezoneId: String
)
