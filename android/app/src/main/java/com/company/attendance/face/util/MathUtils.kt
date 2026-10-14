package com.company.attendance.face.util

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object MathUtils {

    /**
     * Standard ArcFace 112x112 target landmark coordinates.
     */
    val ARCFACE_DST_112 = arrayOf(
        PointF(38.2946f, 51.6963f), // Left eye
        PointF(73.5318f, 51.5014f), // Right eye
        PointF(56.0252f, 71.7366f), // Nose tip
        PointF(41.5493f, 92.3655f), // Left mouth corner
        PointF(70.7299f, 92.2041f)  // Right mouth corner
    )

    /**
     * L2-normalizes the vector in-place or returns a normalized copy.
     */
    fun l2Normalize(v: FloatArray): FloatArray {
        var sumSq = 0.0
        for (x in v) {
            sumSq += (x * x)
        }
        val norm = sqrt(sumSq).toFloat() + 1e-9f
        val out = FloatArray(v.size)
        for (i in v.indices) {
            out[i] = v[i] / norm
        }
        return out
    }

    /**
     * Computes cosine similarity between two 512-d feature vectors.
     * Assumes vectors are either normalized or computes full dot / (norm1 * norm2).
     */
    fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        require(v1.size == v2.size) { "Vector sizes must match: ${v1.size} vs ${v2.size}" }
        var dot = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (i in v1.indices) {
            dot += (v1[i] * v2[i])
            norm1 += (v1[i] * v1[i])
            norm2 += (v2[i] * v2[i])
        }
        val denom = (sqrt(norm1) * sqrt(norm2)).toFloat() + 1e-9f
        return (dot / denom).toFloat()
    }

    /**
     * Estimates Umeyama 2D similarity transform mapping source 5 points to destination 5 points.
     * Returns 2x3 affine matrix [ [a, b, tx], [c, d, ty] ] represented as 6-element FloatArray.
     */
    fun estimateSimilarityTransform(
        src: Array<PointF>,
        dst: Array<PointF> = ARCFACE_DST_112
    ): FloatArray {
        require(src.size == dst.size && src.size >= 3) { "Need at least 3 point correspondences" }
        val n = src.size

        var srcMeanX = 0f
        var srcMeanY = 0f
        var dstMeanX = 0f
        var dstMeanY = 0f
        for (i in 0 until n) {
            srcMeanX += src[i].x
            srcMeanY += src[i].y
            dstMeanX += dst[i].x
            dstMeanY += dst[i].y
        }
        srcMeanX /= n
        srcMeanY /= n
        dstMeanX /= n
        dstMeanY /= n

        var srcVar = 0.0
        var sxx = 0.0
        var sxy = 0.0
        var syx = 0.0
        var syy = 0.0

        for (i in 0 until n) {
            val sx = src[i].x - srcMeanX
            val sy = src[i].y - srcMeanY
            val dx = dst[i].x - dstMeanX
            val dy = dst[i].y - dstMeanY

            srcVar += (sx * sx + sy * sy)
            // Covariance C = src_c^T * dst_c
            sxx += sx * dx
            sxy += sx * dy
            syx += sy * dx
            syy += sy * dy
        }

        srcVar /= n
        sxx /= n
        sxy /= n
        syx /= n
        syy /= n

        // Compute SVD of 2x2 matrix C = [[sxx, sxy], [syx, syy]]
        // C = [[a, b], [c, d]]
        val a = sxx
        val b = sxy
        val c = syx
        val d = syy

        // SVD analytical formulation for 2x2
        val theta = 0.5 * Math.atan2((2 * (a * c + b * d)), (a * a + b * b - c * c - d * d))
        val phi = 0.5 * Math.atan2((2 * (a * b + c * d)), (a * a - b * b + c * c - d * d))

        val u11 = Math.cos(theta)
        val u12 = -Math.sin(theta)
        val u21 = Math.sin(theta)
        val u22 = Math.cos(theta)

        val v11 = Math.cos(phi)
        val v12 = -Math.sin(phi)
        val v21 = Math.sin(phi)
        val v22 = Math.cos(phi)

        // Singular values
        val s1 = a * u11 * v11 + b * u11 * v21 + c * u21 * v11 + d * u21 * v21
        val s2 = a * u12 * v12 + b * u12 * v22 + c * u22 * v12 + d * u22 * v22

        val detU = u11 * u22 - u12 * u21
        val detV = v11 * v22 - v12 * v21
        val detC = detU * detV

        val dSign = if (detC < 0) -1.0 else 1.0

        // R = V * S_diag * U^T
        // S_diag = diag(1, dSign)
        val sv11 = v11
        val sv12 = v12 * dSign
        val sv21 = v21
        val sv22 = v22 * dSign

        val r00 = sv11 * u11 + sv12 * u12
        val r01 = sv11 * u21 + sv12 * u22
        val r10 = sv21 * u11 + sv22 * u12
        val r11 = sv21 * u21 + sv22 * u22

        val traceVal = abs(s1) + dSign * abs(s2)
        val scale = if (srcVar > 1e-8) (traceVal / srcVar) else 1.0

        val m00 = (scale * r00).toFloat()
        val m01 = (scale * r01).toFloat()
        val m10 = (scale * r10).toFloat()
        val m11 = (scale * r11).toFloat()

        val tx = dstMeanX - (m00 * srcMeanX + m01 * srcMeanY)
        val ty = dstMeanY - (m10 * srcMeanX + m11 * srcMeanY)

        return floatArrayOf(m00, m01, tx, m10, m11, ty)
    }

    /**
     * Inverts a 2x3 affine transformation matrix [a, b, tx, c, d, ty].
     */
    fun invertAffineTransform(m: FloatArray): FloatArray {
        val a = m[0]
        val b = m[1]
        val tx = m[2]
        val c = m[3]
        val d = m[4]
        val ty = m[5]

        val det = a * d - b * c
        if (abs(det) < 1e-9f) {
            return floatArrayOf(1f, 0f, -tx, 0f, 1f, -ty)
        }
        val invDet = 1.0f / det
        val invA = d * invDet
        val invB = -b * invDet
        val invC = -c * invDet
        val invD = a * invDet
        val invTx = (b * ty - d * tx) * invDet
        val invTy = (c * tx - a * ty) * invDet

        return floatArrayOf(invA, invB, invTx, invC, invD, invTy)
    }

    /**
     * Transforms 2D points [ [x, y], ... ] by a 2x3 affine matrix.
     */
    fun transformPoints(pts: Array<FloatArray>, m: FloatArray): Array<FloatArray> {
        val out = Array(pts.size) { FloatArray(pts[it].size) }
        val m00 = m[0]
        val m01 = m[1]
        val tx = m[2]
        val m10 = m[3]
        val m11 = m[4]
        val ty = m[5]

        val scale = sqrt(m00 * m00 + m10 * m10)

        for (i in pts.indices) {
            val px = pts[i][0]
            val py = pts[i][1]
            out[i][0] = m00 * px + m01 * py + tx
            out[i][1] = m10 * px + m11 * py + ty
            if (pts[i].size > 2) {
                out[i][2] = pts[i][2] * scale
            }
        }
        return out
    }

    /**
     * Dimensionless head-turn estimate from 68 landmarks (0 = frontal).
     * Matches Python: width = abs(pts[16, 0] - pts[0, 0]) + 1e-6
     * (pts[30, 0] - (pts[0, 0] + pts[16, 0]) / 2) / width
     */
    fun yawProxy(pts: Array<FloatArray>): Float {
        val width = abs(pts[16][0] - pts[0][0]) + 1e-6f
        return (pts[30][0] - (pts[0][0] + pts[16][0]) / 2.0f) / width
    }

    /**
     * Eye Aspect Ratio (EAR) for 6 landmark points around an eye (e.g. 36..41 or 42..47).
     * EAR = (||p1 - p5|| + ||p2 - p4||) / (2 * ||p0 - p3|| + 1e-6)
     */
    fun eyeAspectRatio(eyePts: Array<FloatArray>): Float {
        require(eyePts.size == 6) { "Eye landmark array must have 6 points" }
        val d1 = dist(eyePts[1], eyePts[5])
        val d2 = dist(eyePts[2], eyePts[4])
        val d0 = dist(eyePts[0], eyePts[3])
        return (d1 + d2) / (2.0f * d0 + 1e-6f)
    }

    private fun dist(p1: FloatArray, p2: FloatArray): Float {
        val dx = p1[0] - p2[0]
        val dy = p1[1] - p2[1]
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculates inter-ocular distance (IOD) between left and right eye centers.
     */
    fun interOcularDistance(pts: Array<FloatArray>): Float {
        var leftEyeX = 0f
        var leftEyeY = 0f
        for (i in 36..41) {
            leftEyeX += pts[i][0]
            leftEyeY += pts[i][1]
        }
        leftEyeX /= 6f
        leftEyeY /= 6f

        var rightEyeX = 0f
        var rightEyeY = 0f
        for (i in 42..47) {
            rightEyeX += pts[i][0]
            rightEyeY += pts[i][1]
        }
        rightEyeX /= 6f
        rightEyeY /= 6f

        val dx = leftEyeX - rightEyeX
        val dy = leftEyeY - rightEyeY
        return sqrt(dx * dx + dy * dy) + 1e-6f
    }

    /**
     * Solves least-squares affine transform mapping points P (68x2) to reference R (68x2).
     * Returns the normalized residual: sqrt(mean(||R - P*M||^2)) / iod.
     */
    fun computeAffineResidual(
        p: Array<FloatArray>,
        r: Array<FloatArray>,
        iod: Float
    ): Float {
        val n = p.size
        require(n == r.size && n > 0) { "Point clouds must have equal positive size" }

        // X is [P, 1], shape Nx3
        // A = X^T * X (3x3), B = X^T * R (3x2)
        var a00 = 0.0; var a01 = 0.0; var a02 = 0.0
        var a10 = 0.0; var a11 = 0.0; var a12 = 0.0
        var a20 = 0.0; var a21 = 0.0; var a22 = 0.0

        var b00 = 0.0; var b01 = 0.0
        var b10 = 0.0; var b11 = 0.0
        var b20 = 0.0; var b21 = 0.0

        for (i in 0 until n) {
            val px = p[i][0].toDouble()
            val py = p[i][1].toDouble()
            val rx = r[i][0].toDouble()
            val ry = r[i][1].toDouble()

            a00 += px * px
            a01 += px * py
            a02 += px

            a10 += py * px
            a11 += py * py
            a12 += py

            a20 += px
            a21 += py
            a22 += 1.0

            b00 += px * rx
            b01 += px * ry
            b10 += py * rx
            b11 += py * ry
            b20 += rx
            b21 += ry
        }

        // Invert 3x3 matrix A
        val det = a00 * (a11 * a22 - a12 * a21) -
                a01 * (a10 * a22 - a12 * a20) +
                a02 * (a10 * a21 - a11 * a20)

        if (abs(det) < 1e-12) {
            return 0.0f
        }

        val invDet = 1.0 / det
        val inv00 = (a11 * a22 - a12 * a21) * invDet
        val inv01 = (a02 * a21 - a01 * a22) * invDet
        val inv02 = (a01 * a12 - a02 * a11) * invDet

        val inv10 = (a12 * a20 - a10 * a22) * invDet
        val inv11 = (a00 * a22 - a02 * a20) * invDet
        val inv12 = (a02 * a10 - a00 * a12) * invDet

        val inv20 = (a10 * a21 - a11 * a20) * invDet
        val inv21 = (a01 * a20 - a00 * a21) * invDet
        val inv22 = (a00 * a11 - a01 * a10) * invDet

        // M = inv(A) * B, size 3x2
        val m00 = inv00 * b00 + inv01 * b10 + inv02 * b20
        val m01 = inv00 * b01 + inv01 * b11 + inv02 * b21

        val m10 = inv10 * b00 + inv11 * b10 + inv12 * b20
        val m11 = inv10 * b01 + inv11 * b11 + inv12 * b21

        val m20 = inv20 * b00 + inv21 * b10 + inv22 * b20
        val m21 = inv20 * b01 + inv21 * b11 + inv22 * b21

        // Residual sum of squares: ||R - X*M||^2
        var sumSqErr = 0.0
        for (i in 0 until n) {
            val px = p[i][0].toDouble()
            val py = p[i][1].toDouble()
            val rx = r[i][0].toDouble()
            val ry = r[i][1].toDouble()

            val predX = px * m00 + py * m10 + m20
            val predY = px * m01 + py * m11 + m21

            val ex = rx - predX
            val ey = ry - predY
            sumSqErr += (ex * ex + ey * ey)
        }

        val meanSq = sumSqErr / n
        return (sqrt(meanSq) / iod).toFloat()
    }

    /**
     * Calculates the p-th percentile of a float array (e.g. 75 for 75th percentile).
     */
    fun percentile(data: FloatArray, p: Float): Float {
        if (data.isEmpty()) return 0.0f
        if (data.size == 1) return data[0]
        val sorted = data.clone()
        sorted.sort()
        val rank = (p / 100.0f) * (sorted.size - 1)
        val lower = rank.toInt()
        val upper = min(lower + 1, sorted.size - 1)
        val weight = rank - lower
        return (1 - weight) * sorted[lower] + weight * sorted[upper]
    }

    /**
     * Calibrates raw 512-d ArcFace cosine similarity to an intuitive confidence percentage (0.0..1.0).
     *
     * Cosine similarity in ArcFace:
     * - Strangers / Impostors: 0.00..0.20 -> 0%..25%
     * - Ambiguous / Poor frame: 0.20..0.38 -> 25%..60%
     * - Typical genuine match (glasses, angle): 0.38..0.50 -> 60%..82% (e.g. 0.43 -> ~70%)
     * - High confidence: 0.50..1.00 -> 82%..100%
     */
    fun calibrateConfidence(rawCosine: Float): Float {
        val s = rawCosine.coerceIn(0f, 1f)
        return when {
            s <= 0.20f -> (s / 0.20f) * 0.25f
            s <= 0.38f -> 0.25f + ((s - 0.20f) / 0.18f) * 0.35f
            s <= 0.50f -> 0.60f + ((s - 0.38f) / 0.12f) * 0.22f
            else -> 0.82f + ((s - 0.50f) / 0.50f).coerceAtMost(1f) * 0.18f
        }
    }
}
