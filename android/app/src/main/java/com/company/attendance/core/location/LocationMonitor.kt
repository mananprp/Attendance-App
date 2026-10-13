package com.company.attendance.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.LocationChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

interface LocationMonitor {
    val status: StateFlow<LocationStatus>
    fun start()
    fun stop()
    suspend fun warmUp()
    suspend fun freshFixOrRefresh(
        maxAgeMs: Long = 90_000L,
        maxAccuracyMeters: Float = 50f
    ): LocationStatus
}

@Singleton
class LocationMonitorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val locationChecker: LocationChecker
) : LocationMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val _status = MutableStateFlow<LocationStatus>(LocationStatus.Idle)
    override val status: StateFlow<LocationStatus> = _status.asStateFlow()

    private var isMonitoring = false
    private var locationCallback: LocationCallback? = null

    override fun start() {
        if (isMonitoring) return
        isMonitoring = true

        if (!hasLocationPermission()) {
            _status.value = LocationStatus.PermissionDenied
            return
        }

        if (!isLocationServicesEnabled()) {
            _status.value = LocationStatus.ServicesDisabled
            return
        }

        if (_status.value !is LocationStatus.Fix) {
            _status.value = LocationStatus.Locating
        }

        // Try immediate cached fix first
        scope.launch {
            checkLastKnownLocation()
            startPeriodicUpdates()
        }
    }

    override fun stop() {
        isMonitoring = false
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: Exception) {}
        }
        locationCallback = null
    }

    override suspend fun warmUp() {
        if (!hasLocationPermission() || !isLocationServicesEnabled()) return
        withContext(Dispatchers.Default) {
            val fix = checkLastKnownLocation()
            if (fix == null) {
                requestFreshLocationWithTimeout(3000L)
            }
        }
    }

    override suspend fun freshFixOrRefresh(
        maxAgeMs: Long,
        maxAccuracyMeters: Float
    ): LocationStatus = withContext(Dispatchers.Default) {
        if (!hasLocationPermission()) {
            _status.value = LocationStatus.PermissionDenied
            return@withContext LocationStatus.PermissionDenied
        }

        if (!isLocationServicesEnabled()) {
            _status.value = LocationStatus.ServicesDisabled
            return@withContext LocationStatus.ServicesDisabled
        }

        // 1. Check if current status has a fresh, accurate fix
        val current = _status.value
        if (current is LocationStatus.Fix && current.isUsable(maxAgeMs, maxAccuracyMeters)) {
            return@withContext current
        }

        // 2. Check lastLocation from Google Play Services
        val lastFix = checkLastKnownLocation()
        if (lastFix is LocationStatus.Fix && lastFix.isUsable(maxAgeMs, maxAccuracyMeters)) {
            _status.value = lastFix
            return@withContext lastFix
        }

        // 3. Otherwise request fresh GPS fix with 3.0s strict timeout
        _status.value = LocationStatus.Locating
        val freshFix = requestFreshLocationWithTimeout(3000L)
        if (freshFix != null) {
            _status.value = freshFix
            freshFix
        } else {
            // If fresh fix timed out, but we had any previous fix, use it with a warning or unavailable
            if (current is LocationStatus.Fix) {
                current
            } else if (lastFix is LocationStatus.Fix) {
                lastFix
            } else {
                val unavail = LocationStatus.Unavailable("Location timed out. Please verify GPS signal.")
                _status.value = unavail
                unavail
            }
        }
    }

    private fun startPeriodicUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10_000L // 10 seconds interval
            ).apply {
                setMinUpdateIntervalMillis(5_000L)
                setMinUpdateDistanceMeters(2f)
                setWaitForAccurateLocation(false)
            }.build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    scope.launch {
                        processIncomingLocation(location)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            _status.value = LocationStatus.PermissionDenied
        } catch (_: Exception) {}
    }

    private suspend fun checkLastKnownLocation(): LocationStatus? {
        return try {
            suspendCancellableCoroutine { cont ->
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        if (loc != null) {
                            scope.launch {
                                val status = processIncomingLocation(loc)
                                if (cont.isActive) cont.resume(status)
                            }
                        } else {
                            if (cont.isActive) cont.resume(null)
                        }
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun requestFreshLocationWithTimeout(timeoutMs: Long): LocationStatus? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }

                try {
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            scope.launch {
                                val status = processIncomingLocation(loc)
                                if (cont.isActive) cont.resume(status)
                            }
                        } else {
                            if (cont.isActive) cont.resume(null)
                        }
                    }.addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private suspend fun processIncomingLocation(location: Location): LocationStatus {
        if (isMockLocation(location)) {
            val mockStatus = LocationStatus.MockLocationDetected
            _status.value = mockStatus
            return mockStatus
        }

        val config = remoteConfigRepository.getConfig()
        val distance = locationChecker.calculateDistanceMeters(
            location.latitude,
            location.longitude,
            config.officeLat,
            config.officeLng
        )

        val accuracy = if (location.hasAccuracy()) location.accuracy else 30f
        // Soft margin for indoor jitter (capped at 30% of office radius or accuracy)
        val tolerance = min(accuracy, config.radiusMeters * 0.3f)
        val effectiveDistance = max(0f, distance - tolerance)
        val isInside = effectiveDistance <= config.radiusMeters

        val fixElapsed = if (location.elapsedRealtimeNanos > 0) {
            location.elapsedRealtimeNanos / 1_000_000L
        } else {
            SystemClock.elapsedRealtime()
        }

        val fix = LocationStatus.Fix(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = accuracy,
            distanceMeters = distance,
            allowedRadiusMeters = config.radiusMeters,
            isInside = isInside,
            fixElapsedRealtimeMs = fixElapsed,
            fixTimestampMs = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
        )
        _status.value = fix
        return fix
    }

    @Suppress("DEPRECATION")
    private fun isMockLocation(location: Location): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            location.isFromMockProvider
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun isLocationServicesEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || network
    }
}
