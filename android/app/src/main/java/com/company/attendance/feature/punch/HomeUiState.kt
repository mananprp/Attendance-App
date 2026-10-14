package com.company.attendance.feature.punch

import com.company.attendance.core.interfaces.GeofenceResult
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.TrustedTime

/**
 * UI State for the Home Attendance Screen.
 */
data class HomeUiState(
    val greeting: String = "Good day",
    val displayName: String = "",
    val userId: String = "",
    val officeDateFormatted: String = "",
    val officeTimeFormatted: String = "--:--:--",
    val isTimeUnverified: Boolean = false,
    val inTimeFormatted: String? = null,
    val outTimeFormatted: String? = null,
    val isPunchInEnabled: Boolean = true,
    val isPunchInBlocked: Boolean = false,
    val punchInButtonText: String = "Punch In",
    val isPunchOutEnabled: Boolean = false,
    val isPunchOutBlocked: Boolean = false,
    val punchOutButtonText: String = "Punch Out",
    val pendingCount: Int = 0,
    val lastSyncedText: String? = null,
    val isSyncing: Boolean = false,
    val isCheckingGeofence: Boolean = false,
    val isNewLocationDay: Boolean = false,
    val shiftDisplayName: String? = null,
    val activeShiftCutoffFormatted: String? = null,
    val geofenceDialogState: GeofenceDialogState? = null,
    val locationCardState: LocationCardState = LocationCardState.Locating,
    val profilePhotoBitmap: android.graphics.Bitmap? = null,
    val snackbarMessage: String? = null
)

/**
 * State of the Home screen Location proximity indicator card.
 */
sealed interface LocationCardState {
    data object Locating : LocationCardState
    data object PermissionNeeded : LocationCardState
    data object ServicesDisabled : LocationCardState
    data object MockDetected : LocationCardState
    data class InOffice(
        val distanceMeters: Float,
        val updatedAgoSeconds: Long = 0L
    ) : LocationCardState
    data class FieldVisit(
        val distanceMeters: Float,
        val isInside: Boolean,
        val updatedAgoSeconds: Long = 0L
    ) : LocationCardState
    data class Outside(
        val distanceMeters: Float,
        val allowedRadiusMeters: Int,
        val updatedAgoSeconds: Long = 0L
    ) : LocationCardState
    data class Unavailable(val reason: String) : LocationCardState
}

/**
 * Dialog states for Geofence & Location verification failures.
 */
sealed interface GeofenceDialogState {
    data class OutsideRadius(
        val distanceMeters: Float,
        val allowedRadiusMeters: Int,
        val userLat: Double = 0.0,
        val userLng: Double = 0.0,
        val officeLat: Double = 0.0,
        val officeLng: Double = 0.0,
        val locationType: String = "OFFICE"
    ) : GeofenceDialogState {
        val isFieldVisit: Boolean
            get() = locationType == "NEW_LOCATION"

        val dialogTitle: String
            get() = if (isFieldVisit) "Outside Field Location Radius" else "Outside Office Radius"

        val formattedMessage: String
            get() = if (isFieldVisit) {
                if (userLat != 0.0) {
                    "You are ${distanceMeters.toInt()}m away from your check-in location.\n\nCurrent GPS: %.5f, %.5f\nCheck-in Location: %.5f, %.5f\n\nYou must be within ${allowedRadiusMeters}m of your check-in location to punch out.".format(userLat, userLng, officeLat, officeLng)
                } else {
                    "You are ${distanceMeters.toInt()}m away from your check-in location. Must be within ${allowedRadiusMeters}m of your check-in location to punch out."
                }
            } else {
                if (userLat != 0.0) {
                    "You are ${distanceMeters.toInt()}m away from the office.\n\nCurrent GPS: %.5f, %.5f\nOffice: %.5f, %.5f\n\nMust be within ${allowedRadiusMeters}m to punch.".format(userLat, userLng, officeLat, officeLng)
                } else {
                    "You are ${distanceMeters.toInt()}m away from the office. Must be within ${allowedRadiusMeters}m to punch."
                }
            }
    }

    data object PermissionRequired : GeofenceDialogState {
        val formattedMessage: String
            get() = "Location permission is required to verify office geofence proximity. Please grant location permission."
    }

    data object LocationServicesDisabled : GeofenceDialogState {
        val formattedMessage: String
            get() = "Location services (GPS) are disabled on this device. Please turn on location services in device settings."
    }

    data object MockLocationDetected : GeofenceDialogState {
        val formattedMessage: String
            get() = "Mock location / Fake GPS detected. Please disable mock location apps in device developer options to record attendance."
    }

    data class LocationUnavailable(val reason: String) : GeofenceDialogState {
        val formattedMessage: String
            get() = "Unable to determine current location ($reason). Please check your GPS signal and try again."
    }
}

/**
 * Single-event navigation triggers from Home screen.
 */
sealed interface HomeNavigationEvent {
    data class NavigateToPunchCapture(
        val punchType: PunchType,
        val latitude: Double,
        val longitude: Double,
        val distanceMeters: Float,
        val locationType: String = "OFFICE"
    ) : HomeNavigationEvent

    data object NavigateToProfile : HomeNavigationEvent
}
