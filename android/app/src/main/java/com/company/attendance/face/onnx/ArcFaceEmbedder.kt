package com.company.attendance.face.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.PointF
import com.company.attendance.face.util.ImageProcessor
import com.company.attendance.face.util.MathUtils
import java.nio.FloatBuffer
import kotlin.math.min

class ArcFaceEmbedder(
    private val inputSize: Int = 112
) {
    /**
     * Aligns the face using the 5 facial keypoints to a standardized 112x112 ArcFace crop.
     */
    fun alignFace(bitmap: Bitmap, kps: Array<PointF>): Bitmap {
        val affineMatrix = MathUtils.estimateSimilarityTransform(kps, MathUtils.ARCFACE_DST_112)
        return ImageProcessor.cropAndWarpAffine(bitmap, affineMatrix, inputSize, inputSize)
    }

    /**
     * Extracts a 512-dimensional L2-normalized embedding from the aligned 112x112 face crop.
     */
    fun extractEmbedding(
        env: OrtEnvironment,
        session: OrtSession,
        alignedBitmap: Bitmap
    ): FloatArray {
        val floatBuffer: FloatBuffer = ImageProcessor.bitmapToFloatBufferNCHW(
            alignedBitmap,
            mean = 127.5f,
            std = 127.5f
        )

        val inputShape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        val inputName = session.inputNames.iterator().next()
        val tensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

        val embeddingRaw = FloatArray(512)

        try {
            val result = session.run(mapOf(inputName to tensor))
            try {
                val rawOutput = result.get(0).value
                flattenEmbedding(rawOutput, embeddingRaw)
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
        }

        return MathUtils.l2Normalize(embeddingRaw)
    }

    private fun flattenEmbedding(raw: Any, out: FloatArray) {
        when (raw) {
            is Array<*> -> {
                var offset = 0
                for (item in raw) {
                    when (item) {
                        is FloatArray -> {
                            val copyLen = min(item.size, out.size - offset)
                            if (copyLen > 0) {
                                System.arraycopy(item, 0, out, offset, copyLen)
                                offset += copyLen
                            }
                        }
                        is Array<*> -> {
                            for (sub in item) {
                                if (sub is FloatArray) {
                                    val copyLen = min(sub.size, out.size - offset)
                                    if (copyLen > 0) {
                                        System.arraycopy(sub, 0, out, offset, copyLen)
                                        offset += copyLen
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is FloatArray -> {
                System.arraycopy(raw, 0, out, 0, min(raw.size, out.size))
            }
        }
    }
}
