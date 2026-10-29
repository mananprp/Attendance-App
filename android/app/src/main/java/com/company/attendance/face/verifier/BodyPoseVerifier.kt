package com.company.attendance.face.verifier

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface BodyPoseResult {
    data class Success(val confidence: Float) : BodyPoseResult
    data class Failure(val reason: String) : BodyPoseResult
}

interface BodyPoseVerifier {
    suspend fun verifyFullBody(bitmap: Bitmap): BodyPoseResult
}

class BodyPoseVerifierImpl(
    private val options: PoseDetectorOptions = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
        .build(),
    private val detector: PoseDetector = PoseDetection.getClient(options)
) : BodyPoseVerifier {

    override suspend fun verifyFullBody(bitmap: Bitmap): BodyPoseResult =
        suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            detector.process(inputImage)
                .addOnSuccessListener { pose ->
                    val result = evaluatePose(pose, imageWidth, imageHeight)
                    continuation.resume(result)
                }
                .addOnFailureListener { exception ->
                    continuation.resume(
                        BodyPoseResult.Failure("Body pose detection failed: ${exception.localizedMessage ?: "Unknown error"}")
                    )
                }
        }

    private fun evaluatePose(pose: Pose, imageWidth: Float, imageHeight: Float): BodyPoseResult {
        // 1. Check if any pose is detected at all
        val allLandmarks = pose.allPoseLandmarks
        if (allLandmarks.isEmpty()) {
            return BodyPoseResult.Failure("No person or body detected. Please stand back so your entire outfit is in frame.")
        }

        // 2. Upper Body (Shoulders and Hips)
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val shoulderConf = maxOf(leftShoulder?.inFrameLikelihood ?: 0f, rightShoulder?.inFrameLikelihood ?: 0f)
        val hipConf = maxOf(leftHip?.inFrameLikelihood ?: 0f, rightHip?.inFrameLikelihood ?: 0f)

        if (shoulderConf < 0.45f) {
            return BodyPoseResult.Failure("Upper body / shoulders not clearly visible in the photo.")
        }

        if (hipConf < 0.40f) {
            return BodyPoseResult.Failure("Torso / waist not clearly visible. Please step back.")
        }

        // 3. Lower Body (Knees and Ankles)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val kneeConf = maxOf(leftKnee?.inFrameLikelihood ?: 0f, rightKnee?.inFrameLikelihood ?: 0f)
        val ankleConf = maxOf(leftAnkle?.inFrameLikelihood ?: 0f, rightAnkle?.inFrameLikelihood ?: 0f)

        if (kneeConf < 0.40f) {
            return BodyPoseResult.Failure("Full body not detected: knees/trousers are cut off. Please step back until your legs are visible.")
        }

        if (ankleConf < 0.35f) {
            return BodyPoseResult.Failure("Full body required: shoes/feet must be visible inside the frame.")
        }

        // 4. Boundary & Horizontal Framing Check
        val avgShoulderX = ( (leftShoulder?.position?.x ?: 0f) + (rightShoulder?.position?.x ?: imageWidth) ) / 2f
        if (avgShoulderX < 0.05f * imageWidth || avgShoulderX > 0.95f * imageWidth) {
            return BodyPoseResult.Failure("Please center yourself inside the camera frame.")
        }

        // 5. Vertical Span Check (Head-to-Toe span relative to total image height)
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        val lowestFootY = maxOf(
            leftAnkle?.position?.y ?: 0f,
            rightAnkle?.position?.y ?: 0f
        )
        val topHeadY = nose?.position?.y ?: minOf(
            leftShoulder?.position?.y ?: 0f,
            rightShoulder?.position?.y ?: 0f
        )

        val verticalSpan = lowestFootY - topHeadY
        val verticalSpanRatio = verticalSpan / imageHeight

        // A valid standing person in full-body capture should span at least 40% of the vertical frame
        if (verticalSpanRatio < 0.38f) {
            return BodyPoseResult.Failure("You are standing too far or crouched. Please stand straight within the body guideline.")
        }

        // Average confidence across key skeletal joints
        val avgConfidence = (shoulderConf + hipConf + kneeConf + ankleConf) / 4f
        return BodyPoseResult.Success(confidence = avgConfidence)
    }
}
