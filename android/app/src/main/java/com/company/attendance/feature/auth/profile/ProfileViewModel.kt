package com.company.attendance.feature.auth.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.dao.TrackingDao
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.network.ApiException
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.auth.profile.model.ProfileNavigationEvent
import com.company.attendance.feature.auth.profile.model.ProfileUiState
import com.company.attendance.feature.auth.util.EmbeddingUtils
import com.company.attendance.feature.auth.util.ImageUtils
import android.content.Context
import com.company.attendance.core.util.ImageStorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.room.withTransaction
import com.company.attendance.core.database.AppDatabase
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.face.reference.FaceReferenceManager
import com.company.attendance.face.reference.ReferenceResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao,
    private val punchDao: PunchDao,
    private val daySummaryDao: DaySummaryDao,
    private val trackingDao: TrackingDao,
    private val apiService: AttendanceApiService,
    private val faceVerifier: FaceVerifier,
    private val faceReferenceManager: FaceReferenceManager,
    private val appDatabase: AppDatabase,
    private val syncScheduler: SyncScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ProfileNavigationEvent>()
    val navigationEvent: SharedFlow<ProfileNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val session = tokenStorage.getUserSession()
        val userId = session?.userId ?: tokenStorage.getUserId() ?: ""
        val displayName = session?.displayName ?: tokenStorage.getDisplayName() ?: ""
        val photoChangeUsed = session?.photoChangeUsed ?: tokenStorage.isPhotoChangeUsed()
        val hasPhoto = session?.hasPhoto ?: tokenStorage.hasPhoto()
        val photoVersion = session?.activePhotoVersion ?: tokenStorage.getActivePhotoVersion()
        val isAdmin = tokenStorage.isAdmin()
        val isSupervisor = tokenStorage.getRole().equals("SUPERVISOR", ignoreCase = true)
        val siteCode = session?.siteCode ?: tokenStorage.getSiteCode()

        // Check for locally cached profile photo
        val localPhoto = ImageStorageUtils.loadProfilePhoto(context, userId)

        _uiState.update {
            it.copy(
                userId = userId,
                displayName = displayName,
                photoChangeUsed = photoChangeUsed,
                hasPhoto = hasPhoto,
                activePhotoVersion = photoVersion,
                isAdmin = isAdmin,
                isSupervisor = isSupervisor,
                siteCode = siteCode,
                profilePhotoBitmap = localPhoto,
                isLoadingProfile = true
            )
        }

        viewModelScope.launch {
            // Check for server-side role promotion (e.g. Employee -> Supervisor/Admin or Supervisor -> Admin)
            try {
                val adminUsersResp = apiService.getAdminUsers()
                if (adminUsersResp.isSuccessful && adminUsersResp.body() != null) {
                    val myRecord = adminUsersResp.body()!!.find { it.userId.equals(userId, ignoreCase = true) }
                    if (myRecord != null && !myRecord.role.equals(tokenStorage.getRole(), ignoreCase = true)) {
                        tokenStorage.updateRole(myRecord.role)
                        _uiState.update {
                            it.copy(
                                isAdmin = myRecord.role.equals("ADMIN", ignoreCase = true),
                                isSupervisor = myRecord.role.equals("SUPERVISOR", ignoreCase = true)
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Silently ignore 403 Forbidden for standard employees
            }

            // Load direct from local database if available
            val localUser = userLocalDao.getUserDirect(userId)
            if (localUser != null) {
                _uiState.update {
                    it.copy(
                        displayName = localUser.displayName.ifBlank { displayName },
                        photoChangeUsed = localUser.photoChangeUsed,
                        hasPhoto = localUser.hasPhoto
                    )
                }
            }

            if (hasPhoto) {
                try {
                    val isValid = faceReferenceManager.validateLocalReference(userId)
                    if (!isValid) {
                        val refResult = faceReferenceManager.ensureReference(userId)
                        if (refResult is ReferenceResult.Success) {
                            _uiState.update {
                                it.copy(
                                    profilePhotoBitmap = refResult.bitmap,
                                    activePhotoVersion = refResult.photoVersion,
                                    isLoadingProfile = false
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoadingProfile = false) }
                        }
                    } else {
                        val localBmp = ImageStorageUtils.loadProfilePhoto(context, userId)
                        _uiState.update {
                            it.copy(
                                profilePhotoBitmap = localBmp,
                                activePhotoVersion = photoVersion,
                                isLoadingProfile = false
                            )
                        }
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoadingProfile = false) }
                }
            } else {
                _uiState.update { it.copy(isLoadingProfile = false) }
            }
        }
    }

    fun openPhotoCapture() {
        if (_uiState.value.photoChangeUsed) return
        _uiState.update {
            it.copy(
                isPhotoCaptureOpen = true,
                capturedNewBitmap = null,
                extractedNewEmbedding = null,
                faceValidationError = null,
                replacePhotoError = null,
                successMessage = null
            )
        }
    }

    fun closePhotoCapture() {
        _uiState.update {
            it.copy(
                isPhotoCaptureOpen = false,
                capturedNewBitmap = null,
                extractedNewEmbedding = null,
                faceValidationError = null,
                replacePhotoError = null
            )
        }
    }

    fun onNewPhotoCaptured(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                capturedNewBitmap = bitmap,
                isAnalyzingFace = true,
                faceValidationError = null
            )
        }

        viewModelScope.launch {
            val result = faceVerifier.extractReferenceEmbedding(bitmap)

            result.onSuccess { embedding ->
                _uiState.update {
                    it.copy(
                        isAnalyzingFace = false,
                        extractedNewEmbedding = embedding,
                        faceValidationError = null
                    )
                }
            }.onFailure { throwable ->
                val errorMsg = mapFaceErrorToGuidance(throwable.message)
                _uiState.update {
                    it.copy(
                        isAnalyzingFace = false,
                        extractedNewEmbedding = null,
                        faceValidationError = errorMsg
                    )
                }
            }
        }
    }

    fun onRetakeNewPhoto() {
        _uiState.update {
            it.copy(
                capturedNewBitmap = null,
                extractedNewEmbedding = null,
                faceValidationError = null,
                replacePhotoError = null
            )
        }
    }

    fun confirmPhotoReplacement() {
        val bitmap = _uiState.value.capturedNewBitmap
        val embedding = _uiState.value.extractedNewEmbedding
        val userId = _uiState.value.userId

        if (bitmap == null || embedding == null) {
            _uiState.update { it.copy(replacePhotoError = "Please capture and verify your new photo first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isReplacingPhoto = true, replacePhotoError = null) }

            try {
                val photoPart = ImageUtils.createPhotoMultipart(bitmap, paramName = "photo", fileName = "profile_v2.jpg")
                val response = apiService.replacePhotoV2(photoPart)
                val parseResult = NetworkHelper.parseResponse(response)

                parseResult.onSuccess { uploadDto ->
                    val embeddingBytes = EmbeddingUtils.floatArrayToByteArray(embedding)
                    val photoHash = uploadDto.hash ?: FaceReferenceManager.computeSha256Hex(ImageUtils.bitmapToJpegBytes(bitmap))

                    if (userId.isNotBlank()) {
                        userLocalDao.updateReferenceEmbeddingAndProof(
                            userId = userId,
                            embedding = embeddingBytes,
                            ownerUserId = userId,
                            photoVersion = uploadDto.version,
                            photoHash = photoHash
                        )
                        userLocalDao.updatePhotoChangeUsed(userId, true)
                        ImageStorageUtils.saveProfilePhoto(context, userId, bitmap)
                    }

                    tokenStorage.updatePhotoChangeUsed(true)
                    tokenStorage.updateHasPhoto(hasPhoto = true, version = uploadDto.version, hash = photoHash)

                    _uiState.update {
                        it.copy(
                            isReplacingPhoto = false,
                            photoChangeUsed = true,
                            activePhotoVersion = uploadDto.version,
                            photoUrl = uploadDto.photoUrl,
                            profilePhotoBitmap = bitmap,
                            isPhotoCaptureOpen = false,
                            capturedNewBitmap = null,
                            extractedNewEmbedding = null,
                            successMessage = "Profile photo updated successfully (Version ${uploadDto.version})."
                        )
                    }
                }.onFailure { throwable ->
                    if (throwable is ApiException && throwable.httpCode == 403) {
                        // 403 Photo change limit reached
                        if (userId.isNotBlank()) {
                            userLocalDao.updatePhotoChangeUsed(userId, true)
                        }
                        tokenStorage.updatePhotoChangeUsed(true)

                        _uiState.update {
                            it.copy(
                                isReplacingPhoto = false,
                                photoChangeUsed = true,
                                replacePhotoError = "Photo change limit reached (1 change allowed)."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isReplacingPhoto = false,
                                replacePhotoError = throwable.message ?: "Photo replacement failed. Please try again."
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isReplacingPhoto = false,
                        replacePhotoError = e.message ?: "An unexpected error occurred during photo replacement."
                    )
                }
            }
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            val pendingCount = withContext(ioDispatcher) {
                val punches = punchDao.getAllPendingPunches().size
                val tracking = trackingDao.getUnsyncedCompletedSessions().size
                punches + tracking
            }

            if (pendingCount > 0) {
                // Try automatic sync first
                runCatching { syncScheduler.triggerSyncNow() }
                val remainingCount = withContext(ioDispatcher) {
                    val punches = punchDao.getAllPendingPunches().size
                    val tracking = trackingDao.getUnsyncedCompletedSessions().size
                    punches + tracking
                }

                if (remainingCount > 0) {
                    // Block logout and show popup
                    _uiState.update {
                        it.copy(
                            isSigningOut = false,
                            showUnsyncedLogoutDialog = true,
                            unsyncedCount = remainingCount
                        )
                    }
                    return@launch
                }
            }

            performDeepSignOut()
        }
    }

    fun dismissUnsyncedDialog() {
        _uiState.update { it.copy(showUnsyncedLogoutDialog = false) }
    }

    fun retrySyncAndSignOut() {
        _uiState.update { it.copy(showUnsyncedLogoutDialog = false) }
        signOut()
    }

    fun forceSignOut() {
        _uiState.update { it.copy(showUnsyncedLogoutDialog = false) }
        viewModelScope.launch {
            performDeepSignOut()
        }
    }

    private suspend fun performDeepSignOut() {
        withContext(ioDispatcher + NonCancellable) {
            // 1. Cancel in-flight reference work
            faceReferenceManager.cancelAll()

            // 2. Clear database inside single Room transaction
            appDatabase.withTransaction {
                userLocalDao.clearAll()
                punchDao.clearAll()
                daySummaryDao.clearAll()
                trackingDao.clearAllSessions()
                trackingDao.clearAllPoints()
                trackingDao.clearAllStops()
            }

            // 3. Delete user files (punches, profile photos, caches)
            ImageStorageUtils.deleteAllUserData(context)

            // 4. Synchronously clear session
            tokenStorage.clearSession()

            // 5. Evict network caches & pools
            try {
                okHttpClient.connectionPool.evictAll()
                okHttpClient.cache?.evictAll()
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(isSigningOut = false) }
        _navigationEvent.emit(ProfileNavigationEvent.NavigateToLogin)
    }

    private fun mapFaceErrorToGuidance(error: String?): String {
        if (error == null) return "Face verification failed. Please ensure your face is well-lit and centered."
        return when {
            error.contains("No face", ignoreCase = true) ->
                "No face detected. Please ensure your face is well-lit and clearly centered inside the oval guide."
            error.contains("More than one face", ignoreCase = true) || error.contains("multiple", ignoreCase = true) ->
                "Multiple faces detected. Please make sure only you are in the camera frame."
            error.contains("unclear", ignoreCase = true) || error.contains("blurry", ignoreCase = true) ->
                "The image is blurry or unclear. Please hold still in a well-lit area."
            error.contains("too small", ignoreCase = true) ->
                "Your face is too far away. Please move closer to the camera."
            error.contains("landmark", ignoreCase = true) ->
                "Could not detect all facial features. Please look directly at the camera."
            else -> error
        }
    }
}
