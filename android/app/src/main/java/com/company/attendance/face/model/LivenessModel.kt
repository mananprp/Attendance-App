package com.company.attendance.face.model

import android.graphics.Bitmap

/**
 * Extracted signals used for motion-based and deformation-based liveness verification.
 */
data class LivenessSignals(
    val nonrigid: Float,
    val motion: Float,
    val blinks: Int,
    val earMedian: Float
)

/**
 * Complete evaluation of liveness across a captured frame sequence.
 */
data class LivenessVerdict(
    val verdict: String, // "pass", "inconclusive", "fail"
    val score: Float,
    val reason: String?,
    val signals: LivenessSignals
) {
    val isPassed: Boolean
        get() = verdict == "pass"

    val isFailed: Boolean
        get() = verdict == "fail"
}

/**
 * Per-frame analysis record containing intermediate computation artifacts.
 */
data class FrameRecord(
    val image: Bitmap,
    val face: FaceDetection,
    val pts: Array<FloatArray>, // 68 points [x, y]
    val sharpness: Float,
    val yaw: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrameRecord) return false
        return face == other.face &&
                sharpness == other.sharpness &&
                yaw == other.yaw &&
                pts.contentDeepEquals(other.pts)
    }

    override fun hashCode(): Int {
        var result = face.hashCode()
        result = 31 * result + pts.contentDeepHashCode()
        result = 31 * result + sharpness.hashCode()
        result = 31 * result + yaw.hashCode()
        return result
    }
}
