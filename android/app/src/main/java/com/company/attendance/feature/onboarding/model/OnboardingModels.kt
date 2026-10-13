package com.company.attendance.feature.onboarding.model

import android.graphics.Bitmap

enum class OnboardingStep {
    CAMERA_PERMISSION,
    PHOTO_CAPTURE,
    PHOTO_CONFIRMATION,
    LOCATION_PERMISSION,
    COMPLETED
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.CAMERA_PERMISSION,
    val capturedBitmap: Bitmap? = null,
    val extractedEmbedding: FloatArray? = null,
    val isAnalyzingFace: Boolean = false,
    val faceValidationError: String? = null,
    val isUploading: Boolean = false,
    val uploadError: String? = null,
    val isCameraPermissionGranted: Boolean = false,
    val isLocationPermissionGranted: Boolean = false,
    val uploadedPhotoUrl: String? = null,
    val activePhotoVersion: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OnboardingUiState

        if (currentStep != other.currentStep) return false
        if (capturedBitmap != other.capturedBitmap) return false
        if (extractedEmbedding != null) {
            if (other.extractedEmbedding == null) return false
            if (!extractedEmbedding.contentEquals(other.extractedEmbedding)) return false
        } else if (other.extractedEmbedding != null) return false
        if (isAnalyzingFace != other.isAnalyzingFace) return false
        if (faceValidationError != other.faceValidationError) return false
        if (isUploading != other.isUploading) return false
        if (uploadError != other.uploadError) return false
        if (isCameraPermissionGranted != other.isCameraPermissionGranted) return false
        if (isLocationPermissionGranted != other.isLocationPermissionGranted) return false
        if (uploadedPhotoUrl != other.uploadedPhotoUrl) return false
        if (activePhotoVersion != other.activePhotoVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentStep.hashCode()
        result = 31 * result + (capturedBitmap?.hashCode() ?: 0)
        result = 31 * result + (extractedEmbedding?.contentHashCode() ?: 0)
        result = 31 * result + isAnalyzingFace.hashCode()
        result = 31 * result + (faceValidationError?.hashCode() ?: 0)
        result = 31 * result + isUploading.hashCode()
        result = 31 * result + (uploadError?.hashCode() ?: 0)
        result = 31 * result + isCameraPermissionGranted.hashCode()
        result = 31 * result + isLocationPermissionGranted.hashCode()
        result = 31 * result + (uploadedPhotoUrl?.hashCode() ?: 0)
        result = 31 * result + (activePhotoVersion ?: 0)
        return result
    }
}
