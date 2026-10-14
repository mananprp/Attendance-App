package com.company.attendance.core.interfaces

import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.TrustedTime
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing local punch records, daily summaries,
 * midnight resets, and missed-punch resolution logic.
 *
 * Implemented in Agent 1 (Core).
 * Used by Agent 4 (Punch Flow), Agent 6 (Sync Engine), and Agent 7 (Calendar).
 */
interface PunchRepository {
    /**
     * Observes the daily summary for a given user and local date.
     */
    fun observeDaySummary(userId: String, dateLocal: String): Flow<DaySummary?>

    /**
     * Directly retrieves the daily summary for a given user and local date.
     */
    suspend fun getDaySummary(userId: String, dateLocal: String): DaySummary?

    /**
     * Observes all daily summaries for a given month (YYYY-MM).
     */
    fun observeMonthSummaries(userId: String, yearMonth: String): Flow<List<DaySummary>>

    /**
     * Retrieves all punches marked with sync_status = PENDING.
     */
    suspend fun getPendingPunches(userId: String): List<Punch>

    /**
     * Records a newly validated punch (IN or OUT) into local Room storage:
     * 1. Inserts into `punches` table with status PENDING.
     * 2. Updates `day_summary` table (setting in_time or out_time, transitioning status).
     *
     * @return Result containing the recorded Punch or error.
     */
    suspend fun recordPunch(
        userId: String,
        punchType: PunchType,
        time: TrustedTime,
        latitude: Double,
        longitude: Double,
        distanceMeters: Float,
        matchScore: Float,
        capturedImagePath: String?,
        locationType: String = "OFFICE"
    ): Result<Punch>

    /**
     * Handles business rule FR-17 / Section 2:
     * If an earlier day has Punch In but no Punch Out: at the user's next-day first punch,
     * that earlier day is marked MISSED_PUNCH and its punch-out time is set equal to its punch-in time.
     *
     * @param userId Current user ID
     * @param currentDateLocal Current office local date (YYYY-MM-DD)
     */
    suspend fun resolvePastMissedPunches(userId: String, currentDateLocal: String)

    /**
     * Updates sync status for acknowledged punches.
     */
    suspend fun markPunchesSynced(punchUuids: List<String>)

    /**
     * Updates sync attempt count and failure state for failed uploads.
     */
    suspend fun markPunchSyncFailed(punchUuid: String, attemptCount: Int)

    /**
     * Inserts or replaces day summaries synced down from the server (e.g. on fresh login).
     */
    suspend fun upsertDaySummaries(summaries: List<DaySummary>)
}
