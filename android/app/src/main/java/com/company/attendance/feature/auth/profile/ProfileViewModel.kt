package com.company.attendance.feature.auth.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao,
    private val apiService: AttendanceApiService,
    private val faceVerifier: FaceVerifier
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
                siteCode = siteCode,
                profilePhotoBitmap = localPhoto,
                isLoadingProfile = true
            )
        }

        viewModelScope.launch {
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

            // Fetch active photo URL from server if photo exists
            if (hasPhoto) {
                try {
                    val response = apiService.getActivePhoto()
                    val result = NetworkHelper.parseResponse(response)
                    result.onSuccess { photoDetail ->
                        var photoBmp = _uiState.value.profilePhotoBitmap
                        if (photoBmp == null) {
                            photoBmp = ImageStorageUtils.fetchAndSaveProfilePhoto(
                                context = context,
                                userId = userId,
                                photoUrl = photoDetail.photoUrl,
                                okHttpClient = okHttpClient
                            )
                        }

                        if (photoBmp != null) {
                            val userRecord = userLocalDao.getUserDirect(userId)
                            if (userRecord?.referenceEmbedding == null) {
                                val extractResult = faceVerifier.extractReferenceEmbedding(photoBmp)
                                extractResult.onSuccess { floats ->
                                    val bytes = EmbeddingUtils.floatArrayToByteArray(floats)
                                    userLocalDao.updateReferenceEmbedding(userId, bytes)
                                }
                            }
                        } else {
                            // Rule: The source of truth for has_photo is the server's login response.
                            // A failed download only means showing the placeholder avatar.
                            // Do NOT clear has_photo locally.
                            android.util.Log.w("ProfileViewModel", "Profile photo download failed for $userId – keeping has_photo as set by server")
                        }

                        _uiState.update {
                            it.copy(
                                photoUrl = photoDetail.photoUrl,
                                activePhotoVersion = photoDetail.version,
                                profilePhotoBitmap = photoBmp,
                                isLoadingProfile = false
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isLoadingProfile = false) }
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoadingProfile = false) }
                }
            } else {
                // If local photo exists, make sure embedding is also cached
                val localBmp = _uiState.value.profilePhotoBitmap
                if (localBmp != null) {
                    val userRecord = userLocalDao.getUserDirect(userId)
                    if (userRecord?.referenceEmbedding == null) {
                        val extractResult = faceVerifier.extractReferenceEmbedding(localBmp)
                        extractResult.onSuccess { floats ->
                            val bytes = EmbeddingUtils.floatArrayToByteArray(floats)
                            userLocalDao.updateReferenceEmbedding(userId, bytes)
                        }
                    }
                }
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

                    if (userId.isNotBlank()) {
                        userLocalDao.updateReferenceEmbedding(userId, embeddingBytes)
                        userLocalDao.updatePhotoChangeUsed(userId, true)
                        ImageStorageUtils.saveProfilePhoto(context, userId, bitmap)
                    }

                    tokenStorage.updatePhotoChangeUsed(true)
                    tokenStorage.updateHasPhoto(hasPhoto = true, version = uploadDto.version)

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
            tokenStorage.clearSession()
            userLocalDao.clearAll()
            _navigationEvent.emit(ProfileNavigationEvent.NavigateToLogin)
        }
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
