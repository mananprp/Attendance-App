package com.company.attendance.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.company.attendance.core.database.entity.TrackingPointEntity
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.database.entity.TrackingStopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackingDao {

    // --- SESSIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TrackingSessionEntity)

    @Update
    suspend fun updateSession(session: TrackingSessionEntity)

    @Query("SELECT * FROM tracking_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY start_time_utc DESC LIMIT 1")
    suspend fun getActiveSession(): TrackingSessionEntity?

    @Query("SELECT * FROM tracking_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY start_time_utc DESC LIMIT 1")
    fun observeActiveSession(): Flow<TrackingSessionEntity?>

    @Query("SELECT * FROM tracking_sessions WHERE session_id = :sessionId")
    suspend fun getSession(sessionId: String): TrackingSessionEntity?

    @Query("SELECT * FROM tracking_sessions WHERE is_synced = 0 AND status = 'COMPLETED'")
    suspend fun getUnsyncedCompletedSessions(): List<TrackingSessionEntity>

    // --- POINTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackingPointEntity)

    @Query("SELECT * FROM tracking_points WHERE session_id = :sessionId ORDER BY timestamp_utc ASC")
    suspend fun getPointsForSession(sessionId: String): List<TrackingPointEntity>

    @Query("SELECT * FROM tracking_points WHERE session_id = :sessionId ORDER BY timestamp_utc ASC")
    fun observePointsForSession(sessionId: String): Flow<List<TrackingPointEntity>>

    @Query("SELECT * FROM tracking_points WHERE session_id = :sessionId ORDER BY timestamp_utc DESC LIMIT 1")
    suspend fun getLatestPoint(sessionId: String): TrackingPointEntity?

    @Query("SELECT COUNT(*) FROM tracking_points WHERE session_id = :sessionId")
    suspend fun getPointsCount(sessionId: String): Int

    // --- STOPS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: TrackingStopEntity)

    @Query("SELECT * FROM tracking_stops WHERE session_id = :sessionId ORDER BY arrived_at_utc ASC")
    suspend fun getStopsForSession(sessionId: String): List<TrackingStopEntity>

    @Query("SELECT * FROM tracking_stops WHERE session_id = :sessionId ORDER BY arrived_at_utc ASC")
    fun observeStopsForSession(sessionId: String): Flow<List<TrackingStopEntity>>

    // --- CLEANUP ---
    @Query("DELETE FROM tracking_points WHERE session_id = :sessionId")
    suspend fun deletePoints(sessionId: String)

    @Query("DELETE FROM tracking_stops WHERE session_id = :sessionId")
    suspend fun deleteStops(sessionId: String)
}
