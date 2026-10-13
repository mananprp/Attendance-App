package com.company.attendance.core.network

import com.company.attendance.core.network.model.ApiErrorDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.Response

class ApiException(
    val httpCode: Int,
    val errorCode: String,
    override val message: String,
    val details: Map<String, String>? = null
) : Exception("HTTP $httpCode [$errorCode]: $message")

object NetworkHelper {
    private val json = Json { ignoreUnknownKeys = true }

    fun <T> parseResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.success(body)
            } else {
                Result.failure(ApiException(response.code(), "EMPTY_BODY", "Server returned empty response body"))
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            val parsedError = errorBodyString?.let {
                try {
                    json.decodeFromString<ApiErrorDto>(it)
                } catch (_: Exception) {
                    null
                }
            }

            val exception = ApiException(
                httpCode = response.code(),
                errorCode = parsedError?.code ?: "HTTP_${response.code()}",
                message = parsedError?.message ?: (response.message().ifBlank { "Unknown HTTP Error" }),
                details = parsedError?.details
            )
            Result.failure(exception)
        }
    }
}
