package com.company.attendance.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.company.attendance.core.database.entity.DaySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DaySummaryDao {
    @Query("SELECT * FROM day_summary WHERE user_id = :userId AND date_local = :dateLocal")
    fun getDaySummary(userId: String, dateLocal: String): Flow<DaySummaryEntity?>

    @Query("SELECT * FROM day_summary WHERE user_id = :userId AND date_local = :dateLocal")
    suspend fun getDaySummaryDirect(userId: String, dateLocal: String): DaySummaryEntity?

    @Query("SELECT * FROM day_summary WHERE user_id = :userId AND date_local LIKE :monthPrefix || '%' ORDER BY date_local ASC")
    fun getMonthSummaries(userId: String, monthPrefix: String): Flow<List<DaySummaryEntity>>

    @Query("SELECT * FROM day_summary WHERE user_id = :userId AND date_local LIKE :monthPrefix || '%' ORDER BY date_local ASC")
    suspend fun getMonthSummariesDirect(userId: String, monthPrefix: String): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summary WHERE user_id = :userId AND date_local < :dateLocal AND in_time_utc IS NOT NULL AND out_time_utc IS NULL")
    suspend fun getUnclosedSummariesBeforeDate(userId: String, dateLocal: String): List<DaySummaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(daySummary: DaySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(summaries: List<DaySummaryEntity>)

    @Query("DELETE FROM day_summary WHERE user_id = :userId")
    suspend fun clearUserSummaries(userId: String)

    @Query("DELETE FROM day_summary")
    suspend fun clearAll()
}
