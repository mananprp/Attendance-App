package com.company.attendance.core.location

/**
 * Real-time location state tracked by LocationMonitor.
 */
sealed interface LocationStatus {
    data object Idle : LocationStatus

    data object Locating : LocationStatus

    data object PermissionDenied : LocationStatus

    data object ServicesDisabled : LocationStatus

    data object MockLocationDetected : LocationStatus

    data class Fix(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val distanceMeters: Float,
        val allowedRadiusMeters: Int,
        val isInside: Boolean,
        val fixElapsedRealtimeMs: Long,
        val fixTimestampMs: Long = System.currentTimeMillis()
    ) : LocationStatus {
        /**
         * Checks if the fix is still fresh (default: within 90 seconds)
         * and meets accuracy threshold (default: <= 50m).
         */
        fun isUsable(
            maxAgeMs: Long = 90_000L,
            maxAccuracyMeters: Float = 50f,
            currentElapsedMs: Long = android.os.SystemClock.elapsedRealtime()
        ): Boolean {
            val ageMs = currentElapsedMs - fixElapsedRealtimeMs
            return ageMs in 0..maxAgeMs && accuracyMeters <= maxAccuracyMeters
        }
    }

    data class Unavailable(val reason: String) : LocationStatus
}
