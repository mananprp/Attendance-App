package com.company.attendance.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "day_summary",
    primaryKeys = ["user_id", "date_local"],
    indices = [
        Index(value = ["user_id", "date_local"], name = "index_day_summary_user_date")
    ]
)
data class DaySummaryEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "date_local")
    val dateLocal: String, // YYYY-MM-DD

    @ColumnInfo(name = "in_time_utc")
    val inTimeUtc: Long?,

    @ColumnInfo(name = "out_time_utc")
    val outTimeUtc: Long?,

    @ColumnInfo(name = "status")
    val status: String = "NO_RECORD", // COMPLETE, MISSED_PUNCH, NO_RECORD

    @ColumnInfo(name = "time_unverified")
    val timeUnverified: Boolean = false
)
