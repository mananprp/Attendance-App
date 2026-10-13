package com.company.attendance.feature.onboarding

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.auth.util.EmbeddingUtils
import com.company.attendance.feature.auth.util.ImageUtils
import com.company.attendance.feature.onboarding.model.OnboardingStep
import com.company.attendance.feature.onboarding.model.OnboardingUiState
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
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val faceVerifier: FaceVerifier,
    private val apiService: AttendanceApiService,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingCompletedEvent = MutableSharedFlow<Unit>()
    val onboardingCompletedEvent: SharedFlow<Unit> = _onboardingCompletedEvent.asSharedFlow()

    fun onCameraPermissionResult(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                isCameraPermissionGranted = isGranted,
                currentStep = if (isGranted) OnboardingStep.PHOTO_CAPTURE else OnboardingStep.CAMERA_PERMISSION
            )
        }
    }

    fun onPhotoCaptured(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                capturedBitmap = bitmap,
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
                        extractedEmbedding = embedding,
                        faceValidationError = null,
                        currentStep = OnboardingStep.PHOTO_CONFIRMATION
                    )
                }
            }.onFailure { throwable ->
                val errorMsg = mapFaceErrorToGuidance(throwable.message)
                _uiState.update {
                    it.copy(
                        isAnalyzingFace = false,
                        extractedEmbedding = null,
                        faceValidationError = errorMsg
                    )
                }
            }
        }
    }

    fun onRetakePhoto() {
        _uiState.update {
            it.copy(
                capturedBitmap = null,
                extractedEmbedding = null,
                faceValidationError = null,
                uploadError = null,
                currentStep = OnboardingStep.PHOTO_CAPTURE
            )
        }
    }

    fun uploadPhoto() {
        val bitmap = _uiState.value.capturedBitmap
        val embedding = _uiState.value.extractedEmbedding

        if (bitmap == null || embedding == null) {
            _uiState.update { it.copy(uploadError = "Please capture and verify your face first.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, uploadError = null) }

            try {
                val photoPart = ImageUtils.createPhotoMultipart(bitmap, paramName = "photo", fileName = "profile_v1.jpg")
                val response = apiService.uploadPhotoV1(photoPart)
                val parseResult = NetworkHelper.parseResponse(response)

                parseResult.onSuccess { uploadDto ->
                    val userId = tokenStorage.getUserId() ?: ""
                    val embeddingBytes = EmbeddingUtils.floatArrayToByteArray(embedding)

                    if (userId.isNotBlank()) {
                        userLocalDao.updateReferenceEmbedding(userId, embeddingBytes)
                        userLocalDao.updatePhotoChangeUsed(userId, uploadDto.photoChangeUsed)
                        ImageStorageUtils.saveProfilePhoto(context, userId, bitmap)
                    }

                    tokenStorage.updateHasPhoto(hasPhoto = true, version = uploadDto.version)
                    tokenStorage.updatePhotoChangeUsed(uploadDto.photoChangeUsed)

                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadedPhotoUrl = uploadDto.photoUrl,
                            activePhotoVersion = uploadDto.version,
                            currentStep = OnboardingStep.LOCATION_PERMISSION
                        )
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadError = throwable.message ?: "Photo upload failed. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadError = e.message ?: "An unexpected error occurred during photo upload."
                    )
                }
            }
        }
    }

    fun onLocationPermissionResult(isGranted: Boolean) {
        _uiState.update {
            it.copy(
                isLocationPermissionGranted = isGranted,
                currentStep = if (isGranted) OnboardingStep.COMPLETED else OnboardingStep.LOCATION_PERMISSION
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _onboardingCompletedEvent.emit(Unit)
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
                "Could not detect all facial features. Please look directly at the camera with neutral expression."
            else -> error
        }
    }
}
