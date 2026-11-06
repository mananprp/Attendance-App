package com.company.attendance.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.company.attendance.core.database.dao.AppConfigDao
import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.dao.TrackingDao
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.database.entity.DaySummaryEntity
import com.company.attendance.core.database.entity.PunchEntity
import com.company.attendance.core.database.entity.TrackingPointEntity
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.database.entity.TrackingStopEntity
import com.company.attendance.core.database.entity.UserLocalEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserLocalEntity::class,
        PunchEntity::class,
        DaySummaryEntity::class,
        AppConfigEntity::class,
        TrackingSessionEntity::class,
        TrackingPointEntity::class,
        TrackingStopEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userLocalDao(): UserLocalDao
    abstract fun punchDao(): PunchDao
    abstract fun daySummaryDao(): DaySummaryDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun trackingDao(): TrackingDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tracking_sessions (
                        session_id TEXT PRIMARY KEY NOT NULL,
                        start_time_utc INTEGER NOT NULL,
                        end_time_utc INTEGER,
                        total_distance_m REAL NOT NULL DEFAULT 0,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        is_synced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tracking_points (
                        id TEXT PRIMARY KEY NOT NULL,
                        session_id TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        accuracy_m REAL,
                        timestamp_utc INTEGER NOT NULL,
                        is_paused_gap INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracking_points_session_time ON tracking_points(session_id, timestamp_utc)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tracking_stops (
                        id TEXT PRIMARY KEY NOT NULL,
                        session_id TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        stop_name TEXT,
                        arrived_at_utc INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tracking_stops_session_time ON tracking_stops(session_id, arrived_at_utc)")
            }
        }
    }
}
