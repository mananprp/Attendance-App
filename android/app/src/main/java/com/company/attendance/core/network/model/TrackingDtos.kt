package com.company.attendance.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StartTrackingRequestDto(
    @SerialName("sessionId")
    val sessionId: String,

    @SerialName("startTimeUtc")
    val startTimeUtc: Long
)

@Serializable
data class StartTrackingResponseDto(
    @SerialName("success")
    val success: Boolean,

    @SerialName("sessionId")
    val sessionId: String,

    @SerialName("status")
    val status: String
)

@Serializable
data class NetworkTrackingPointDto(
    @SerialName("id")
    val id: String,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("accuracyM")
    val accuracyM: Float? = null,

    @SerialName("recordedAtUtc")
    val recordedAtUtc: Long,

    @SerialName("isPausedGap")
    val isPausedGap: Boolean = false
)

@Serializable
data class NetworkTrackingStopDto(
    @SerialName("id")
    val id: String,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("stopName")
    val stopName: String? = null,

    @SerialName("arrivedAtUtc")
    val arrivedAtUtc: Long
)

@Serializable
data class FinishTrackingRequestDto(
    @SerialName("endTimeUtc")
    val endTimeUtc: Long,

    @SerialName("totalDistanceM")
    val totalDistanceM: Float,

    @SerialName("points")
    val points: List<NetworkTrackingPointDto>,

    @SerialName("stops")
    val stops: List<NetworkTrackingStopDto>
)

@Serializable
data class FinishTrackingResponseDto(
    @SerialName("success")
    val success: Boolean,

    @SerialName("sessionId")
    val sessionId: String,

    @SerialName("totalDistanceM")
    val totalDistanceM: Float? = null,

    @SerialName("stopsCount")
    val stopsCount: Int = 0,

    @SerialName("totalPointsSaved")
    val totalPointsSaved: Int = 0
)
