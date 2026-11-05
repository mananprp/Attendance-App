package com.company.attendance.design.navigation

import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.location.LocationMonitor
import com.company.attendance.core.location.LocationStatus
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.design.components.CenterPunchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GlobalPunchEvent {
    data class NavigateToCapture(
        val punchType: PunchType,
        val locationType: String = "OFFICE",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val distanceMeters: Float = 0.0f
    ) : GlobalPunchEvent

    data class ShowGeofenceError(
        val title: String,
        val message: String
    ) : GlobalPunchEvent
}

@Singleton
class PunchCoordinator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val punchRepository: PunchRepository,
    private val trustedTimeProvider: TrustedTimeProvider,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val locationChecker: LocationChecker,
    private val locationMonitor: LocationMonitor
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isCheckingGeofence = MutableStateFlow(false)
    val isCheckingGeofence: StateFlow<Boolean> = _isCheckingGeofence.asStateFlow()

    private val _centerPunchState = MutableStateFlow(CenterPunchState.PUNCH_IN)
    val centerPunchState: StateFlow<CenterPunchState> = _centerPunchState.asStateFlow()

    private val _punchEvents = MutableSharedFlow<GlobalPunchEvent>(extraBufferCapacity = 1)
    val punchEvents: SharedFlow<GlobalPunchEvent> = _punchEvents.asSharedFlow()

    init {
        scope.launch {
            tokenStorage.sessionFlow.collect { session ->
                val userId = session?.userId ?: tokenStorage.getUserId() ?: ""
                if (userId.isNotBlank()) {
                    runCatching {
                        val todayDate = trustedTimeProvider.getOfficeLocalDate().toString()
                        val summary = punchRepository.getDaySummary(userId, todayDate)
                        updateCenterState(summary)
                    }
                }
            }
        }
    }

    fun updateCenterState(summary: DaySummary?) {
        val hasIn = summary?.inTimeUtcEpochMs != null
        val hasOut = summary?.outTimeUtcEpochMs != null
        _centerPunchState.value = when {
            hasIn && !hasOut -> CenterPunchState.PUNCH_OUT
            hasIn && hasOut -> CenterPunchState.COMPLETE
            else -> CenterPunchState.PUNCH_IN
        }
    }

    fun triggerPunch() {
        if (_isCheckingGeofence.value) return

        val userId = tokenStorage.getUserId() ?: ""
        if (userId.isBlank()) return

        scope.launch {
            _isCheckingGeofence.value = true
            try {
                val todayDateLocal = trustedTimeProvider.getOfficeLocalDate().toString()
                val summary = punchRepository.getDaySummary(userId, todayDateLocal)
                updateCenterState(summary)

                val punchType = if (summary?.inTimeUtcEpochMs != null && summary.outTimeUtcEpochMs == null) {
                    PunchType.OUT
                } else {
                    PunchType.IN
                }

                // If user is punching in, resolve any earlier incomplete day summaries
                if (punchType == PunchType.IN) {
                    punchRepository.resolvePastMissedPunches(userId, todayDateLocal)
                }

                // If user has a NEW_LOCATION punch-in, handle anchor checking
                if (summary?.locationType == "NEW_LOCATION" && punchType == PunchType.OUT) {
                    val anchorLat = summary.anchorLat ?: 0.0
                    val anchorLng = summary.anchorLng ?: 0.0
                    val locStatus = locationMonitor.freshFixOrRefresh(maxAgeMs = 90_000L, maxAccuracyMeters = 50f)
                    if (locStatus is LocationStatus.Fix) {
                        val distance = locationChecker.calculateDistanceMeters(
                            locStatus.latitude,
                            locStatus.longitude,
                            anchorLat,
                            anchorLng
                        )
                        if (distance <= 100f || (anchorLat == 0.0 && anchorLng == 0.0)) {
                            _punchEvents.tryEmit(
                                GlobalPunchEvent.NavigateToCapture(
                                    punchType = PunchType.OUT,
                                    locationType = "NEW_LOCATION",
                                    latitude = locStatus.latitude,
                                    longitude = locStatus.longitude,
                                    distanceMeters = distance
                                )
                            )
                        } else {
                            _punchEvents.tryEmit(
                                GlobalPunchEvent.ShowGeofenceError(
                                    title = "Outside Field Site Radius",
                                    message = "You are ${distance.toInt()}m from your check-in spot (allowed: 100m)."
                                )
                            )
                        }
                    } else {
                        handleNonFixLocation(locStatus)
                    }
                    return@launch
                }

                // Standard Office Geofence verification
                val locationStatus = locationMonitor.freshFixOrRefresh(
                    maxAgeMs = 90_000L,
                    maxAccuracyMeters = 50f
                )

                when (locationStatus) {
                    is LocationStatus.Fix -> {
                        if (locationStatus.isInside) {
                            _punchEvents.tryEmit(
                                GlobalPunchEvent.NavigateToCapture(
                                    punchType = punchType,
                                    locationType = "OFFICE",
                                    latitude = locationStatus.latitude,
                                    longitude = locationStatus.longitude,
                                    distanceMeters = locationStatus.distanceMeters
                                )
                            )
                        } else {
                            _punchEvents.tryEmit(
                                GlobalPunchEvent.ShowGeofenceError(
                                    title = "Outside Office Geofence",
                                    message = "You are ${locationStatus.distanceMeters.toInt()}m from office (allowed: ${locationStatus.allowedRadiusMeters}m)."
                                )
                            )
                        }
                    }
                    else -> handleNonFixLocation(locationStatus)
                }
            } catch (e: Exception) {
                val config: AppConfig = remoteConfigRepository.getConfig()
                _punchEvents.tryEmit(
                    GlobalPunchEvent.NavigateToCapture(
                        punchType = PunchType.IN,
                        locationType = "OFFICE",
                        latitude = config.officeLat,
                        longitude = config.officeLng,
                        distanceMeters = 0f
                    )
                )
            } finally {
                _isCheckingGeofence.value = false
            }
        }
    }

    private fun handleNonFixLocation(status: LocationStatus) {
        when (status) {
            is LocationStatus.PermissionDenied -> {
                _punchEvents.tryEmit(
                    GlobalPunchEvent.ShowGeofenceError(
                        title = "Location Permission Required",
                        message = "Please grant location permission to verify your presence."
                    )
                )
            }
            is LocationStatus.ServicesDisabled -> {
                _punchEvents.tryEmit(
                    GlobalPunchEvent.ShowGeofenceError(
                        title = "Location Services Disabled",
                        message = "Please enable GPS/location services on your device."
                    )
                )
            }
            is LocationStatus.MockLocationDetected -> {
                _punchEvents.tryEmit(
                    GlobalPunchEvent.ShowGeofenceError(
                        title = "Mock Location Detected",
                        message = "Please disable mock location / fake GPS apps to proceed."
                    )
                )
            }
            is LocationStatus.Unavailable -> {
                _punchEvents.tryEmit(
                    GlobalPunchEvent.ShowGeofenceError(
                        title = "Location Unavailable",
                        message = status.reason
                    )
                )
            }
            else -> {
                _punchEvents.tryEmit(
                    GlobalPunchEvent.ShowGeofenceError(
                        title = "Locating...",
                        message = "Acquiring GPS fix. Please try again in a moment."
                    )
                )
            }
        }
    }
}
