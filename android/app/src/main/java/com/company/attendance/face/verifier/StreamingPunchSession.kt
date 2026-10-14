package com.company.attendance.face.verifier

import android.graphics.Bitmap
import android.graphics.RectF
import com.company.attendance.core.interfaces.FaceVerificationResult
import com.company.attendance.face.model.FaceDetection
import com.company.attendance.face.model.FrameRecord
import com.company.attendance.face.model.ModelType
import com.company.attendance.face.onnx.ArcFaceEmbedder
import com.company.attendance.face.onnx.Landmark3DRunner
import com.company.attendance.face.onnx.LivenessEngine
import com.company.attendance.face.onnx.OnnxSessionManager
import com.company.attendance.face.onnx.ScrfdDetector
import com.company.attendance.face.util.ImageProcessor
import com.company.attendance.face.util.MathUtils
import com.company.attendance.face.util.StageTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance streaming session that processes video frames concurrently
 * during the 2.5s recording window, leaving only ArcFace embedding extraction for post-capture.
 */
class StreamingPunchSession(
    private val sessionManager: OnnxSessionManager,
    private val scrfdDetector: ScrfdDetector,
    private val landmarkRunner: Landmark3DRunner,
    private val arcFaceEmbedder: ArcFaceEmbedder,
    private val livenessEngine: LivenessEngine,
    private val scope: CoroutineScope
) {
    private val frameQueue = Channel<Bitmap>(capacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val frameRecords = Collections.synchronizedList(mutableListOf<FrameRecord>())

    // Tracks candidates eligible for ArcFace recognition (pairs of bitmap + 5 kps)
    private data class ArcFaceCandidate(val image: Bitmap, val kps: Array<android.graphics.PointF>, val sharpness: Float, val yaw: Float)
    private val recognitionCandidates = Collections.synchronizedList(mutableListOf<ArcFaceCandidate>())

    private var multipleFacesCount = 0
    private var sampledDetectionsCount = 0
    private var isCancelled = false

    private val workerJob: Job = scope.launch(Dispatchers.Default.limitedParallelism(1)) {
        var frameIndex = 0
        var trackedBox: RectF? = null
        var lastDetectionKps: Array<android.graphics.PointF>? = null

        for (bitmap in frameQueue) {
            if (isCancelled) break
            val currentIdx = frameIndex++

            try {
                // Run SCRFD detection at 640px on frame 0 and every 4th frame, or if face was lost
                val needDetect = (trackedBox == null) || (currentIdx % 4 == 0)

                if (needDetect) {
                    sessionManager.withSession(ModelType.DETECTION) { env, session ->
                        StageTimer.time("Live SCRFD Detect (frame $currentIdx)") {
                            val detections = scrfdDetector.detect(env, session, bitmap, inputSize = 640, fallbackTo640 = false)
                            sampledDetectionsCount++

                            if (detections.isNotEmpty()) {
                                val sorted = detections.sortedByDescending { it.area }
                                val main = sorted[0]

                                if (sorted.size > 1 && sorted[1].area > 0.3f * main.area) {
                                    multipleFacesCount++
                                } else if (main.score >= 0.50f) {
                                    trackedBox = main.bbox
                                    lastDetectionKps = main.kps
                                }
                            } else {
                                trackedBox = null
                                lastDetectionKps = null
                            }
                            Unit
                        }
                    }
                }

                val currentBox = trackedBox ?: continue
                val kps = lastDetectionKps

                // Run 3D landmarks (192px input) on all valid frames for continuous liveness signals
                sessionManager.withSession(ModelType.LANDMARK_3D_68) { env, session ->
                    StageTimer.time("Live 3D Landmarks (frame $currentIdx)") {
                        val landmarks3d = landmarkRunner.extractLandmarks(env, session, bitmap, currentBox)
                        val stats = ImageProcessor.calculateFaceCropStats(bitmap, currentBox)
                        val sharpness = stats?.sharpness ?: 0f

                        val detection = FaceDetection(bbox = currentBox, kps = kps, score = 0.9f)
                        frameRecords.add(
                            FrameRecord(
                                image = bitmap,
                                face = detection,
                                pts = landmarks3d.points,
                                sharpness = sharpness,
                                yaw = landmarks3d.yaw
                            )
                        )

                        // Track bounding box smoothly from 3D landmarks
                        updateTrackedBoxFromLandmarks(landmarks3d.points, bitmap.width, bitmap.height)?.let { updated ->
                            trackedBox = updated
                        }

                        // Save as ArcFace candidate if keypoints exist
                        if (kps != null && stats != null && stats.minSidePx >= 100) {
                            recognitionCandidates.add(
                                ArcFaceCandidate(
                                    image = bitmap,
                                    kps = kps,
                                    sharpness = sharpness,
                                    yaw = landmarks3d.yaw
                                )
                            )
                        }
                        Unit
                    }
                }
            } catch (e: Throwable) {
                StageTimer.log("Frame processing error on frame $currentIdx: ${e.message}")
            }
        }
    }

    /**
     * Ingests a sampled camera preview frame (non-blocking).
     */
    fun submit(bitmap: Bitmap) {
        if (!isCancelled) {
            frameQueue.trySend(bitmap)
        }
    }

    /**
     * Finishes the session, waits for in-flight live inference, evaluates liveness,
     * and extracts ArcFace embeddings on top candidates.
     */
    suspend fun finish(
        referenceEmbedding: FloatArray,
        threshold: Float
    ): FaceVerificationResult = withContext(Dispatchers.Default) {
        frameQueue.close()
        workerJob.join()

        if (isCancelled) {
            return@withContext FaceVerificationResult.Error(IllegalStateException("Capture cancelled"), "Session cancelled")
        }

        StageTimer.time("Post-Capture Final Verification") {
            if (sampledDetectionsCount > 0 && multipleFacesCount > 0.3f * sampledDetectionsCount) {
                return@withContext FaceVerificationResult.MultipleFacesDetected(multipleFacesCount)
            }

            if (frameRecords.isEmpty() || recognitionCandidates.isEmpty()) {
                return@withContext FaceVerificationResult.NoFaceDetected
            }

            // 1. Evaluate Liveness & Anti-Spoofing
            if (frameRecords.size >= 2) {
                val signals = livenessEngine.computeSignals(frameRecords.toList())
                val verdict = livenessEngine.evaluateVerdict(signals, isIdentityConsistent = true)

                if (verdict.isFailed) {
                    return@withContext FaceVerificationResult.LivenessFailed(
                        verdict.reason ?: "Liveness check failed"
                    )
                }
            }

            // 2. Extract ArcFace Embedding on top 3 sharpest frontal candidates
            val frontal = recognitionCandidates.filter { abs(it.yaw) <= 0.25f }.ifEmpty { recognitionCandidates }
            val bestRecords = frontal.sortedByDescending { it.sharpness }.take(3)

            val sims = mutableListOf<Float>()
            var bestAlignedBitmap: Bitmap? = null
            var bestEmbedding: FloatArray? = null
            var bestFrameBitmap: Bitmap? = null

            sessionManager.withSession(ModelType.RECOGNITION) { env, session ->
                StageTimer.time("ArcFace Embedding (up to 3 frames)") {
                    for (candidate in bestRecords) {
                        val aligned = arcFaceEmbedder.alignFace(candidate.image, candidate.kps)
                        val embedding = arcFaceEmbedder.extractEmbedding(env, session, aligned)
                        val sim = MathUtils.cosineSimilarity(referenceEmbedding, embedding)
                        sims.add(sim)

                        if (bestAlignedBitmap == null || sim > (sims.maxOrNull() ?: 0f)) {
                            bestAlignedBitmap = aligned
                            bestEmbedding = embedding
                            bestFrameBitmap = candidate.image
                        }
                    }
                }
            }

            if (sims.isEmpty() || bestAlignedBitmap == null || bestEmbedding == null) {
                return@withContext FaceVerificationResult.NoFaceDetected
            }

            // Use best (max) similarity score achieved among candidate frames
            val rawBestScore = sims.maxOrNull() ?: 0f

            // Calibrate raw cosine score and threshold into intuitive confidence percentages (e.g. 0.43 -> ~70%)
            val calibratedScore = MathUtils.calibrateConfidence(rawBestScore)
            val calibratedThreshold = if (threshold <= 0.50f) MathUtils.calibrateConfidence(threshold) else threshold

            val isMatch = rawBestScore >= (if (threshold > 0.50f) 0.38f else threshold) || calibratedScore >= calibratedThreshold
            StageTimer.log("Face verified in post-capture: RawScore = $rawBestScore, Calibrated = $calibratedScore, Req = $calibratedThreshold, Match = $isMatch")

            if (isMatch) {
                FaceVerificationResult.Match(
                    score = calibratedScore,
                    threshold = calibratedThreshold,
                    alignedFaceBitmap = bestAlignedBitmap!!,
                    embedding = bestEmbedding!!
                )
            } else {
                FaceVerificationResult.Mismatch(
                    score = calibratedScore,
                    threshold = calibratedThreshold,
                    bestFrameBitmap = bestFrameBitmap
                )
            }
        }
    }

    fun cancel() {
        isCancelled = true
        frameQueue.close()
        workerJob.cancel()
    }

    private fun updateTrackedBoxFromLandmarks(points: Array<FloatArray>, imgW: Int, imgH: Int): RectF? {
        if (points.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            val x = p[0]
            val y = p[1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        val boxW = maxX - minX
        val boxH = maxY - minY
        if (boxW <= 0 || boxH <= 0) return null

        // Add 20% margin around landmark contour
        val padX = boxW * 0.20f
        val padY = boxH * 0.20f

        val left = max(0f, minX - padX)
        val top = max(0f, minY - padY)
        val right = min(imgW.toFloat(), maxX + padX)
        val bottom = min(imgH.toFloat(), maxY + padY)

        return RectF(left, top, right, bottom)
    }
}
