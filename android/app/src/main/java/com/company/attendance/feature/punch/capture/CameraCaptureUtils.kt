package com.company.attendance.feature.punch.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

object CameraCaptureUtils {

    /**
     * Converts a 512-float array into a 2048-byte array for SQLite BLOB storage.
     */
    fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) {
            byteBuffer.putFloat(f)
        }
        return byteBuffer.array()
    }

    /**
     * Converts a byte array BLOB from SQLite back into a 512-float vector.
     */
    fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatArray = FloatArray(bytes.size / 4)
        byteBuffer.asFloatBuffer().get(floatArray)
        return floatArray
    }

    /**
     * Saves a punch frame Bitmap to app-private internal storage (/data/data/.../files/punches/<uuid>.jpg).
     * Compresses with JPEG quality 90.
     */
    fun savePunchImageToInternalStorage(context: Context, bitmap: Bitmap): String {
        val punchesDir = File(context.filesDir, "punches")
        if (!punchesDir.exists()) {
            punchesDir.mkdirs()
        }
        val file = File(punchesDir, "punch_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    /**
     * Converts a CameraX ImageProxy to a rotated Bitmap suitable for ONNX face pipeline.
     */
    fun imageProxyToRotatedBitmap(imageProxy: ImageProxy): Bitmap {
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
}
