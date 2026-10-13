package com.company.attendance.core.interfaces

import kotlinx.coroutines.flow.StateFlow

data class SyncUiState(
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastSyncedEpochMs: Long? = null,
    val lastError: String? = null
)

sealed interface ManualSyncResult {
    data class Success(val uploadedCount: Int) : ManualSyncResult
    data class NoData(val message: String = "No pending records to sync") : ManualSyncResult
    data class Failure(val errorMessage: String) : ManualSyncResult
}

/**
 * Interface for scheduling WorkManager background sync and executing immediate Sync Now actions.
 *
 * Implemented in Agent 6 (Sync Engine).
 * Used by Agent 4 (Home screen Sync Now button and status display).
 */
interface SyncScheduler {
    /**
     * Observes current sync status, pending punch count, and timestamp of last successful sync.
     */
    val syncState: StateFlow<SyncUiState>

    /**
     * Schedules periodic WorkManager jobs for each configured sync window with randomized per-device jitter.
     *
     * @param syncWindows List of HH:mm strings, e.g. ["16:00", "00:00"]
     * @param jitterMinutes Maximum random delay in minutes (e.g. 30)
     */
    fun scheduleConfiguredWindows(syncWindows: List<String>, jitterMinutes: Int)

    /**
     * Triggers an immediate one-time sync of all PENDING records.
     */
    suspend fun triggerSyncNow(): ManualSyncResult

    /**
     * Refreshes the pending count and UI sync state.
     */
    suspend fun refreshPendingCount()
}
