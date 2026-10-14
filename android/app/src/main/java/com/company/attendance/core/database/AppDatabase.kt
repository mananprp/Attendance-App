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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserLocalEntity::class,
        PunchEntity::class,
        DaySummaryEntity::class,
        AppConfigEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userLocalDao(): UserLocalDao
    abstract fun punchDao(): PunchDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        const val DATABASE_NAME = "attendance_app.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE punches ADD COLUMN location_type TEXT NOT NULL DEFAULT 'OFFICE'")
                db.execSQL("ALTER TABLE day_summary ADD COLUMN anchor_lat REAL")
                db.execSQL("ALTER TABLE day_summary ADD COLUMN anchor_lng REAL")
                db.execSQL("ALTER TABLE day_summary ADD COLUMN location_type TEXT NOT NULL DEFAULT 'OFFICE'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE punches ADD COLUMN shift_type TEXT NOT NULL DEFAULT 'GENERAL'")
                db.execSQL("ALTER TABLE day_summary ADD COLUMN shift_type TEXT NOT NULL DEFAULT 'GENERAL'")
            }
        }
    }
}
