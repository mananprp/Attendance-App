package com.company.attendance.core.network

import android.os.Build
import com.company.attendance.BuildConfig

object BackendConfig {
    const val EMULATOR_HOST = "10.0.2.2"
    const val PHYSICAL_HOST = "172.31.122.136"
    const val PORT = 3000
    const val DEFAULT_PRODUCTION_URL = "https://attendance.pess.co.in/"

    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    fun getBaseHost(): String = if (isEmulator()) EMULATOR_HOST else PHYSICAL_HOST

    fun getBaseUrl(): String {
        val configuredUrl = BuildConfig.SERVER_BASE_URL.trim()
        val targetUrl = if (configuredUrl.isNotEmpty() && !configuredUrl.contains("10.0.2.2") && !configuredUrl.contains("172.31.122.136")) {
            configuredUrl
        } else {
            DEFAULT_PRODUCTION_URL
        }
        val trimmed = targetUrl.trimEnd('/')
        return if (trimmed.endsWith("/v1")) "$trimmed/" else "$trimmed/v1/"
    }

    fun getFileUrl(path: String): String {
        val cleanPath = if (path.contains("/uploads/")) {
            "uploads/" + path.substringAfter("/uploads/")
        } else {
            path.removePrefix("/")
        }
        val configuredUrl = BuildConfig.SERVER_BASE_URL.trim()
        val targetUrl = if (configuredUrl.isNotEmpty() && !configuredUrl.contains("10.0.2.2") && !configuredUrl.contains("172.31.122.136")) {
            configuredUrl
        } else {
            DEFAULT_PRODUCTION_URL
        }
        val baseRoot = targetUrl.trimEnd('/').removeSuffix("/v1")
        return "$baseRoot/$cleanPath"
    }
}
