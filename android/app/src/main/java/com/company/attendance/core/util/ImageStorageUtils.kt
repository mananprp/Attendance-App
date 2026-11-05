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
import java.util.concurrent.TimeUnit

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
        val filename = photoUrl.substringAfterLast("/")
        val baseUrl = com.company.attendance.core.network.BackendConfig.getBaseUrl().trimEnd('/')
        val candidateUrls = mutableListOf<String>()

        // 1. Direct configured file URL & v1 upload paths
        candidateUrls.add(com.company.attendance.core.network.BackendConfig.getFileUrl(photoUrl))
        candidateUrls.add("$baseUrl/uploads/photos/$filename")
        candidateUrls.add("$baseUrl/uploads/$filename")
        candidateUrls.add("https://attendance.pess.co.in/uploads/photos/$filename")
        candidateUrls.add("https://attendance.pess.co.in/v1/uploads/photos/$filename")
        candidateUrls.add("https://attendance.pess.co.in/v1/uploads/$filename")
        // 2. Physical & emulator host fallbacks (office LAN / dev server)
        candidateUrls.add("http://${com.company.attendance.core.network.BackendConfig.PHYSICAL_HOST}:${com.company.attendance.core.network.BackendConfig.PORT}/uploads/photos/$filename")
        candidateUrls.add("http://${com.company.attendance.core.network.BackendConfig.EMULATOR_HOST}:${com.company.attendance.core.network.BackendConfig.PORT}/uploads/photos/$filename")
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            candidateUrls.add(photoUrl)
        }

        val fastClient = okHttpClient.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        for (url in candidateUrls.distinct()) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "image/*, */*")
                    .build()
                val response = fastClient.newCall(request).execute()
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
            } catch (_: Exception) {
                // Try next candidate URL
            }
        }
        null
    }
}
