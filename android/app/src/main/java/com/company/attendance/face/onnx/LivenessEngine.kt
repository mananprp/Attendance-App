package com.company.attendance.face.onnx

import com.company.attendance.face.model.FrameRecord
import com.company.attendance.face.model.LivenessSignals
import com.company.attendance.face.model.LivenessVerdict
import com.company.attendance.face.util.MathUtils
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class LivenessEngine(
    private val nonrigidMin: Float = 0.010f,
    private val minMotion: Float = 0.015f,
    private val blinkDrop: Float = 0.75f,
    private val livenessPass: Float = 0.60f,
    private val livenessFail: Float = 0.25f
) {
    /**
     * Extracts motion, nonrigid landmark residual, and eye blink signals across frame records.
     */
    fun computeSignals(records: List<FrameRecord>): LivenessSignals {
        if (records.isEmpty()) {
            return LivenessSignals(nonrigid = 0f, motion = 0f, blinks = 0, earMedian = 0f)
        }
        if (records.size == 1) {
            val pts = records[0].pts
            val leftEar = MathUtils.eyeAspectRatio(pts.sliceArray(36..41))
            val rightEar = MathUtils.eyeAspectRatio(pts.sliceArray(42..47))
            val ear = (leftEar + rightEar) / 2.0f
            return LivenessSignals(nonrigid = 0f, motion = 0f, blinks = 0, earMedian = ear)
        }

        // 1. Select reference frame with lowest absolute yaw (most frontal)
        var refIndex = 0
        var minAbsYaw = Float.MAX_VALUE
        for (i in records.indices) {
            val absYaw = abs(records[i].yaw)
            if (absYaw < minAbsYaw) {
                minAbsYaw = absYaw
                refIndex = i
            }
        }

        val refPoints = records[refIndex].pts
        val iod = MathUtils.interOcularDistance(refPoints)

        // 2. Compute nonrigid landmark residual vs reference frame
        val residuals = mutableListOf<Float>()
        for (i in records.indices) {
            if (i == refIndex) continue
            val resid = MathUtils.computeAffineResidual(records[i].pts, refPoints, iod)
            residuals.add(resid)
        }
        val nonrigid = if (residuals.isNotEmpty()) {
            MathUtils.percentile(residuals.toFloatArray(), 75f)
        } else {
            0.0f
        }

        // 3. Compute head motion (translation of face center relative to IOD)
        val numFrames = records.size
        val centerXs = FloatArray(numFrames)
        val centerYs = FloatArray(numFrames)
        var meanCenterX = 0.0
        var meanCenterY = 0.0

        for (i in 0 until numFrames) {
            val pts = records[i].pts
            var sumX = 0f
            var sumY = 0f
            for (p in pts) {
                sumX += p[0]
                sumY += p[1]
            }
            val cx = sumX / pts.size
            val cy = sumY / pts.size
            centerXs[i] = cx
            centerYs[i] = cy
            meanCenterX += cx
            meanCenterY += cy
        }
        meanCenterX /= numFrames
        meanCenterY /= numFrames

        var varX = 0.0
        var varY = 0.0
        for (i in 0 until numFrames) {
            val dx = centerXs[i] - meanCenterX
            val dy = centerYs[i] - meanCenterY
            varX += (dx * dx)
            varY += (dy * dy)
        }
        val stdX = sqrt(varX / numFrames)
        val stdY = sqrt(varY / numFrames)
        val motionNorm = sqrt(stdX * stdX + stdY * stdY).toFloat()
        val motion = motionNorm / iod

        // 4. Compute Eye Aspect Ratio (EAR) and count blinks
        val ears = FloatArray(numFrames)
        for (i in 0 until numFrames) {
            val pts = records[i].pts
            val leftEar = MathUtils.eyeAspectRatio(pts.sliceArray(36..41))
            val rightEar = MathUtils.eyeAspectRatio(pts.sliceArray(42..47))
            ears[i] = (leftEar + rightEar) / 2.0f
        }

        val earMedian = MathUtils.percentile(ears, 50f)
        var blinks = 0
        if (earMedian > 0.12f) {
            val threshold = blinkDrop * earMedian
            val closed = BooleanArray(numFrames) { ears[it] < threshold }
            if (closed.isNotEmpty() && closed[0]) {
                blinks++
            }
            for (i in 1 until numFrames) {
                if (closed[i] && !closed[i - 1]) {
                    blinks++
                }
            }
        }

        return LivenessSignals(
            nonrigid = nonrigid,
            motion = motion,
            blinks = blinks,
            earMedian = earMedian
        )
    }

    /**
     * Evaluates liveness signals into a pass/inconclusive/fail verdict and score.
     */
    fun evaluateVerdict(
        signals: LivenessSignals,
        isIdentityConsistent: Boolean = true
    ): LivenessVerdict {
        val nonrigidPart = min(signals.nonrigid / (2.0f * nonrigidMin), 1.0f)
        val motionPart = min(signals.motion / (2.0f * minMotion), 1.0f) * min(signals.nonrigid / nonrigidMin, 1.0f)
        val blinkPart = if (signals.blinks >= 1) 1.0f else 0.0f

        val score = (0.5f * nonrigidPart) + (0.3f * blinkPart) + (0.2f * motionPart)

        val verdict: String
        val reason: String?

        if (!isIdentityConsistent) {
            verdict = "fail"
            reason = "face_changed_during_clip"
        } else if (score >= livenessPass) {
            verdict = "pass"
            reason = null
        } else if (score < livenessFail) {
            verdict = "fail"
            reason = "flat_or_static_face"
        } else {
            verdict = "inconclusive"
            reason = "weak_liveness_signals"
        }

        return LivenessVerdict(
            verdict = verdict,
            score = score,
            reason = reason,
            signals = signals
        )
    }
}
