package com.company.attendance.face.reference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.BackendConfig
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.core.util.ImageStorageUtils
import com.company.attendance.feature.punch.capture.CameraCaptureUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ReferenceResult {
    data class Success(
        val bitmap: Bitmap,
        val embedding: FloatArray,
        val embeddingBytes: ByteArray,
        val photoVersion: Int,
        val photoHash: String
    ) : ReferenceResult

    sealed interface Failure : ReferenceResult {
        data object NoPhoto : Failure
        data object SessionChanged : Failure
        data class OwnerMismatch(val expected: String, val actual: String) : Failure
        data class HashMismatch(val expected: String, val actual: String) : Failure
        data class ExtractFailed(val reason: String) : Failure
        data class NetworkError(val message: String) : Failure
        data object Offline : Failure
    }
}

@Singleton
class FaceReferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage,
    private val apiService: AttendanceApiService,
    private val okHttpClient: OkHttpClient,
    private val faceVerifier: FaceVerifier,
    private val userLocalDao: UserLocalDao
) {
    private val userMutex = Mutex()
    private val activeCalls = Collections.synchronizedSet(mutableSetOf<Call>())

    fun cancelAll() {
        synchronized(activeCalls) {
            activeCalls.forEach { call ->
                try {
                    call.cancel()
                } catch (_: Exception) {}
            }
            activeCalls.clear()
        }
    }

    suspend fun clearLocalReference(userId: String) = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(context.filesDir, "profile_${userId}.jpg")
            if (photoFile.exists()) {
                photoFile.delete()
            }
            userLocalDao.clearReference(userId)
            tokenStorage.updateHasPhoto(false, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear local reference for $userId", e)
        }
    }

    suspend fun syncPhotoReferenceState(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        try {
            val response = apiService.getActivePhoto()
            if (response.code() == 404 || response.code() == 400) {
                // Photo was reset on server! Clear local cache immediately
                clearLocalReference(userId)
                return@withContext false
            } else if (response.isSuccessful && response.body() != null) {
                val serverPhoto = response.body()!!
                val userEntity = userLocalDao.getUserDirect(userId)
                val photoFile = File(context.filesDir, "profile_${userId}.jpg")
                if (!photoFile.exists() || userEntity?.photoHash != serverPhoto.hash || userEntity?.photoVersion != serverPhoto.version) {
                    // Photo changed on server or missing locally! Re-download
                    ensureReference(userId)
                }
                tokenStorage.updateHasPhoto(true, serverPhoto.version, serverPhoto.hash)
                return@withContext true
            }
            validateLocalReference(userId)
        } catch (e: Exception) {
            Log.w(TAG, "Photo sync check failed: ${e.message}")
            validateLocalReference(userId)
        }
    }

    suspend fun validateLocalReference(expectedUserId: String): Boolean = withContext(Dispatchers.IO) {
        if (expectedUserId.isBlank()) return@withContext false

        val userEntity = userLocalDao.getUserDirect(expectedUserId) ?: return@withContext false
        val embedding = userEntity.referenceEmbedding
        val ownerId = userEntity.ownerUserId
        val storedHash = userEntity.photoHash
        val storedVersion = userEntity.photoVersion

        if (embedding == null || embedding.isEmpty()) return@withContext false
        if (ownerId.isNullOrBlank() || !ownerId.equals(expectedUserId, ignoreCase = true)) return@withContext false
        if (storedHash.isNullOrBlank()) return@withContext false

        // Version check against active session version when available
        val sessionVersion = tokenStorage.getActivePhotoVersion()
        if (sessionVersion != null && storedVersion != null && sessionVersion != storedVersion) {
            return@withContext false
        }

        // Verify physical profile photo file exists and its SHA-256 matches storedHash
        val photoFile = File(context.filesDir, "profile_${expectedUserId}.jpg")
        val fileHash = computeFileSha256Hex(photoFile) ?: return@withContext false
        if (!fileHash.equals(storedHash.trim(), ignoreCase = true)) {
            Log.w(TAG, "Local photo hash mismatch for $expectedUserId. File: $fileHash, Stored: $storedHash")
            return@withContext false
        }

        true
    }

    suspend fun ensureReference(expectedUserId: String): ReferenceResult = userMutex.withLock {
        withContext(Dispatchers.IO) {
            if (expectedUserId.isBlank()) return@withContext ReferenceResult.Failure.SessionChanged

            // Check if already valid locally
            if (validateLocalReference(expectedUserId)) {
                val userEntity = userLocalDao.getUserDirect(expectedUserId)
                val photoBmp = ImageStorageUtils.loadProfilePhoto(context, expectedUserId)
                if (userEntity != null && userEntity.referenceEmbedding != null && photoBmp != null) {
                    val floats = CameraCaptureUtils.byteArrayToFloatArray(userEntity.referenceEmbedding)
                    return@withContext ReferenceResult.Success(
                        bitmap = photoBmp,
                        embedding = floats,
                        embeddingBytes = userEntity.referenceEmbedding,
                        photoVersion = userEntity.photoVersion ?: 1,
                        photoHash = userEntity.photoHash ?: ""
                    )
                }
            }

            // 1. Session binding capture
            val capturedToken = tokenStorage.getToken()
            val capturedSessionId = tokenStorage.getSessionId()
            if (capturedToken.isNullOrBlank() || capturedSessionId.isNullOrBlank()) {
                return@withContext ReferenceResult.Failure.SessionChanged
            }

            // 2. Fetch photo metadata from server
            val metadataResult = try {
                val response = apiService.getActivePhoto()
                NetworkHelper.parseResponse(response)
            } catch (e: Exception) {
                return@withContext ReferenceResult.Failure.NetworkError(e.message ?: "Failed to fetch photo metadata")
            }

            val photoDto = metadataResult.getOrElse { err ->
                if (err.message?.contains("PHOTO_NOT_FOUND", ignoreCase = true) == true) {
                    return@withContext ReferenceResult.Failure.NoPhoto
                }
                return@withContext ReferenceResult.Failure.NetworkError(err.message ?: "Photo metadata fetch error")
            }

            // 3. Server proof validation
            val returnedUserId = photoDto.userId
            if (!returnedUserId.isNullOrBlank() && !returnedUserId.equals(expectedUserId, ignoreCase = true)) {
                Log.e(TAG, "Owner mismatch! Expected $expectedUserId but server returned photo for $returnedUserId")
                return@withContext ReferenceResult.Failure.OwnerMismatch(expected = expectedUserId, actual = returnedUserId)
            }

            val expectedHash = photoDto.hash?.trim()
            if (expectedHash.isNullOrBlank()) {
                Log.e(TAG, "Server photo metadata for $expectedUserId is missing SHA-256 hash")
                return@withContext ReferenceResult.Failure.NetworkError("Server photo missing hash")
            }

            // 4. Cancellable HTTP download with captured token
            val targetUrl = BackendConfig.getFileUrl(photoDto.photoUrl)
            val separator = if (targetUrl.contains("?")) "&" else "?"
            val cacheBustedUrl = "$targetUrl${separator}uid=$expectedUserId&v=${photoDto.version}&t=${System.currentTimeMillis()}"

            val request = Request.Builder()
                .url(cacheBustedUrl)
                .header("Authorization", "Bearer $capturedToken")
                .header("Accept", "image/*, */*")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()

            val call = okHttpClient.newCall(request)
            activeCalls.add(call)

            val photoBytes = try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    return@withContext ReferenceResult.Failure.NetworkError("Photo download HTTP ${response.code}")
                }
                response.body?.bytes() ?: return@withContext ReferenceResult.Failure.NetworkError("Empty photo body")
            } catch (e: Exception) {
                if (call.isCanceled()) {
                    return@withContext ReferenceResult.Failure.SessionChanged
                }
                return@withContext ReferenceResult.Failure.NetworkError(e.message ?: "Photo download failed")
            } finally {
                activeCalls.remove(call)
            }

            // 5. Byte hash verification
            val computedHash = computeSha256Hex(photoBytes)
            if (!computedHash.equals(expectedHash, ignoreCase = true)) {
                Log.e(TAG, "Photo hash mismatch for $expectedUserId! Expected: $expectedHash, got: $computedHash")
                return@withContext ReferenceResult.Failure.HashMismatch(expected = expectedHash, actual = computedHash)
            }

            // 6. Decode & extract ArcFace reference embedding
            val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                ?: return@withContext ReferenceResult.Failure.ExtractFailed("Failed to decode photo bytes to Bitmap")

            val extractResult = faceVerifier.extractReferenceEmbedding(bitmap)
            val floats = extractResult.getOrElse { throwable ->
                return@withContext ReferenceResult.Failure.ExtractFailed(throwable.message ?: "Face extraction failed")
            }
            val embeddingBytes = CameraCaptureUtils.floatArrayToByteArray(floats)

            // 7. Commit guard: verify session has not changed while processing
            if (tokenStorage.getSessionId() != capturedSessionId ||
                !tokenStorage.getUserId().equals(expectedUserId, ignoreCase = true)
            ) {
                Log.w(TAG, "Session changed during reference processing for $expectedUserId. Discarding result.")
                return@withContext ReferenceResult.Failure.SessionChanged
            }

            // Write photo file and database proof together
            ImageStorageUtils.saveProfilePhoto(context, expectedUserId, bitmap)
            userLocalDao.updateReferenceEmbeddingAndProof(
                userId = expectedUserId,
                embedding = embeddingBytes,
                ownerUserId = expectedUserId,
                photoVersion = photoDto.version,
                photoHash = expectedHash
            )
            tokenStorage.updateHasPhoto(hasPhoto = true, version = photoDto.version, hash = expectedHash)

            Log.d(TAG, "Successfully bound and verified reference face for $expectedUserId (v=${photoDto.version}, hash=$expectedHash)")
            ReferenceResult.Success(
                bitmap = bitmap,
                embedding = floats,
                embeddingBytes = embeddingBytes,
                photoVersion = photoDto.version,
                photoHash = expectedHash
            )
        }
    }

    companion object {
        private const val TAG = "FaceReferenceManager"

        fun computeSha256Hex(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun computeFileSha256Hex(file: File): String? {
            if (!file.exists() || !file.isFile || file.length() == 0L) return null
            return try {
                val bytes = file.readBytes()
                computeSha256Hex(bytes)
            } catch (_: Exception) {
                null
            }
        }
    }
}
