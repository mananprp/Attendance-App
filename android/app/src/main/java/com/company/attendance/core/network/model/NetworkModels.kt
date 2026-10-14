package com.company.attendance.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String,
    @SerialName("details") val details: Map<String, String>? = null
)

@Serializable
data class LoginRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("password") val password: String
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("role") val role: String = "EMPLOYEE",
    @SerialName("site_code") val siteCode: String = "DEFAULT_OFFICE",
    @SerialName("photo_change_used") val photoChangeUsed: Boolean,
    @SerialName("has_photo") val hasPhoto: Boolean,
    @SerialName("active_photo_version") val activePhotoVersion: Int? = null
)

@Serializable
data class LoginResponseDto(
    @SerialName("token") val token: String,
    @SerialName("user") val user: UserProfileDto
)

@Serializable
data class AdminSiteDto(
    @SerialName("office_id") val officeId: String,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("radius_m") val radiusM: Int,
    @SerialName("timezone") val timezone: String,
    @SerialName("match_threshold") val matchThreshold: Float
)

@Serializable
data class AdminUserDto(
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("role") val role: String,
    @SerialName("site_code") val siteCode: String,
    @SerialName("site_name") val siteName: String,
    @SerialName("status") val status: String,
    @SerialName("photo_change_used") val photoChangeUsed: Boolean
)

@Serializable
data class CreateUserRequestDto(
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("password") val password: String,
    @SerialName("site_code") val siteCode: String = "DEFAULT_OFFICE",
    @SerialName("role") val role: String = "EMPLOYEE"
)

@Serializable
data class CreateSiteRequestDto(
    @SerialName("office_id") val officeId: String,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("radius_m") val radiusM: Int = 100,
    @SerialName("timezone") val timezone: String = "Asia/Kolkata",
    @SerialName("match_threshold") val matchThreshold: Float = 0.38f
)

@Serializable
data class UpdateUserSiteRequestDto(
    @SerialName("site_code") val siteCode: String
)

@Serializable
data class UpdateUserRoleRequestDto(
    @SerialName("role") val role: String
)

@Serializable
data class AppConfigResponseDto(
    @SerialName("office_lat") val officeLat: Double,
    @SerialName("office_lng") val officeLng: Double,
    @SerialName("radius_m") val radiusM: Int,
    @SerialName("timezone") val timezone: String,
    @SerialName("sync_windows") val syncWindows: List<String>,
    @SerialName("sync_jitter_minutes") val syncJitterMinutes: Int,
    @SerialName("match_threshold") val matchThreshold: Float
)

@Serializable
data class PhotoUploadResponseDto(
    @SerialName("version") val version: Int,
    @SerialName("photo_url") val photoUrl: String,
    @SerialName("photo_change_used") val photoChangeUsed: Boolean
)

@Serializable
data class PhotoDetailResponseDto(
    @SerialName("version") val version: Int,
    @SerialName("photo_url") val photoUrl: String,
    @SerialName("uploaded_at") val uploadedAt: String,
    @SerialName("hash") val hash: String
)

@Serializable
data class SyncPunchItemDto(
    @SerialName("punch_uuid") val punchUuid: String,
    @SerialName("user_id") val userId: String,
    @SerialName("date_local") val dateLocal: String,
    @SerialName("type") val type: String,
    @SerialName("punch_time_utc") val punchTimeUtc: String,
    @SerialName("time_source") val timeSource: String,
    @SerialName("time_unverified") val timeUnverified: Boolean,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("distance_m") val distanceM: Float,
    @SerialName("match_score") val matchScore: Float,
    @SerialName("image_file_name") val imageFileName: String? = null,
    @SerialName("location_type") val locationType: String = "OFFICE",
    @SerialName("shift_type") val shiftType: String = "GENERAL"
)

@Serializable
data class PunchSyncResultDto(
    @SerialName("punch_uuid") val punchUuid: String,
    @SerialName("status") val status: String, // ACCEPTED, DUPLICATE, REJECTED
    @SerialName("reason") val reason: String? = null
)

@Serializable
data class SyncBatchResponseDto(
    @SerialName("batch_id") val batchId: String,
    @SerialName("processed_count") val processedCount: Int,
    @SerialName("results") val results: List<PunchSyncResultDto>
)

@Serializable
data class DaySummaryDto(
    @SerialName("date_local") val dateLocal: String,
    @SerialName("in_time") val inTime: String? = null,
    @SerialName("out_time") val outTime: String? = null,
    @SerialName("status") val status: String, // COMPLETE, MISSED_PUNCH, NO_RECORD
    @SerialName("time_unverified") val timeUnverified: Boolean = false,
    @SerialName("shift_type") val shiftType: String = "GENERAL"
)

@Serializable
data class MonthlyAttendanceResponseDto(
    @SerialName("month") val month: String,
    @SerialName("days") val days: List<DaySummaryDto>,
    @SerialName("punches") val punches: List<SyncPunchItemDto>
)

@Serializable
data class ModelManifestItemDto(
    @SerialName("name") val name: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("sha256") val sha256: String,
    @SerialName("download_url") val downloadUrl: String
)

@Serializable
data class ModelManifestResponseDto(
    @SerialName("models") val models: List<ModelManifestItemDto>,
    @SerialName("total_size_bytes") val totalSizeBytes: Long
)
