package com.company.attendance.feature.auth.profile.model

import android.graphics.Bitmap

sealed interface ProfileNavigationEvent {
    data object NavigateToLogin : ProfileNavigationEvent
}

data class ProfileUiState(
    val userId: String = "",
    val displayName: String = "",
    val photoChangeUsed: Boolean = false,
    val hasPhoto: Boolean = false,
    val activePhotoVersion: Int? = null,
    val photoUrl: String? = null,
    val isLoadingProfile: Boolean = false,
    val isReplacingPhoto: Boolean = false,
    val isAnalyzingFace: Boolean = false,
    val faceValidationError: String? = null,
    val replacePhotoError: String? = null,
    val isPhotoCaptureOpen: Boolean = false,
    val capturedNewBitmap: Bitmap? = null,
    val extractedNewEmbedding: FloatArray? = null,
    val profilePhotoBitmap: Bitmap? = null,
    val isAdmin: Boolean = false,
    val siteCode: String = "",
    val successMessage: String? = null
) {
    val canChangePhoto: Boolean
        get() = !photoChangeUsed && hasPhoto

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileUiState

        if (userId != other.userId) return false
        if (displayName != other.displayName) return false
        if (photoChangeUsed != other.photoChangeUsed) return false
        if (hasPhoto != other.hasPhoto) return false
        if (activePhotoVersion != other.activePhotoVersion) return false
        if (photoUrl != other.photoUrl) return false
        if (isLoadingProfile != other.isLoadingProfile) return false
        if (isReplacingPhoto != other.isReplacingPhoto) return false
        if (isAnalyzingFace != other.isAnalyzingFace) return false
        if (faceValidationError != other.faceValidationError) return false
        if (replacePhotoError != other.replacePhotoError) return false
        if (isPhotoCaptureOpen != other.isPhotoCaptureOpen) return false
        if (capturedNewBitmap != other.capturedNewBitmap) return false
        if (extractedNewEmbedding != null) {
            if (other.extractedNewEmbedding == null) return false
            if (!extractedNewEmbedding.contentEquals(other.extractedNewEmbedding)) return false
        } else if (other.extractedNewEmbedding != null) return false
        if (successMessage != other.successMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + photoChangeUsed.hashCode()
        result = 31 * result + hasPhoto.hashCode()
        result = 31 * result + (activePhotoVersion ?: 0)
        result = 31 * result + (photoUrl?.hashCode() ?: 0)
        result = 31 * result + isLoadingProfile.hashCode()
        result = 31 * result + isReplacingPhoto.hashCode()
        result = 31 * result + isAnalyzingFace.hashCode()
        result = 31 * result + (faceValidationError?.hashCode() ?: 0)
        result = 31 * result + (replacePhotoError?.hashCode() ?: 0)
        result = 31 * result + isPhotoCaptureOpen.hashCode()
        result = 31 * result + (capturedNewBitmap?.hashCode() ?: 0)
        result = 31 * result + (extractedNewEmbedding?.contentHashCode() ?: 0)
        result = 31 * result + (successMessage?.hashCode() ?: 0)
        return result
    }
}
