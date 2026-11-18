package com.company.attendance.feature.tracking.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.company.attendance.core.database.dao.TrackingDao
import com.company.attendance.core.database.entity.TrackingPointEntity
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.database.entity.TrackingStopEntity
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.FinishTrackingRequestDto
import com.company.attendance.core.network.model.NetworkTrackingPointDto
import com.company.attendance.core.network.model.NetworkTrackingStopDto
import com.company.attendance.core.network.model.StartTrackingRequestDto
import com.company.attendance.feature.tracking.service.LocationTrackerService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TrackingTripSummary(
    val sessionId: String,
    val startTimeUtc: Long,
    val endTimeUtc: Long,
    val totalDistanceM: Float,
    val points: List<TrackingPointEntity>,
    val stops: List<TrackingStopEntity>
)

@Singleton
class TrackingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingDao: TrackingDao,
    private val apiService: AttendanceApiService
) {
    fun observeActiveSession(): Flow<TrackingSessionEntity?> {
        return trackingDao.observeActiveSession()
    }

    fun observeStopsForSession(sessionId: String): Flow<List<TrackingStopEntity>> {
        return trackingDao.observeStopsForSession(sessionId)
    }

    suspend fun getActiveSession(): TrackingSessionEntity? {
        return withContext(Dispatchers.IO) {
            trackingDao.getActiveSession()
        }
    }

    suspend fun startTracking(): String {
        return withContext(Dispatchers.IO) {
            val sessionId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            val session = TrackingSessionEntity(
                sessionId = sessionId,
                startTimeUtc = startTime,
                status = "ACTIVE"
            )
            trackingDao.insertSession(session)

            // Notify backend asynchronously
            try {
                apiService.startTrackingSession(StartTrackingRequestDto(sessionId, startTime))
            } catch (_: Exception) {}

            LocationTrackerService.startTracking(context, sessionId)
            sessionId
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun addStop(stopName: String): Boolean {
        return withContext(Dispatchers.IO) {
            val session = trackingDao.getActiveSession() ?: return@withContext false
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)

            var loc: Location? = null
            try {
                loc = Tasks.await(
                    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null),
                    5,
                    TimeUnit.SECONDS
                )
            } catch (_: Exception) {}

            if (loc == null) {
                try {
                    loc = Tasks.await(fusedClient.lastLocation, 3, TimeUnit.SECONDS)
                } catch (_: Exception) {}
            }

            val lat = loc?.latitude ?: 0.0
            val lng = loc?.longitude ?: 0.0

            val stop = TrackingStopEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session.sessionId,
                latitude = lat,
                longitude = lng,
                stopName = stopName.ifBlank { "Site Stop" },
                arrivedAtUtc = System.currentTimeMillis()
            )
            trackingDao.insertStop(stop)
            true
        }
    }

    fun observeUnsyncedCompletedSessions(): Flow<List<TrackingSessionEntity>> {
        return trackingDao.observeUnsyncedCompletedSessions()
    }

    suspend fun stopTracking(): TrackingTripSummary? {
        return withContext(Dispatchers.IO) {
            val session = trackingDao.getActiveSession() ?: return@withContext null
            val endTime = System.currentTimeMillis()

            // Save COMPLETED status to local database immediately
            val completedSession = session.copy(
                status = "COMPLETED",
                endTimeUtc = endTime,
                isSynced = false
            )
            trackingDao.updateSession(completedSession)

            LocationTrackerService.stopTracking(context)

            val points = trackingDao.getPointsForSession(session.sessionId)
            val stops = trackingDao.getStopsForSession(session.sessionId)

            TrackingTripSummary(
                sessionId = session.sessionId,
                startTimeUtc = session.startTimeUtc,
                endTimeUtc = endTime,
                totalDistanceM = session.totalDistanceM,
                points = points,
                stops = stops
            )
        }
    }

    suspend fun syncSession(sessionId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val session = trackingDao.getSession(sessionId)
                    ?: return@withContext Result.failure(IllegalStateException("Session $sessionId not found"))

                val points = trackingDao.getPointsForSession(sessionId)
                val stops = trackingDao.getStopsForSession(sessionId)

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

                val response = apiService.finishTrackingSession(sessionId, payload)
                if (response.isSuccessful) {
                    trackingDao.updateSession(session.copy(isSynced = true))
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("Server returned HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun resumeTracking() {
        LocationTrackerService.resumeTracking(context)
    }

    suspend fun getTripSummary(sessionId: String): TrackingTripSummary? {
        return withContext(Dispatchers.IO) {
            val session = trackingDao.getSession(sessionId) ?: return@withContext null
            val points = trackingDao.getPointsForSession(sessionId)
            val stops = trackingDao.getStopsForSession(sessionId)

            TrackingTripSummary(
                sessionId = session.sessionId,
                startTimeUtc = session.startTimeUtc,
                endTimeUtc = session.endTimeUtc ?: System.currentTimeMillis(),
                totalDistanceM = session.totalDistanceM,
                points = points,
                stops = stops
            )
        }
    }
}
