package com.company.attendance.sync.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.Punch
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PunchSyncResultDto
import com.company.attendance.core.network.model.SyncPunchItemDto
import com.company.attendance.core.storage.TokenStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.time.Instant

@HiltWorker
class AttendanceSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val punchRepository: PunchRepository,
    private val apiService: AttendanceApiService,
    private val tokenStorage: TokenStorage
) : CoroutineWorker(appContext, workerParams) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun doWork(): Result {
        val userId = tokenStorage.getUserId()
        if (userId.isNullOrBlank()) {
            Log.d(TAG, "No logged-in user found in session storage. Skipping sync.")
            return Result.success()
        }

        val pendingPunches = try {
            punchRepository.getPendingPunches(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query pending punches from local database", e)
            return Result.retry()
        }

        if (pendingPunches.isEmpty()) {
            Log.d(TAG, "No pending punches to sync for user $userId.")
            return Result.success()
        }

        Log.i(TAG, "Starting sync batch for user $userId with ${pendingPunches.size} pending punch(es).")

        return try {
            val syncResult = uploadPunches(userId, pendingPunches)
            syncResult
        } catch (e: IOException) {
            Log.w(TAG, "Network connection error during punch sync. Triggering retry with exponential backoff.", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during punch sync execution", e)
            Result.failure()
        }
    }

    private suspend fun uploadPunches(userId: String, pendingPunches: List<Punch>): Result {
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
                locationType = punch.locationType,
                shiftType = punch.shiftType.name
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
                Log.i(TAG, "Batch sync successful: batchId=${body.batchId}, processed=${body.processedCount}")
                processSyncResults(pendingPunches, body.results)
                return Result.success()
            } else {
                Log.w(TAG, "Server returned 200 OK but empty body in sync response.")
                return Result.retry()
            }
        } else {
            val code = response.code()
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "Sync server response error: HTTP $code - $errorBody")

            if (code in 500..599 || code == 429) {
                // Server-side or rate limit error -> retry with backoff
                return Result.retry()
            } else {
                // Client error (4xx) -> mark punches failed
                pendingPunches.forEach { punch ->
                    punchRepository.markPunchSyncFailed(punch.punchUuid, punch.syncAttempts + 1)
                }
                return Result.failure()
            }
        }
    }

    private suspend fun processSyncResults(
        pendingPunches: List<Punch>,
        results: List<PunchSyncResultDto>
    ) {
        val resultsMap = results.associateBy { it.punchUuid }
        val punchesMap = pendingPunches.associateBy { it.punchUuid }
        val acknowledgedUuids = mutableListOf<String>()

        for (result in results) {
            val punch = punchesMap[result.punchUuid]
            when (result.status.uppercase()) {
                "ACCEPTED", "DUPLICATE" -> {
                    acknowledgedUuids.add(result.punchUuid)
                    // Delete temporary verification image from app-private storage
                    val path = punch?.imagePath
                    if (!path.isNullOrBlank()) {
                        try {
                            val file = File(path)
                            if (file.exists()) {
                                val deleted = file.delete()
                                Log.d(TAG, "Deleted local image file $path: $deleted")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to clean up image file at $path", e)
                        }
                    }
                }
                "REJECTED" -> {
                    val currentAttempts = punch?.syncAttempts ?: 0
                    Log.w(TAG, "Punch ${result.punchUuid} rejected by server: reason=${result.reason}")
                    punchRepository.markPunchSyncFailed(result.punchUuid, currentAttempts + 1)
                }
                else -> {
                    Log.w(TAG, "Unrecognized status ${result.status} for punch ${result.punchUuid}")
                }
            }
        }

        if (acknowledgedUuids.isNotEmpty()) {
            punchRepository.markPunchesSynced(acknowledgedUuids)
            Log.i(TAG, "Marked ${acknowledgedUuids.size} punch record(s) as SYNCED.")
        }
    }

    companion object {
        private const val TAG = "AttendanceSyncWorker"
        const val WORK_NAME = "attendance_sync_periodic"
        const val TAG_SYNC = "attendance_sync"
    }
}
