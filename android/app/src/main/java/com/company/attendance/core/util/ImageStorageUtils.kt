package com.company.attendance.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ImageStorageUtils {

    fun saveProfilePhoto(context: Context, userId: String, bitmap: Bitmap): File? {
        return try {
            val file = File(context.filesDir, "profile_${userId}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    fun loadProfilePhoto(context: Context, userId: String): Bitmap? {
        return try {
            val file = File(context.filesDir, "profile_${userId}.jpg")
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAndSaveProfilePhoto(
        context: Context,
        userId: String,
        photoUrl: String,
        okHttpClient: OkHttpClient
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val fullUrl = if (photoUrl.startsWith("https://") && !photoUrl.contains("localhost") && !photoUrl.contains("127.0.0.1")) {
                photoUrl
            } else {
                com.company.attendance.core.network.BackendConfig.getFileUrl(photoUrl)
            }

            val request = Request.Builder()
                .url(fullUrl)
                .header("Accept", "image/*, */*")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        saveProfilePhoto(context, userId, bitmap)
                        return@withContext bitmap
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
