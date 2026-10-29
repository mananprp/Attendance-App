package com.company.attendance.feature.punch.capture

import android.graphics.Bitmap
import com.company.attendance.core.model.PunchType

enum class CaptureStep {
    INITIALIZING,
    READY,
    CAPTURING,
    PROCESSING,
    SUCCESS,
    FAILURE
}

enum class CaptureMode {
    VIDEO,
    GUARD_PHOTO
}

/**
 * UI State for Face Punch Capture Flow.
 */
data class PunchCaptureUiState(
    val step: CaptureStep = CaptureStep.INITIALIZING,
    val punchType: PunchType = PunchType.IN,
    val captureMode: CaptureMode = CaptureMode.VIDEO,
    val captureProgress: Float = 0f, // 0.0 to 1.0
    val sampledFramesCount: Int = 0,
    val failureTitle: String = "Verification Failed",
    val failureMessage: String? = null,
    val failureReason: String? = null,
    val matchScore: Float? = null,
    val requiredThreshold: Float = 0.55f,
    val recordedTimeFormatted: String? = null,
    val recordedDateFormatted: String? = null,
    val isTimeUnverified: Boolean = false,
    val capturedImagePreview: Bitmap? = null,
    val isSavingPunch: Boolean = false
)

sealed interface PunchCaptureNavigationEvent {
    data object FinishSuccess : PunchCaptureNavigationEvent
    data object FinishCancel : PunchCaptureNavigationEvent
}
