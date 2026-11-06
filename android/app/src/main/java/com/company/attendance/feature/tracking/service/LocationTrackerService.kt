package com.company.attendance.feature.tracking.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.company.attendance.core.database.dao.TrackingDao
import com.company.attendance.core.database.entity.TrackingPointEntity
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.FinishTrackingRequestDto
import com.company.attendance.core.network.model.NetworkTrackingPointDto
import com.company.attendance.core.network.model.NetworkTrackingStopDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.tracking.notification.TrackingNotificationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackerService : Service() {

    @Inject
    lateinit var trackingDao: TrackingDao

    @Inject
    lateinit var apiService: AttendanceApiService

    @Inject
    lateinit var tokenStorage: TokenStorage

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: TrackingNotificationManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var activeSessionId: String? = null
    private var isPaused = false
    private var lastLocation: Location? = null
    private var isReceiverRegistered = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleNewLocation(location)
        }
    }

    private val providersChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                checkGpsState()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = TrackingNotificationManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: UUID.randomUUID().toString()
                startTracking(sessionId)
            }
            ACTION_RESUME -> {
                resumeTracking()
            }
            ACTION_STOP -> {
                stopTrackingSession()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking(sessionId: String) {
        activeSessionId = sessionId
        isPaused = false

        // Start foreground notification immediately
        startForeground(
            TrackingNotificationManager.NOTIFICATION_ID,
            notificationManager.buildActiveNotification("0m", "0.0 km")
        )

        serviceScope.launch {
            var session = trackingDao.getSession(sessionId)
            if (session == null) {
                session = TrackingSessionEntity(
                    sessionId = sessionId,
                    startTimeUtc = System.currentTimeMillis(),
                    status = "ACTIVE"
                )
                trackingDao.insertSession(session)
            }
        }

        // Register Providers Changed Receiver to catch GPS being turned off in background
        if (!isReceiverRegistered) {
            registerReceiver(
                providersChangedReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            )
            isReceiverRegistered = true
        }

        // Request Location Updates (every 60s, min distance 10m)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000L)
            .setMinUpdateDistanceMeters(10f)
            .setMinUpdateIntervalMillis(30000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates", e)
        }

        checkGpsState()
    }

    private fun checkGpsState() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isPaused) {
            // Location was turned OFF!
            isPaused = true
            serviceScope.launch {
                activeSessionId?.let { sId ->
                    val session = trackingDao.getSession(sId)
                    if (session != null) {
                        trackingDao.updateSession(session.copy(status = "PAUSED"))
                    }
                }
            }
            // Trigger Heads-Up sound/vibration alert banner
            notificationManager.notifyPaused()
        } else if (isGpsEnabled && isPaused) {
            // Location was turned back ON!
            resumeTracking()
        }
    }

    private fun resumeTracking() {
        isPaused = false
        serviceScope.launch {
            activeSessionId?.let { sId ->
                val session = trackingDao.getSession(sId)
                if (session != null) {
                    trackingDao.updateSession(session.copy(status = "ACTIVE"))
                    updateActiveNotification(session)
                }
            }
        }
    }

    private fun handleNewLocation(location: Location) {
        val sId = activeSessionId ?: return
        if (isPaused) return

        serviceScope.launch {
            val session = trackingDao.getSession(sId) ?: return@launch

            var addedDist = 0f
            if (lastLocation != null) {
                addedDist = lastLocation!!.distanceTo(location)
            }
            lastLocation = location

            val updatedDist = session.totalDistanceM + addedDist

            // Insert tracking point
            val point = TrackingPointEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyM = location.accuracy,
                timestampUtc = System.currentTimeMillis(),
                isPausedGap = false
            )
            trackingDao.insertPoint(point)

            // Update session total distance
            val updatedSession = session.copy(totalDistanceM = updatedDist)
            trackingDao.updateSession(updatedSession)

            updateActiveNotification(updatedSession)
        }
    }

    private fun updateActiveNotification(session: TrackingSessionEntity) {
        val durationMins = (System.currentTimeMillis() - session.startTimeUtc) / 60000
        val durationText = if (durationMins >= 60) {
            "${durationMins / 60}h ${durationMins % 60}m"
        } else {
            "${durationMins}m"
        }

        val distanceKm = (session.totalDistanceM / 1000f)
        val distanceText = String.format("%.1f km", distanceKm)

        val notif = notificationManager.buildActiveNotification(durationText, distanceText)
        val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notifManager.notify(TrackingNotificationManager.NOTIFICATION_ID, notif)
    }

    private fun stopTrackingSession() {
        val sId = activeSessionId

        // Stop updates and unregister receivers
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        if (isReceiverRegistered) {
            try {
                unregisterReceiver(providersChangedReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }

        serviceScope.launch {
            if (sId != null) {
                val session = trackingDao.getSession(sId)
                if (session != null) {
                    val completedSession = session.copy(
                        status = "COMPLETED",
                        endTimeUtc = System.currentTimeMillis()
                    )
                    trackingDao.updateSession(completedSession)

                    // Gather points & stops for backend sync
                    syncCompletedSession(completedSession)
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun syncCompletedSession(session: TrackingSessionEntity) {
        try {
            val points = trackingDao.getPointsForSession(session.sessionId)
            val stops = trackingDao.getStopsForSession(session.sessionId)

            val payload = FinishTrackingRequestDto(
                endTimeUtc = session.endTimeUtc ?: System.currentTimeMillis(),
                totalDistanceM = session.totalDistanceM,
                points = points.map {
                    NetworkTrackingPointDto(
                        id = it.id,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        accuracyM = it.accuracyM,
                        recordedAtUtc = it.timestampUtc,
                        isPausedGap = it.isPausedGap
                    )
                },
                stops = stops.map {
                    NetworkTrackingStopDto(
                        id = it.id,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        stopName = it.stopName,
                        arrivedAtUtc = it.arrivedAtUtc
                    )
                }
            )

            val response = apiService.finishTrackingSession(session.sessionId, payload)
            if (response.isSuccessful) {
                trackingDao.updateSession(session.copy(isSynced = true))
                Log.i(TAG, "Successfully synced tracking session ${session.sessionId}")
            } else {
                Log.w(TAG, "Sync returned ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync tracking session to server", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(providersChangedReceiver)
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTrackerService"
        const val ACTION_START = "com.company.attendance.action.START_TRACKING"
        const val ACTION_RESUME = "com.company.attendance.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.company.attendance.action.STOP_TRACKING"
        const val EXTRA_SESSION_ID = "extra_session_id"

        fun startTracking(context: Context, sessionId: String) {
            val intent = Intent(context, LocationTrackerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun resumeTracking(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
