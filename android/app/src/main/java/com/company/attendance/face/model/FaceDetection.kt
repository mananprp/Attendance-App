package com.company.attendance.face.model

import android.graphics.PointF
import android.graphics.RectF

/**
 * Result of SCRFD face detection on a single frame.
 */
data class FaceDetection(
    val bbox: RectF,
    val kps: Array<PointF>?, // 5 facial landmarks: [left_eye, right_eye, nose, left_mouth, right_mouth]
    val score: Float
) {
    val area: Float
        get() = (bbox.right - bbox.left) * (bbox.bottom - bbox.top)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceDetection) return false
        return bbox == other.bbox &&
                score == other.score &&
                ((kps == null && other.kps == null) || (kps != null && other.kps != null && kps.contentEquals(other.kps)))
    }

    override fun hashCode(): Int {
        var result = bbox.hashCode()
        result = 31 * result + (kps?.contentHashCode() ?: 0)
        result = 31 * result + score.hashCode()
        return result
    }
}

/**
 * Visual quality metrics of the detected face region.
 */
data class FaceCropStats(
    val minSidePx: Int,
    val sharpness: Float, // Laplacian variance
    val brightness: Float // Mean luminance (0..255)
)

/**
 * 3D 68 facial landmarks predicted by the 1k3d68 model.
 */
data class FaceLandmarks3D(
    val points: Array<FloatArray>, // 68 points, each [x, y, z] or [x, y] in original image coordinates
    val yaw: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceLandmarks3D) return false
        return yaw == other.yaw && points.contentDeepEquals(other.points)
    }

    override fun hashCode(): Int {
        var result = points.contentDeepHashCode()
        result = 31 * result + yaw.hashCode()
        return result
    }
}
