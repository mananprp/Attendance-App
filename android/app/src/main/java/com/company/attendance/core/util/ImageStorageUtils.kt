package com.company.attendance.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.company.attendance.core.network.BackendConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ImageStorageUtils {

    private const val TAG = "ImageStorageUtils"
    var defaultDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun saveProfilePhoto(context: Context, userId: String, bitmap: Bitmap): File? {
        return try {
            val file = File(context.filesDir, "profile_${userId}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile photo for $userId: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile photo for $userId: ${e.message}", e)
            null
        }
    }

    fun deleteProfilePhoto(context: Context, userId: String): Boolean {
        return try {
            val file = File(context.filesDir, "profile_${userId}.jpg")
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting profile photo for $userId: ${e.message}", e)
            false
        }
    }

    fun deleteAllProfilePhotos(context: Context) {
        try {
            context.filesDir.listFiles()
                ?.filter { it.name.startsWith("profile_") && it.name.endsWith(".jpg") }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all profile photos: ${e.message}", e)
        }
    }

    fun deleteAllUserData(context: Context) {
        try {
            // 1. Delete all profile photos
            deleteAllProfilePhotos(context)

            // 2. Delete all punch capture frames
            val filesDir = context.filesDir
            if (filesDir != null) {
                val punchesDir = File(filesDir, "punches")
                if (punchesDir.exists()) {
                    punchesDir.deleteRecursively()
                }
            }

            // 3. Delete all cached image/network files
            context.cacheDir?.listFiles()?.forEach { it.deleteRecursively() }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user data: ${e.message}", e)
        }
    }

    suspend fun fetchAndSaveProfilePhoto(
        context: Context,
        userId: String,
        photoUrl: String,
        okHttpClient: OkHttpClient
    ): Bitmap? = withContext(defaultDispatcher) {
        val targetUrl = BackendConfig.getFileUrl(photoUrl)
        val separator = if (targetUrl.contains("?")) "&" else "?"
        val cacheBustedUrl = "$targetUrl${separator}uid=$userId&t=${System.currentTimeMillis()}"
        Log.d(TAG, "Fetching profile photo for user $userId from URL: $cacheBustedUrl")

        try {
            val request = Request.Builder()
                .url(cacheBustedUrl)
                .header("Accept", "image/*, */*")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val statusCode = response.code
            Log.d(TAG, "Photo download HTTP status $statusCode for user $userId (URL: $targetUrl)")

            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        saveProfilePhoto(context, userId, bitmap)
                        Log.d(TAG, "Profile photo downloaded, decoded, and saved for user: $userId")
                        return@withContext bitmap
                    } else {
                        Log.e(TAG, "Failed to decode photo bytes into Bitmap for user: $userId")
                    }
                } else {
                    Log.w(TAG, "Empty response body received for profile photo of user: $userId")
                }
            } else {
                Log.w(TAG, "Profile photo download failed with HTTP status $statusCode for URL: $targetUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during profile photo download for user $userId from $targetUrl: ${e.message}", e)
        }
        null
    }
}
