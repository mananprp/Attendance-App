package com.company.attendance.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.attendance.core.database.entity.PunchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PunchDao {
    @Query("SELECT * FROM punches WHERE punch_uuid = :punchUuid")
    suspend fun getPunch(punchUuid: String): PunchEntity?

    @Query("SELECT * FROM punches WHERE user_id = :userId AND date_local = :dateLocal ORDER BY punch_time_utc ASC")
    fun getPunchesForDate(userId: String, dateLocal: String): Flow<List<PunchEntity>>

    @Query("SELECT * FROM punches WHERE user_id = :userId AND date_local = :dateLocal ORDER BY punch_time_utc ASC")
    suspend fun getPunchesForDateDirect(userId: String, dateLocal: String): List<PunchEntity>

    @Query("SELECT * FROM punches WHERE user_id = :userId AND sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingPunches(userId: String): List<PunchEntity>

    @Query("SELECT * FROM punches WHERE sync_status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getAllPendingPunches(): List<PunchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPunch(punch: PunchEntity)

    @Query("UPDATE punches SET sync_status = :status, sync_attempts = :attempts WHERE punch_uuid = :punchUuid")
    suspend fun updateSyncStatus(punchUuid: String, status: String, attempts: Int)

    @Query("UPDATE punches SET sync_status = 'SYNCED' WHERE punch_uuid IN (:punchUuids)")
    suspend fun markPunchesSynced(punchUuids: List<String>)

    @Query("DELETE FROM punches WHERE user_id = :userId")
    suspend fun clearUserPunches(userId: String)

    @Query("DELETE FROM punches")
    suspend fun clearAll()
}
