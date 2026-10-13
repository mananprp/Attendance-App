package com.company.attendance.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "office_lat")
    val officeLat: Double,

    @ColumnInfo(name = "office_lng")
    val officeLng: Double,

    @ColumnInfo(name = "radius_m")
    val radiusMeters: Int = 100,

    @ColumnInfo(name = "timezone")
    val timezone: String = "Asia/Kolkata",

    @ColumnInfo(name = "sync_windows_json")
    val syncWindowsJson: String = "[\"16:00\", \"00:00\"]",

    @ColumnInfo(name = "sync_jitter_minutes")
    val syncJitterMinutes: Int = 30,

    @ColumnInfo(name = "match_threshold")
    val matchThreshold: Float = 0.46f,

    @ColumnInfo(name = "last_fetched_at")
    val lastFetchedAt: Long = System.currentTimeMillis()
)
