package com.company.attendance.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracking_sessions")
data class TrackingSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "start_time_utc")
    val startTimeUtc: Long,

    @ColumnInfo(name = "end_time_utc")
    val endTimeUtc: Long? = null,

    @ColumnInfo(name = "total_distance_m")
    val totalDistanceM: Float = 0f,

    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)

@Entity(
    tableName = "tracking_points",
    indices = [
        Index(value = ["session_id", "timestamp_utc"], name = "index_tracking_points_session_time")
    ]
)
data class TrackingPointEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "accuracy_m")
    val accuracyM: Float?,

    @ColumnInfo(name = "timestamp_utc")
    val timestampUtc: Long,

    @ColumnInfo(name = "is_paused_gap")
    val isPausedGap: Boolean = false
)

@Entity(
    tableName = "tracking_stops",
    indices = [
        Index(value = ["session_id", "arrived_at_utc"], name = "index_tracking_stops_session_time")
    ]
)
data class TrackingStopEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "stop_name")
    val stopName: String?,

    @ColumnInfo(name = "arrived_at_utc")
    val arrivedAtUtc: Long
)
