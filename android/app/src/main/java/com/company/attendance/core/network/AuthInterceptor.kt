package com.company.attendance.core.network

import com.company.attendance.core.storage.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenStorage.getToken()

        val requestBuilder = originalRequest.newBuilder()

        val path = originalRequest.url.encodedPath
        if (originalRequest.header("Accept") == null) {
            if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") || path.contains("/uploads/")) {
                requestBuilder.header("Accept", "image/*, */*")
            } else {
                requestBuilder.header("Accept", "application/json")
            }
        }

        if (!token.isNullOrBlank() && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        // If 401 Unauthorized, we could also notify token expiry if needed
        return response
    }
}
