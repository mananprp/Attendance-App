package com.company.attendance.core.network

import com.company.attendance.core.storage.TokenStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    private val tokenStorage: TokenStorage = mockk()
    private val authInterceptor = AuthInterceptor(tokenStorage)

    @Test
    fun `attaches Bearer token when token is present in TokenStorage`() {
        every { tokenStorage.getToken() } returns "jwt-mock-token-123"

        val request = Request.Builder()
            .url("https://api.attendance.company.com/v1/config")
            .build()

        val capturedRequestSlot = slot<Request>()
        val chain: Interceptor.Chain = mockk()
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequestSlot)) } answers {
            Response.Builder()
                .request(capturedRequestSlot.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val response = authInterceptor.intercept(chain)

        assertEquals(200, response.code)
        val authHeader = capturedRequestSlot.captured.header("Authorization")
        assertEquals("Bearer jwt-mock-token-123", authHeader)
    }

    @Test
    fun `does not attach Authorization header when token is null or empty`() {
        every { tokenStorage.getToken() } returns null

        val request = Request.Builder()
            .url("https://api.attendance.company.com/v1/auth/login")
            .build()

        val capturedRequestSlot = slot<Request>()
        val chain: Interceptor.Chain = mockk()
        every { chain.request() } returns request
        every { chain.proceed(capture(capturedRequestSlot)) } answers {
            Response.Builder()
                .request(capturedRequestSlot.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        authInterceptor.intercept(chain)
        val authHeader = capturedRequestSlot.captured.header("Authorization")
        assertNull(authHeader)
    }
}
