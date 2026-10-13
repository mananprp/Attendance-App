package com.company.attendance.core.interfaces

/**
 * Result of checking whether the user is within the office geofence.
 */
sealed interface GeofenceResult {
    /**
     * User location is within the office radius.
     * @param distanceMeters Distance in meters from office center point.
     * @param latitude Current latitude.
     * @param longitude Current longitude.
     */
    data class Inside(
        val distanceMeters: Float,
        val latitude: Double,
        val longitude: Double
    ) : GeofenceResult

    /**
     * User location is outside the allowed office radius.
     * @param distanceMeters Calculated distance in meters.
     * @param allowedRadiusMeters Configured maximum radius in meters.
     * @param latitude Current latitude.
     * @param longitude Current longitude.
     */
    data class Outside(
        val distanceMeters: Float,
        val allowedRadiusMeters: Int,
        val latitude: Double,
        val longitude: Double
    ) : GeofenceResult

    /**
     * Location permission has not been granted by user.
     */
    data object PermissionDenied : GeofenceResult

    /**
     * Location services (GPS/Network) are disabled on the device.
     */
    data object LocationServicesDisabled : GeofenceResult

    /**
     * Mock / Fake GPS location detected.
     */
    data object MockLocationDetected : GeofenceResult

    /**
     * Location fix could not be acquired within timeout.
     */
    data class LocationUnavailable(val reason: String) : GeofenceResult
}

/**
 * Contract for acquiring current device coordinates and verifying against office geofence radius.
 *
 * Implemented in Core (Agent 1) / Used by Agent 4 (Punch Flow).
 */
interface LocationChecker {
    /**
     * Checks if current device position is within the configured office geofence.
     *
     * @param officeLat Office latitude from Remote Config
     * @param officeLng Office longitude from Remote Config
     * @param radiusMeters Maximum allowed distance in meters (e.g. 100m)
     */
    suspend fun checkGeofence(
        officeLat: Double,
        officeLng: Double,
        radiusMeters: Int
    ): GeofenceResult

    /**
     * Computes distance in meters between two coordinates using the Haversine / WGS84 formula.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float
}
