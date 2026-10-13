package com.company.attendance.face.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.RectF
import com.company.attendance.face.model.FaceLandmarks3D
import com.company.attendance.face.util.ImageProcessor
import com.company.attendance.face.util.MathUtils
import java.nio.FloatBuffer
import kotlin.math.max

class Landmark3DRunner(
    private val inputSize: Int = 192
) {
    /**
     * Extracts 68 3D facial landmarks for a detected face bounding box.
     */
    fun extractLandmarks(
        env: OrtEnvironment,
        session: OrtSession,
        bitmap: Bitmap,
        bbox: RectF
    ): FaceLandmarks3D {
        val w = bbox.right - bbox.left
        val h = bbox.bottom - bbox.top
        val centerX = (bbox.left + bbox.right) / 2.0f
        val centerY = (bbox.top + bbox.bottom) / 2.0f

        val scale = inputSize.toFloat() / (max(w, h) * 1.5f)
        val halfSize = inputSize / 2.0f

        // Affine transform matrix mapping original bitmap -> 192x192 crop
        val m00 = scale
        val m01 = 0f
        val tx = halfSize - (scale * centerX)

        val m10 = 0f
        val m11 = scale
        val ty = halfSize - (scale * centerY)

        val affineMatrix = floatArrayOf(m00, m01, tx, m10, m11, ty)

        val cropBitmap = ImageProcessor.cropAndWarpAffine(bitmap, affineMatrix, inputSize, inputSize)
        val floatBuffer: FloatBuffer = ImageProcessor.bitmapToFloatBufferNCHW(
            cropBitmap,
            mean = 127.5f,
            std = 128.0f
        )

        val inputShape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        val inputName = session.inputNames.iterator().next()
        val tensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

        val points68 = Array(68) { FloatArray(3) }

        try {
            val result = session.run(mapOf(inputName to tensor))
            try {
                val rawOutput = result.get(0).value
                val flattened = flattenOutput(rawOutput)

                // 1k3d68 output can be 3309 floats (dense mesh) or 204 floats (68 * 3)
                // In InsightFace, 68 landmarks are the last 68 points
                val lmkNum = 68
                val totalPoints = flattened.size / 3
                val startPointIdx = max(0, totalPoints - lmkNum)

                for (i in 0 until lmkNum) {
                    val pIdx = startPointIdx + i
                    val xNorm = flattened[pIdx * 3]
                    val yNorm = flattened[pIdx * 3 + 1]
                    val zNorm = flattened[pIdx * 3 + 2]

                    // Denormalize from [-1, 1] to [0, inputSize]
                    points68[i][0] = (xNorm + 1.0f) * halfSize
                    points68[i][1] = (yNorm + 1.0f) * halfSize
                    points68[i][2] = zNorm * halfSize
                }
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
        }

        // Invert the crop affine transform to project landmarks back to original image space
        val invAffine = MathUtils.invertAffineTransform(affineMatrix)
        val mappedLandmarks = MathUtils.transformPoints(points68, invAffine)

        val yaw = MathUtils.yawProxy(mappedLandmarks)
        return FaceLandmarks3D(mappedLandmarks, yaw)
    }

    private fun flattenOutput(raw: Any): FloatArray {
        return when (raw) {
            is Array<*> -> {
                val list = mutableListOf<Float>()
                flattenIntoList(raw, list)
                list.toFloatArray()
            }
            is FloatArray -> raw
            else -> FloatArray(0)
        }
    }

    private fun flattenIntoList(array: Array<*>, out: MutableList<Float>) {
        for (item in array) {
            when (item) {
                is Array<*> -> flattenIntoList(item, out)
                is FloatArray -> {
                    for (v in item) out.add(v)
                }
                is Number -> out.add(item.toFloat())
            }
        }
    }
}
