package com.company.attendance.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostFallbackInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            return chain.proceed(request)
        } catch (e: IOException) {
            val originalUrl = request.url
            val host = originalUrl.host

            // Never perform fallback if connecting to production domain or HTTPS
            if (host.contains("attendance.pess.co.in") || originalUrl.isHttps) {
                throw e
            }

            val alternateHost = when (host) {
                BackendConfig.PHYSICAL_HOST -> BackendConfig.EMULATOR_HOST
                BackendConfig.EMULATOR_HOST -> BackendConfig.PHYSICAL_HOST
                else -> null
            }

            if (alternateHost != null) {
                val newUrl = originalUrl.newBuilder()
                    .host(alternateHost)
                    .build()
                val newRequest = request.newBuilder()
                    .url(newUrl)
                    .build()
                return chain.proceed(newRequest)
            }
            throw e
        }
    }
}
