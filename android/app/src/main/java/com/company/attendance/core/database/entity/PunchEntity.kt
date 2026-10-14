package com.company.attendance.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "punches",
    indices = [
        Index(value = ["user_id", "date_local"], name = "index_punches_user_date"),
        Index(value = ["sync_status"], name = "index_punches_sync_status")
    ]
)
data class PunchEntity(
    @PrimaryKey
    @ColumnInfo(name = "punch_uuid")
    val punchUuid: String, // UUID v4

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "date_local")
    val dateLocal: String, // YYYY-MM-DD

    @ColumnInfo(name = "type")
    val type: String, // IN, OUT

    @ColumnInfo(name = "punch_time_utc")
    val punchTimeUtc: Long, // Epoch ms (UTC)

    @ColumnInfo(name = "time_source")
    val timeSource: String, // NTP, DEVICE

    @ColumnInfo(name = "time_unverified")
    val timeUnverified: Boolean,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "distance_m")
    val distanceMeters: Float,

    @ColumnInfo(name = "match_score")
    val matchScore: Float,

    @ColumnInfo(name = "image_path")
    val imagePath: String?,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED

    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "location_type")
    val locationType: String = "OFFICE", // OFFICE, NEW_LOCATION

    @ColumnInfo(name = "shift_type")
    val shiftType: String = "GENERAL" // GENERAL, EVENING, NIGHT
)

