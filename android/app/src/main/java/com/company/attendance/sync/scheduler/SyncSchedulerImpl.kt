package com.company.attendance.sync.scheduler

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.ManualSyncResult
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.core.interfaces.SyncUiState
import com.company.attendance.core.model.Punch
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PunchSyncResultDto
import com.company.attendance.core.network.model.SyncPunchItemDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.sync.util.SyncOffsetCalculator
import com.company.attendance.sync.work.AttendanceSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val punchRepository: PunchRepository,
    private val apiService: AttendanceApiService,
    private val tokenStorage: TokenStorage,
    private val remoteConfigRepository: RemoteConfigRepository
) : SyncScheduler {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val coroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _syncState = MutableStateFlow(SyncUiState())
    override val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    override fun scheduleConfiguredWindows(syncWindows: List<String>, jitterMinutes: Int) {
        if (syncWindows.isEmpty()) {
            Log.w(TAG, "No sync windows provided for scheduling.")
            return
        }

        coroutineScope.launch {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val timezone = try {
                remoteConfigRepository.getConfig().timezone
            } catch (_: Exception) {
                "Asia/Kolkata"
            }

            for (window in syncWindows) {
                val initialDelayMs = SyncOffsetCalculator.calculateInitialDelayMs(
                    windowStr = window,
                    jitterMinutes = jitterMinutes,
                    timezoneId = timezone
                )

                Log.i(
                    TAG,
                    "Scheduling sync window '$window' with jitter up to ${jitterMinutes}m in tz $timezone (initial delay: ${initialDelayMs / 1000}s)"
                )

                val periodicRequest = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .addTag(TAG_WINDOW_SYNC)
                    .addTag("window_${window.replace(':', '_')}")
                    .build()

                val uniqueWorkName = "attendance_sync_window_${window.replace(':', '_')}"
                workManager.enqueueUniquePeriodicWork(
                    uniqueWorkName,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicRequest
                )
            }
        }
    }

    override suspend fun triggerSyncNow(): ManualSyncResult = withContext(ioDispatcher) {
        if (_syncState.value.isSyncing) {
            return@withContext ManualSyncResult.Failure("Sync already in progress")
        }

        _syncState.update { it.copy(isSyncing = true, lastError = null) }

        val userId = tokenStorage.getUserId()
        if (userId.isNullOrBlank()) {
            _syncState.update { it.copy(isSyncing = false, lastError = "User not logged in") }
            return@withContext ManualSyncResult.Failure("User not logged in")
        }

        val pendingPunches = try {
            punchRepository.getPendingPunches(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load pending punches", e)
            val msg = e.message ?: "Failed to read pending punches"
            _syncState.update { it.copy(isSyncing = false, lastError = msg) }
            return@withContext ManualSyncResult.Failure(msg)
        }

        if (pendingPunches.isEmpty()) {
            _syncState.update { it.copy(isSyncing = false, pendingCount = 0) }
            return@withContext ManualSyncResult.NoData("No pending records to sync")
        }

        try {
            val result = executeSyncUpload(userId, pendingPunches)
            val remainingPending = punchRepository.getPendingPunches(userId).size
            val nowMs = System.currentTimeMillis()

            when (result) {
                is SyncUploadInternalResult.Success -> {
                    _syncState.update {
                        it.copy(
                            isSyncing = false,
                            pendingCount = remainingPending,
                            lastSyncedEpochMs = nowMs,
                            lastError = null
                        )
                    }
                    ManualSyncResult.Success(uploadedCount = result.uploadedCount)
                }
                is SyncUploadInternalResult.Failure -> {
                    _syncState.update {
                        it.copy(
                            isSyncing = false,
                            pendingCount = remainingPending,
                            lastError = result.errorMessage
                        )
                    }
                    ManualSyncResult.Failure(result.errorMessage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing immediate sync", e)
            val errorMsg = e.message ?: "Sync execution failed"
            val remaining = try { punchRepository.getPendingPunches(userId).size } catch (_: Exception) { pendingPunches.size }
            _syncState.update { it.copy(isSyncing = false, pendingCount = remaining, lastError = errorMsg) }
            ManualSyncResult.Failure(errorMsg)
        }
    }

    override suspend fun refreshPendingCount() = withContext(ioDispatcher) {
        val userId = tokenStorage.getUserId()
        if (userId.isNullOrBlank()) {
            _syncState.update { it.copy(pendingCount = 0) }
            return@withContext
        }

        try {
            val pending = punchRepository.getPendingPunches(userId)
            _syncState.update { it.copy(pendingCount = pending.size) }
        } catch (e: Exception) {
            Log.w(TAG, "Could not refresh pending count", e)
        }
    }

    private suspend fun executeSyncUpload(
        userId: String,
        pendingPunches: List<Punch>
    ): SyncUploadInternalResult {
        val syncItems = pendingPunches.map { punch ->
            val imageFileName = punch.imagePath?.let { path ->
                val file = File(path)
                if (file.exists() && file.isFile) file.name else null
            }

            SyncPunchItemDto(
                punchUuid = punch.punchUuid,
                userId = punch.userId,
                dateLocal = punch.dateLocal,
                type = punch.type.name,
                punchTimeUtc = Instant.ofEpochMilli(punch.punchTimeUtcEpochMs).toString(),
                timeSource = punch.timeSource.name,
                timeUnverified = punch.timeUnverified,
                latitude = punch.latitude,
                longitude = punch.longitude,
                distanceM = punch.distanceMeters,
                matchScore = punch.matchScore,
                imageFileName = imageFileName,
                locationType = punch.locationType
            )
        }

        val batchJsonString = json.encodeToString(syncItems)
        val batchJsonPart = MultipartBody.Part.createFormData("batch_json", batchJsonString)

        val imageParts = mutableListOf<MultipartBody.Part>()
        for (punch in pendingPunches) {
            val path = punch.imagePath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.isFile) {
                    val mediaType = "image/jpeg".toMediaTypeOrNull()
                    val reqBody = file.asRequestBody(mediaType)
                    val part = MultipartBody.Part.createFormData("images", file.name, reqBody)
                    imageParts.add(part)
                }
            }
        }

        val response = apiService.syncPunches(batchJsonPart, imageParts)

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val acknowledgedCount = handleAcknowledgedResults(pendingPunches, body.results)
                return SyncUploadInternalResult.Success(uploadedCount = acknowledgedCount)
            } else {
                return SyncUploadInternalResult.Failure("Empty response body from server")
            }
        } else {
            val code = response.code()
            val errorBody = response.errorBody()?.string()
            val errorMsg = when (code) {
                401 -> "Session expired or invalid login. Please sign out and log in again to sync."
                403 -> "Sync forbidden. User account may be inactive."
                else -> "Server error HTTP $code: $errorBody"
            }
            // Record failure on local punches
            pendingPunches.forEach { punch ->
                punchRepository.markPunchSyncFailed(punch.punchUuid, punch.syncAttempts + 1)
            }
            return SyncUploadInternalResult.Failure(errorMsg)
        }
    }

    private suspend fun handleAcknowledgedResults(
        pendingPunches: List<Punch>,
        results: List<PunchSyncResultDto>
    ): Int {
        val resultsMap = results.associateBy { it.punchUuid }
        val punchesMap = pendingPunches.associateBy { it.punchUuid }
        val acknowledgedUuids = mutableListOf<String>()

        for (result in results) {
            val punch = punchesMap[result.punchUuid]
            when (result.status.uppercase()) {
                "ACCEPTED", "DUPLICATE" -> {
                    acknowledgedUuids.add(result.punchUuid)
                    // Delete image file after server acknowledgment
                    val path = punch?.imagePath
                    if (!path.isNullOrBlank()) {
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to clean up image file: $path", e)
                        }
                    }
                }
                "REJECTED" -> {
                    val currentAttempts = punch?.syncAttempts ?: 0
                    Log.w(TAG, "Punch ${result.punchUuid} rejected: ${result.reason}")
                    punchRepository.markPunchSyncFailed(result.punchUuid, currentAttempts + 1)
                }
            }
        }

        if (acknowledgedUuids.isNotEmpty()) {
            punchRepository.markPunchesSynced(acknowledgedUuids)
        }

        return acknowledgedUuids.size
    }

    private sealed interface SyncUploadInternalResult {
        data class Success(val uploadedCount: Int) : SyncUploadInternalResult
        data class Failure(val errorMessage: String) : SyncUploadInternalResult
    }

    companion object {
        private const val TAG = "SyncSchedulerImpl"
        const val TAG_WINDOW_SYNC = "attendance_window_sync"
    }
}
