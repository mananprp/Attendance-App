package com.company.attendance.face

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.company.attendance.face.model.FaceDetection
import com.company.attendance.face.model.FrameRecord
import com.company.attendance.face.model.LivenessSignals
import com.company.attendance.face.onnx.LivenessEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LivenessEngineTest {

    private val engine = LivenessEngine()

    @Test
    fun testVerdictPassWhenSignalsExceedThresholds() {
        val signals = LivenessSignals(
            nonrigid = 0.025f, // Exceeds 2 * 0.010 = 0.020 (nonrigid_part = 1.0)
            motion = 0.035f,   // Exceeds 2 * 0.015 = 0.030 (motion_part = 1.0)
            blinks = 1,        // blink_part = 1.0
            earMedian = 0.28f
        )
        val verdict = engine.evaluateVerdict(signals, isIdentityConsistent = true)

        assertEquals("pass", verdict.verdict)
        assertTrue("Verdict score should be >= 0.60", verdict.score >= 0.60f)
        assertTrue(verdict.isPassed)
        assertFalse(verdict.isFailed)
    }

    @Test
    fun testVerdictFailWhenFaceIsStaticOrFlat() {
        val signals = LivenessSignals(
            nonrigid = 0.002f, // Static photo
            motion = 0.001f,
            blinks = 0,
            earMedian = 0.25f
        )
        val verdict = engine.evaluateVerdict(signals, isIdentityConsistent = true)

        assertEquals("fail", verdict.verdict)
        assertEquals("flat_or_static_face", verdict.reason)
        assertTrue(verdict.isFailed)
        assertFalse(verdict.isPassed)
    }

    @Test
    fun testVerdictFailWhenIdentityIsInconsistent() {
        val signals = LivenessSignals(
            nonrigid = 0.030f,
            motion = 0.040f,
            blinks = 2,
            earMedian = 0.30f
        )
        val verdict = engine.evaluateVerdict(signals, isIdentityConsistent = false)

        assertEquals("fail", verdict.verdict)
        assertEquals("face_changed_during_clip", verdict.reason)
        assertTrue(verdict.isFailed)
    }

    @Test
    fun testVerdictInconclusiveWhenSignalsAreMarginal() {
        val signals = LivenessSignals(
            nonrigid = 0.012f,
            motion = 0.010f,
            blinks = 0,
            earMedian = 0.25f
        )
        val verdict = engine.evaluateVerdict(signals, isIdentityConsistent = true)

        assertEquals("inconclusive", verdict.verdict)
        assertEquals("weak_liveness_signals", verdict.reason)
        assertFalse(verdict.isPassed)
        assertFalse(verdict.isFailed)
    }
}
