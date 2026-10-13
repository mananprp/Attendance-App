package com.company.attendance.face.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.company.attendance.face.model.FaceCropStats
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    /**
     * Converts a Bitmap into a direct FloatBuffer in NCHW format [1, 3, height, width] with normalization.
     * Writes directly to native buffer in a single pass without intermediate R/G/B plane array allocations.
     */
    fun bitmapToFloatBufferNCHW(
        bitmap: Bitmap,
        mean: Float = 127.5f,
        std: Float = 128.0f
    ): FloatBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        val pixels = IntArray(totalPixels)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val byteBuffer = ByteBuffer.allocateDirect(1 * 3 * height * width * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()

        val invStd = 1.0f / std
        val gOffset = totalPixels
        val bOffset = totalPixels * 2

        for (i in 0 until totalPixels) {
            val pixel = pixels[i]
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            floatBuffer.put(i, (r - mean) * invStd)
            floatBuffer.put(gOffset + i, (g - mean) * invStd)
            floatBuffer.put(bOffset + i, (b - mean) * invStd)
        }

        floatBuffer.position(0)
        floatBuffer.limit(3 * totalPixels)
        return floatBuffer
    }

    /**
     * Resizes the bitmap with aspect-ratio preservation and top-left placement onto a padded canvas of targetWidth x targetHeight.
     * Returns the padded bitmap and the scale factor (det_scale) used.
     */
    fun resizeWithPad(
        src: Bitmap,
        targetWidth: Int = 640,
        targetHeight: Int = 640
    ): Pair<Bitmap, Float> {
        val imRatio = src.height.toFloat() / src.width.toFloat()
        val modelRatio = targetHeight.toFloat() / targetWidth.toFloat()

        val newWidth: Int
        val newHeight: Int
        if (imRatio > modelRatio) {
            newHeight = targetHeight
            newWidth = (targetHeight / imRatio).toInt()
        } else {
            newWidth = targetWidth
            newHeight = (targetWidth * imRatio).toInt()
        }

        val detScale = newHeight.toFloat() / src.height.toFloat()

        val scaledBitmap = Bitmap.createScaledBitmap(src, newWidth, newHeight, true)
        val paddedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(paddedBitmap)
        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

        if (scaledBitmap != src && scaledBitmap != paddedBitmap) {
            scaledBitmap.recycle()
        }

        return Pair(paddedBitmap, detScale)
    }

    /**
     * Warps the source bitmap using a 2x3 affine matrix [m00, m01, tx, m10, m11, ty]
     * mapping source image coordinates to output canvas coordinates.
     */
    fun cropAndWarpAffine(
        src: Bitmap,
        affineMatrix2x3: FloatArray,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)

        val androidMatrix = Matrix().apply {
            setValues(floatArrayOf(
                affineMatrix2x3[0], affineMatrix2x3[1], affineMatrix2x3[2],
                affineMatrix2x3[3], affineMatrix2x3[4], affineMatrix2x3[5],
                0f, 0f, 1f
            ))
        }

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(src, androidMatrix, paint)
        return outBitmap
    }

    /**
     * Computes face crop statistics: min side, Laplacian variance (sharpness), and mean luminance.
     */
    fun calculateFaceCropStats(image: Bitmap, bbox: RectF): FaceCropStats? {
        val imgWidth = image.width
        val imgHeight = image.height

        val x1 = max(0, bbox.left.toInt())
        val y1 = max(0, bbox.top.toInt())
        val x2 = min(imgWidth, bbox.right.toInt())
        val y2 = min(imgHeight, bbox.bottom.toInt())

        val cropWidth = x2 - x1
        val cropHeight = y2 - y1

        if (cropWidth <= 0 || cropHeight <= 0) {
            return null
        }

        // Downsample for sharpness computation if crop is very large (caps processing time to < 2ms)
        val maxSide = max(cropWidth, cropHeight)
        val scale = if (maxSide > 128) 128f / maxSide else 1f
        val sampleW = (cropWidth * scale).toInt().coerceAtLeast(16)
        val sampleH = (cropHeight * scale).toInt().coerceAtLeast(16)

        val cropBitmap = Bitmap.createBitmap(image, x1, y1, cropWidth, cropHeight)
        val sampleBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(cropBitmap, sampleW, sampleH, true)
        } else {
            cropBitmap
        }

        val pixels = IntArray(sampleW * sampleH)
        sampleBitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)

        if (sampleBitmap != cropBitmap && sampleBitmap != image) {
            sampleBitmap.recycle()
        }
        if (cropBitmap != image) {
            cropBitmap.recycle()
        }

        // Convert to grayscale and calculate luminance
        val gray = FloatArray(sampleW * sampleH)
        var sumGray = 0f
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            gray[i] = lum
            sumGray += lum
        }
        val meanLuminance = sumGray / (sampleW * sampleH)

        // Compute discrete Laplacian variance
        var sumLap = 0.0
        var sumLapSq = 0.0
        var lapCount = 0

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val center = gray[y * sampleW + x]
                val top = gray[(y - 1) * sampleW + x]
                val bottom = gray[(y + 1) * sampleW + x]
                val left = gray[y * sampleW + (x - 1)]
                val right = gray[y * sampleW + (x + 1)]

                val lap = (top + bottom + left + right - 4.0f * center).toDouble()
                sumLap += lap
                sumLapSq += (lap * lap)
                lapCount++
            }
        }

        val sharpness = if (lapCount > 0) {
            val meanLap = sumLap / lapCount
            val variance = (sumLapSq / lapCount) - (meanLap * meanLap)
            max(0.0, variance).toFloat()
        } else {
            0.0f
        }

        return FaceCropStats(
            minSidePx = min(cropWidth, cropHeight),
            sharpness = sharpness,
            brightness = meanLuminance
        )
    }
}
