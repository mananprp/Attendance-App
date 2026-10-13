package com.company.attendance.face.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.company.attendance.face.model.FaceDetection
import com.company.attendance.face.util.ImageProcessor
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class ScrfdDetector(
    private val detThresh: Float = 0.50f,
    private val nmsThresh: Float = 0.40f
) {
    private val strides = intArrayOf(8, 16, 32)
    private val numAnchors = 2

    // Pre-computed anchor centers cache for supported input sizes (320 and 640)
    // Key: Pair(inputSize, stride), Value: FloatArray of anchor center coordinates
    private val anchorCentersCache = mutableMapOf<Pair<Int, Int>, FloatArray>()

    init {
        for (size in intArrayOf(320, 640)) {
            for (stride in strides) {
                val h = size / stride
                val w = size / stride
                val total = h * w * numAnchors
                val centers = FloatArray(total * 2)
                var idx = 0
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val cx = (x * stride).toFloat()
                        val cy = (y * stride).toFloat()
                        for (a in 0 until numAnchors) {
                            centers[idx++] = cx
                            centers[idx++] = cy
                        }
                    }
                }
                anchorCentersCache[Pair(size, stride)] = centers
            }
        }
    }

    /**
     * Runs face detection on the input bitmap using the provided active OrtSession.
     * Defaults to 320x320 for 4x faster inference during live punch capture,
     * with an automatic single retry at 640x640 if no face is detected.
     */
    fun detect(
        env: OrtEnvironment,
        session: OrtSession,
        bitmap: Bitmap,
        inputSize: Int = 640,
        fallbackTo640: Boolean = true
    ): List<FaceDetection> {
        val targetSize = if (inputSize == 320) 320 else 640
        val results = try {
            runDetectionPass(env, session, bitmap, targetSize)
        } catch (_: Exception) {
            emptyList()
        }

        if (results.isEmpty() && targetSize == 320 && fallbackTo640) {
            return try {
                runDetectionPass(env, session, bitmap, 640)
            } catch (_: Exception) {
                emptyList()
            }
        }
        return results
    }

    private fun runDetectionPass(
        env: OrtEnvironment,
        session: OrtSession,
        bitmap: Bitmap,
        size: Int
    ): List<FaceDetection> {
        val (paddedBitmap, detScale) = ImageProcessor.resizeWithPad(bitmap, size, size)
        val floatBuffer: FloatBuffer = ImageProcessor.bitmapToFloatBufferNCHW(
            paddedBitmap,
            mean = 127.5f,
            std = 128.0f
        )

        val inputShape = longArrayOf(1, 3, size.toLong(), size.toLong())
        val inputName = session.inputNames.iterator().next()
        val tensor = OnnxTensor.createTensor(env, floatBuffer, inputShape)

        val candidates = mutableListOf<FaceDetection>()

        try {
            val result = session.run(mapOf(inputName to tensor))
            try {
                val outputs = (0 until result.size()).map { result.get(it).value }
                val fmc = strides.size

                for (idx in strides.indices) {
                    val stride = strides[idx]
                    val centers = anchorCentersCache[Pair(size, stride)] ?: continue
                    val totalAnchors = centers.size / 2

                    val scoreRaw = outputs[idx]
                    val bboxRaw = outputs[idx + fmc]
                    val kpsRaw = outputs[idx + fmc * 2]

                    val scores = extractFlattenedArray(scoreRaw, totalAnchors, 1)
                    val bboxes = extractFlattenedArray(bboxRaw, totalAnchors, 4)
                    val kps = extractFlattenedArray(kpsRaw, totalAnchors, 10)

                    val invDetScale = 1.0f / detScale
                    val strideScale = stride.toFloat()

                    for (a in 0 until totalAnchors) {
                        val score = scores[a]
                        if (score < detThresh) continue

                        val cx = centers[a * 2]
                        val cy = centers[a * 2 + 1]

                        val l = bboxes[a * 4] * strideScale
                        val t = bboxes[a * 4 + 1] * strideScale
                        val r = bboxes[a * 4 + 2] * strideScale
                        val b = bboxes[a * 4 + 3] * strideScale

                        val x1 = max(0f, (cx - l) * invDetScale)
                        val y1 = max(0f, (cy - t) * invDetScale)
                        val x2 = min(bitmap.width.toFloat(), (cx + r) * invDetScale)
                        val y2 = min(bitmap.height.toFloat(), (cy + b) * invDetScale)

                        val landmarks = Array(5) { k ->
                            val kx = (cx + kps[a * 10 + k * 2] * strideScale) * invDetScale
                            val ky = (cy + kps[a * 10 + k * 2 + 1] * strideScale) * invDetScale
                            PointF(kx, ky)
                        }

                        candidates.add(
                            FaceDetection(
                                bbox = RectF(x1, y1, x2, y2),
                                kps = landmarks,
                                score = score
                            )
                        )
                    }
                }
            } finally {
                result.close()
            }
        } finally {
            tensor.close()
            if (paddedBitmap != bitmap) {
                paddedBitmap.recycle()
            }
        }

        return applyNms(candidates, nmsThresh)
    }

    private fun extractFlattenedArray(raw: Any, numItems: Int, itemDim: Int): FloatArray {
        val total = numItems * itemDim
        val out = FloatArray(total)
        when (raw) {
            is Array<*> -> {
                var offset = 0
                flattenRecursive(raw, out, offset)
            }
            is FloatArray -> {
                System.arraycopy(raw, 0, out, 0, min(raw.size, total))
            }
        }
        return out
    }

    private fun flattenRecursive(array: Array<*>, out: FloatArray, startOffset: Int): Int {
        var offset = startOffset
        for (item in array) {
            when (item) {
                is Array<*> -> offset = flattenRecursive(item, out, offset)
                is FloatArray -> {
                    System.arraycopy(item, 0, out, offset, item.size)
                    offset += item.size
                }
            }
        }
        return offset
    }

    /**
     * Non-Maximum Suppression (NMS) to eliminate overlapping bounding boxes.
     */
    private fun applyNms(dets: List<FaceDetection>, iouThreshold: Float): List<FaceDetection> {
        if (dets.isEmpty()) return emptyList()

        val sorted = dets.sortedByDescending { it.score }.toMutableList()
        val kept = mutableListOf<FaceDetection>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                if (computeIoU(best.bbox, next.bbox) > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return kept
    }

    private fun computeIoU(b1: RectF, b2: RectF): Float {
        val x1 = max(b1.left, b2.left)
        val y1 = max(b1.top, b2.top)
        val x2 = min(b1.right, b2.right)
        val y2 = min(b1.bottom, b2.bottom)

        val interWidth = max(0f, x2 - x1)
        val interHeight = max(0f, y2 - y1)
        val interArea = interWidth * interHeight

        val area1 = (b1.right - b1.left) * (b1.bottom - b1.top)
        val area2 = (b2.right - b2.left) * (b2.bottom - b2.top)

        val unionArea = area1 + area2 - interArea
        if (unionArea <= 0f) return 0f

        return interArea / unionArea
    }
}
