package com.company.attendance.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.company.attendance.core.database.dao.AppConfigDao
import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.database.entity.UserLocalEntity

@Database(
    entities = [
        UserLocalEntity::class,
        PunchEntity::class,
        DaySummaryEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userLocalDao(): UserLocalDao
    abstract fun punchDao(): PunchDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DATABASE_NAME = "attendance_app.db"
    }
}
