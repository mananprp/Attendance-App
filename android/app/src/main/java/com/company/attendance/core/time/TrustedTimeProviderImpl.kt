package com.company.attendance.core.time

import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.model.TrustedTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrustedTimeProviderImpl @Inject constructor(
    private val sntpClient: SntpClient,
    private val clockWrapper: ClockWrapper,
    private val remoteConfigRepository: RemoteConfigRepository
) : TrustedTimeProvider {

    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var ntpAnchorEpochMs: Long? = null
    private var elapsedRealtimeAnchorMs: Long? = null

    private var officeTimezone: String = "Asia/Kolkata"

    private val _currentTimeFlow = MutableStateFlow(computeCurrentTrustedTime())
    override val currentTimeFlow: StateFlow<TrustedTime> = _currentTimeFlow.asStateFlow()

    init {
        // Observe dynamic remote config timezone updates
        externalScope.launch {
            remoteConfigRepository.observeConfig().collect { config ->
                officeTimezone = config.timezone
                _currentTimeFlow.value = computeCurrentTrustedTime()
            }
        }

        // Initial sync attempt in background
        externalScope.launch {
            syncNtp()
        }

        // Periodic ticker to update currentTimeFlow
        externalScope.launch {
            while (isActive) {
                delay(1000)
                _currentTimeFlow.value = computeCurrentTrustedTime()
            }
        }
    }

    override suspend fun getTrustedTime(): TrustedTime {
        // If not yet anchored, try quick sync once with 1.5s max timeout to prevent blocking punch recording
        if (ntpAnchorEpochMs == null) {
            kotlinx.coroutines.withTimeoutOrNull(1500L) {
                syncNtp()
            }
        }
        return computeCurrentTrustedTime()
    }

    override suspend fun syncNtp(): Boolean = mutex.withLock {
        val ntpTime = sntpClient.fetchNtpTime()
        return if (ntpTime != null && ntpTime > 0) {
            val elapsed = clockWrapper.elapsedRealtime()
            ntpAnchorEpochMs = ntpTime
            elapsedRealtimeAnchorMs = elapsed
            _currentTimeFlow.value = computeCurrentTrustedTime()
            true
        } else {
            false
        }
    }

    override suspend fun getOfficeLocalDate(): LocalDate {
        return getTrustedTime().localDate
    }

    override fun isNtpSynchronized(): Boolean {
        return ntpAnchorEpochMs != null && elapsedRealtimeAnchorMs != null
    }

    fun setAnchorForTesting(ntpEpochMs: Long, elapsedRealtimeMs: Long) {
        ntpAnchorEpochMs = ntpEpochMs
        elapsedRealtimeAnchorMs = elapsedRealtimeMs
        _currentTimeFlow.value = computeCurrentTrustedTime()
    }

    private fun getOfficeZoneId(): ZoneId {
        return try {
            ZoneId.of(officeTimezone)
        } catch (_: Exception) {
            ZoneId.of("Asia/Kolkata")
        }
    }

    private fun computeCurrentTrustedTime(): TrustedTime {
        val zoneId = getOfficeZoneId()
        val anchorNtp = ntpAnchorEpochMs
        val anchorElapsed = elapsedRealtimeAnchorMs

        return if (anchorNtp != null && anchorElapsed != null) {
            val elapsedDelta = clockWrapper.elapsedRealtime() - anchorElapsed
            val currentTrustedEpoch = anchorNtp + elapsedDelta
            val localDate = Instant.ofEpochMilli(currentTrustedEpoch)
                .atZone(zoneId)
                .toLocalDate()

            TrustedTime(
                epochMillis = currentTrustedEpoch,
                source = TimeSource.NTP,
                isUnverified = false,
                localDate = localDate,
                timezoneId = zoneId.id
            )
        } else {
            val deviceEpoch = clockWrapper.currentTimeMillis()
            val localDate = Instant.ofEpochMilli(deviceEpoch)
                .atZone(zoneId)
                .toLocalDate()

            TrustedTime(
                epochMillis = deviceEpoch,
                source = TimeSource.DEVICE,
                isUnverified = true,
                localDate = localDate,
                timezoneId = zoneId.id
            )
        }
    }
}
