package com.company.attendance.face

import android.graphics.PointF
import com.company.attendance.face.util.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MathUtilsTest {

    @Test
    fun testCosineSimilarityIdenticalVectors() {
        val v1 = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val v2 = floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f)
        val sim = MathUtils.cosineSimilarity(v1, v2)
        assertEquals(1.0f, sim, 1e-5f)
    }

    @Test
    fun testCosineSimilarityOrthogonalVectors() {
        val v1 = floatArrayOf(1.0f, 0.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f, 0.0f)
        val sim = MathUtils.cosineSimilarity(v1, v2)
        assertEquals(0.0f, sim, 1e-5f)
    }

    @Test
    fun testCosineSimilarityOppositeVectors() {
        val v1 = floatArrayOf(1.0f, 2.0f, 3.0f)
        val v2 = floatArrayOf(-1.0f, -2.0f, -3.0f)
        val sim = MathUtils.cosineSimilarity(v1, v2)
        assertEquals(-1.0f, sim, 1e-5f)
    }

    @Test
    fun testL2Normalize() {
        val v = floatArrayOf(3.0f, 4.0f)
        val normalized = MathUtils.l2Normalize(v)
        assertEquals(0.6f, normalized[0], 1e-5f)
        assertEquals(0.8f, normalized[1], 1e-5f)

        var normSq = 0f
        for (x in normalized) normSq += x * x
        assertEquals(1.0f, normSq, 1e-5f)
    }

    @Test
    fun testUmeyamaSimilarityTransformAgainstArcFaceTemplate() {
        val src = arrayOf(
            PointF(120.5f, 140.2f),
            PointF(210.8f, 142.1f),
            PointF(165.0f, 195.4f),
            PointF(135.2f, 245.8f),
            PointF(198.6f, 246.3f)
        )

        val affine = MathUtils.estimateSimilarityTransform(src, MathUtils.ARCFACE_DST_112)
        assertEquals(6, affine.size)

        // Verify transformed points are close to destination points
        val transformed = MathUtils.transformPoints(
            Array(src.size) { floatArrayOf(src[it].x, src[it].y) },
            affine
        )

        for (i in src.indices) {
            val dstX = MathUtils.ARCFACE_DST_112[i].x
            val dstY = MathUtils.ARCFACE_DST_112[i].y
            val diffX = abs(transformed[i][0] - dstX)
            val diffY = abs(transformed[i][1] - dstY)
            assertTrue("Transformed point $i error too large: ($diffX, $diffY)", diffX < 15.0f && diffY < 15.0f)
        }
    }

    @Test
    fun testInvertAffineTransform() {
        val m = floatArrayOf(0.5f, -0.2f, 15.0f, 0.2f, 0.5f, -25.0f)
        val inv = MathUtils.invertAffineTransform(m)

        val pts = arrayOf(floatArrayOf(50.0f, 75.0f))
        val forward = MathUtils.transformPoints(pts, m)
        val back = MathUtils.transformPoints(forward, inv)

        assertEquals(50.0f, back[0][0], 1e-3f)
        assertEquals(75.0f, back[0][1], 1e-3f)
    }

    @Test
    fun testEyeAspectRatio() {
        // Open eye coordinates
        val openEye = arrayOf(
            floatArrayOf(100f, 200f), // 0: outer corner
            floatArrayOf(110f, 190f), // 1: top 1
            floatArrayOf(120f, 190f), // 2: top 2
            floatArrayOf(130f, 200f), // 3: inner corner
            floatArrayOf(120f, 210f), // 4: bottom 2
            floatArrayOf(110f, 210f)  // 5: bottom 1
        )
        val earOpen = MathUtils.eyeAspectRatio(openEye)
        assertTrue("Open EAR should be >= 0.25", earOpen >= 0.25f)

        // Closed eye coordinates (collapsed vertical distance)
        val closedEye = arrayOf(
            floatArrayOf(100f, 200f),
            floatArrayOf(110f, 200f),
            floatArrayOf(120f, 200f),
            floatArrayOf(130f, 200f),
            floatArrayOf(120f, 200f),
            floatArrayOf(110f, 200f)
        )
        val earClosed = MathUtils.eyeAspectRatio(closedEye)
        assertEquals(0.0f, earClosed, 1e-4f)
    }

    @Test
    fun testYawProxy() {
        val pts = Array(68) { FloatArray(2) }
        pts[0] = floatArrayOf(100f, 200f)  // Left contour
        pts[16] = floatArrayOf(300f, 200f) // Right contour
        pts[30] = floatArrayOf(200f, 220f) // Nose tip (center)

        val yawFrontal = MathUtils.yawProxy(pts)
        assertEquals(0.0f, yawFrontal, 1e-4f)

        // Shift nose tip to right (turn head)
        pts[30] = floatArrayOf(250f, 220f)
        val yawTurned = MathUtils.yawProxy(pts)
        assertTrue("Turned yaw should be positive", yawTurned > 0.2f)
    }

    @Test
    fun testPercentile() {
        val data = floatArrayOf(10f, 20f, 30f, 40f, 50f)
        val p50 = MathUtils.percentile(data, 50f)
        assertEquals(30.0f, p50, 1e-4f)

        val p75 = MathUtils.percentile(data, 75f)
        assertEquals(40.0f, p75, 1e-4f)
    }

    @Test
    fun testCalibrateConfidence() {
        // Strangers (< 0.20) should map to low confidence (< 25%)
        val strangerScore = MathUtils.calibrateConfidence(0.15f)
        assertTrue("Stranger should be < 25%", strangerScore < 0.25f)

        // Baseline threshold (0.38) maps to 60%
        val thresholdScore = MathUtils.calibrateConfidence(0.38f)
        assertEquals(0.60f, thresholdScore, 1e-3f)

        // Current genuine match (0.43) maps to ~69-70%
        val typicalGenuine = MathUtils.calibrateConfidence(0.43f)
        assertTrue("Typical genuine should be around 68-70%", typicalGenuine in 0.68f..0.71f)

        // Strong match (0.50) maps to 82%
        val strongMatch = MathUtils.calibrateConfidence(0.50f)
        assertEquals(0.82f, strongMatch, 1e-3f)
    }
}
