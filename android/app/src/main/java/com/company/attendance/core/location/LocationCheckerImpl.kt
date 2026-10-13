package com.company.attendance.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.company.attendance.core.interfaces.GeofenceResult
import com.company.attendance.core.interfaces.LocationChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class LocationCheckerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationChecker {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun checkGeofence(
        officeLat: Double,
        officeLng: Double,
        radiusMeters: Int
    ): GeofenceResult {
        // 1. Permission check
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            return GeofenceResult.PermissionDenied
        }

        // 2. Location services enabled check
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

        if (!isGpsEnabled && !isNetworkEnabled) {
            return GeofenceResult.LocationServicesDisabled
        }

        // 3. Obtain location fix
        val location = getCurrentLocation()
            ?: return GeofenceResult.LocationUnavailable("Unable to acquire location fix. Ensure GPS is active.")

        if (isMockLocation(location)) {
            return GeofenceResult.MockLocationDetected
        }

        val lat = location.latitude
        val lng = location.longitude
        val distance = calculateDistanceMeters(lat, lng, officeLat, officeLng)

        val accuracy = if (location.hasAccuracy()) location.accuracy else 30f
        val tolerance = kotlin.math.min(accuracy, radiusMeters * 0.3f)
        val effectiveDistance = kotlin.math.max(0f, distance - tolerance)

        return if (effectiveDistance <= radiusMeters) {
            GeofenceResult.Inside(
                distanceMeters = distance,
                latitude = lat,
                longitude = lng
            )
        } else {
            GeofenceResult.Outside(
                distanceMeters = distance,
                allowedRadiusMeters = radiusMeters,
                latitude = lat,
                longitude = lng
            )
        }
    }

    override fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val earthRadius = 6371000.0 // Earth's radius in meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2).pow(2.0) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }

    private suspend fun getCurrentLocation(): Location? {
        return kotlinx.coroutines.withTimeoutOrNull(3000L) {
            try {
                suspendCancellableCoroutine { continuation ->
                    val cts = CancellationTokenSource()

                    continuation.invokeOnCancellation {
                        cts.cancel()
                    }

                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cts.token
                        ).addOnSuccessListener { location ->
                            if (location != null) {
                                if (continuation.isActive) continuation.resume(location)
                            } else {
                                // Fallback to last known location
                                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                    if (continuation.isActive) continuation.resume(lastLoc)
                                }.addOnFailureListener {
                                    if (continuation.isActive) continuation.resume(null)
                                }
                            }
                        }.addOnFailureListener {
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    } catch (_: SecurityException) {
                        if (continuation.isActive) continuation.resume(null)
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isMockLocation(location: Location): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            location.isMock
        } else {
            location.isFromMockProvider
        }
    }
}
