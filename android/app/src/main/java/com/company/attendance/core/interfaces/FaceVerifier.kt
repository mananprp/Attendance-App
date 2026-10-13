package com.company.attendance.core.interfaces

import android.graphics.Bitmap

/**
 * Result returned by the face verification pipeline.
 */
sealed interface FaceVerificationResult {
    /**
     * Face detected, liveness confirmed, and similarity score meets or exceeds the threshold.
     */
    data class Match(
        val score: Float,
        val threshold: Float,
        val alignedFaceBitmap: Bitmap,
        val embedding: FloatArray
    ) : FaceVerificationResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Match) return false
            return score == other.score && threshold == other.threshold && embedding.contentEquals(other.embedding)
        }
        override fun hashCode(): Int = 31 * score.hashCode() + embedding.contentHashCode()
    }

    /**
     * Face detected and liveness confirmed, but similarity score is below the threshold.
     */
    data class Mismatch(
        val score: Float,
        val threshold: Float,
        val bestFrameBitmap: Bitmap?
    ) : FaceVerificationResult

    /**
     * No face was detected in any of the sampled frames.
     */
    data object NoFaceDetected : FaceVerificationResult

    /**
     * Multiple faces were detected in the frame; single person constraint violated.
     */
    data class MultipleFacesDetected(val faceCount: Int) : FaceVerificationResult

    /**
     * Face detected, but failed 3D landmark liveness / anti-spoofing verification.
     */
    data class LivenessFailed(val reason: String) : FaceVerificationResult

    /**
     * Required ONNX models are missing or corrupted. Download must be triggered.
     */
    data class ModelMissingOrCorrupt(val missingModels: List<String>) : FaceVerificationResult

    /**
     * Pipeline processing error or unexpected inference failure.
     */
    data class Error(val throwable: Throwable, val message: String) : FaceVerificationResult
}

/**
 * Contract for the on-device ONNX Face Pipeline.
 *
 * Implemented by Agent 5 (Face Pipeline).
 * Used by Agent 3 (Onboarding profile capture) and Agent 4 (Punch Flow).
 */
interface FaceVerifier {
    /**
     * Checks if all required ONNX models (det_10g, 1k3d68, w600k_r50) exist on disk and pass checksum verification.
     */
    suspend fun areModelsReady(): Boolean

    /**
     * Extracts reference embedding and validates single-face presence from onboarding/photo replacement image.
     */
    suspend fun extractReferenceEmbedding(image: Bitmap): Result<FloatArray>

    /**
     * Verifies a sequence of captured camera frames (or single frame) against the cached reference embedding.
     *
     * Executes sequential ONNX model passes:
     * 1. det_10g (Detection) -> Bounding Box & 5 facial landmarks
     * 2. 1k3d68 (3D Landmarks / Liveness) -> Pitch, Yaw, Roll, Depth & Eye/Mouth ratio
     * 3. w600k_r50 (ArcFace Recognition) -> 512-d normalized embedding -> Cosine similarity
     *
     * @param frames List of sampled camera preview frames (Bitmap / ImageProxy converted)
     * @param referenceEmbedding The 512-float vector of the employee's approved profile photo
     * @param threshold Cosine similarity threshold (defaults from Remote Config, e.g. 0.55)
     */
    suspend fun verifyPunchFrames(
        frames: List<Bitmap>,
        referenceEmbedding: FloatArray,
        threshold: Float
    ): FaceVerificationResult

    /**
     * Pre-loads / warms up ONNX sessions into memory in preparation for capture.
     */
    suspend fun prepare(): Boolean

    /**
     * Starts a real-time streaming punch session that processes camera frames concurrently during video recording.
     */
    fun startStreamingSession(scope: kotlinx.coroutines.CoroutineScope): com.company.attendance.face.verifier.StreamingPunchSession

    /**
     * Releases any in-memory ONNX session native memory.
     */
    fun releaseResources()
}
