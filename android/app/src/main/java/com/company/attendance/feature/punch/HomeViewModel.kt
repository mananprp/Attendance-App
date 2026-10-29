package com.company.attendance.feature.punch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.GeofenceResult
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.TrustedTime
import com.company.attendance.core.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.company.attendance.core.model.ShiftType
import com.company.attendance.core.util.ShiftEvaluator
import android.content.Context
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.util.ImageStorageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.util.Locale
import com.company.attendance.core.location.LocationMonitor
import com.company.attendance.core.location.LocationStatus
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.model.DayStatus
import com.company.attendance.feature.punch.capture.CameraCaptureUtils
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao,
    private val apiService: AttendanceApiService,
    private val okHttpClient: OkHttpClient,
    private val trustedTimeProvider: TrustedTimeProvider,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val punchRepository: PunchRepository,
    private val locationChecker: LocationChecker,
    private val locationMonitor: LocationMonitor,
    private val syncScheduler: SyncScheduler,
    private val faceVerifier: FaceVerifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<HomeNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var daySummaryJob: Job? = null
    private var lastObservedDate: LocalDate? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault())
    private val punchTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val syncTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private var currentDaySummary: DaySummary? = null

    init {
        loadUserSession()
        observeTrustedClock()
        observeSyncState()
        observeLocationStatus()
        refreshRemoteConfig()
    }

    fun onScreenStarted() {
        locationMonitor.start()
        viewModelScope.launch {
            syncScheduler.refreshPendingCount()
        }
    }

    fun onScreenStopped() {
        locationMonitor.stop()
    }

    fun onRefreshLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingGeofence = true) }
            locationMonitor.freshFixOrRefresh(maxAgeMs = 0L)
            _uiState.update { it.copy(isCheckingGeofence = false) }
        }
    }

    private fun observeLocationStatus() {
        viewModelScope.launch {
            locationMonitor.status.collectLatest { status ->
                val cardState = when (status) {
                    is LocationStatus.Idle,
                    is LocationStatus.Locating -> LocationCardState.Locating
                    is LocationStatus.PermissionDenied -> LocationCardState.PermissionNeeded
                    is LocationStatus.ServicesDisabled -> LocationCardState.ServicesDisabled
                    is LocationStatus.MockLocationDetected -> LocationCardState.MockDetected
                    is LocationStatus.Fix -> {
                        val ageSec = ((System.currentTimeMillis() - status.fixTimestampMs) / 1000L).coerceAtLeast(0L)
                        val summary = currentDaySummary
                        if (summary?.locationType == "NEW_LOCATION" && summary.inTimeUtcEpochMs != null && summary.outTimeUtcEpochMs == null) {
                            val anchorLat = summary.anchorLat
                            val anchorLng = summary.anchorLng
                            if (anchorLat != null && anchorLng != null && (anchorLat != 0.0 || anchorLng != 0.0)) {
                                val fieldDistance = locationChecker.calculateDistanceMeters(
                                    status.latitude,
                                    status.longitude,
                                    anchorLat,
                                    anchorLng
                                )
                                LocationCardState.FieldVisit(
                                    distanceMeters = fieldDistance,
                                    isInside = fieldDistance <= 100f,
                                    updatedAgoSeconds = ageSec
                                )
                            } else {
                                LocationCardState.FieldVisit(
                                    distanceMeters = 0f,
                                    isInside = true,
                                    updatedAgoSeconds = ageSec
                                )
                            }
                        } else if (status.isInside) {
                            // Warm up ONNX models in background so capture screen opens instantly with models in RAM
                            viewModelScope.launch {
                                try {
                                    faceVerifier.prepare()
                                } catch (_: Exception) {}
                            }
                            LocationCardState.InOffice(
                                distanceMeters = status.distanceMeters,
                                updatedAgoSeconds = ageSec
                            )
                        } else {
                            LocationCardState.Outside(
                                distanceMeters = status.distanceMeters,
                                allowedRadiusMeters = status.allowedRadiusMeters,
                                updatedAgoSeconds = ageSec
                            )
                        }
                    }
                    is LocationStatus.Unavailable -> LocationCardState.Unavailable(status.reason)
                }
                _uiState.update { it.copy(locationCardState = cardState) }
            }
        }
    }

    private fun refreshRemoteConfig() {
        viewModelScope.launch {
            val result = remoteConfigRepository.fetchAndCacheConfig()
            val config = result.getOrNull() ?: remoteConfigRepository.getConfig()
            syncScheduler.scheduleConfiguredWindows(config.syncWindows, config.syncJitterMinutes)
            syncScheduler.refreshPendingCount()
        }
    }

    private fun loadUserSession() {
        val userSession = tokenStorage.getUserSession()
        val userId = userSession?.userId ?: tokenStorage.getUserId() ?: ""
        val displayName = userSession?.displayName ?: tokenStorage.getDisplayName() ?: ""
        val localPhoto = ImageStorageUtils.loadProfilePhoto(context, userId)

        _uiState.update { current ->
            current.copy(
                userId = userId,
                displayName = displayName,
                profilePhotoBitmap = localPhoto
            )
        }

        viewModelScope.launch {
            if (userId.isNotBlank()) {
                // 1. Synchronize monthly history to ensure today's punches & calendar are up to date
                fetchAndCacheMonthlyHistory(userId)

                // 2. Self-healing photo & face embedding verification
                val userEntity = userLocalDao.getUserDirect(userId)
                val needsPhotoDownload = (localPhoto == null && tokenStorage.hasPhoto())
                val needsEmbeddingExtraction = (userEntity?.referenceEmbedding == null && tokenStorage.hasPhoto())

                if (needsPhotoDownload || needsEmbeddingExtraction) {
                    try {
                        var activeBmp = localPhoto
                        if (activeBmp == null) {
                            val response = apiService.getActivePhoto()
                            val result = NetworkHelper.parseResponse(response)
                            result.onSuccess { photoDetail ->
                                activeBmp = ImageStorageUtils.fetchAndSaveProfilePhoto(
                                    context = context,
                                    userId = userId,
                                    photoUrl = photoDetail.photoUrl,
                                    okHttpClient = okHttpClient
                                )
                                if (activeBmp != null) {
                                    _uiState.update { it.copy(profilePhotoBitmap = activeBmp) }
                                }
                            }
                        }

                        if (activeBmp != null && userEntity?.referenceEmbedding == null) {
                            val extractResult = faceVerifier.extractReferenceEmbedding(activeBmp!!)
                            extractResult.onSuccess { floats ->
                                val bytes = CameraCaptureUtils.floatArrayToByteArray(floats)
                                userLocalDao.updateReferenceEmbedding(userId, bytes)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private suspend fun fetchAndCacheMonthlyHistory(userId: String) {
        runCatching {
            val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val response = apiService.getMonthlyAttendance(currentMonth)
            val parsed = NetworkHelper.parseResponse(response)
            parsed.onSuccess { attendanceDto ->
                val summaries = attendanceDto.days.map { dayDto ->
                    DaySummary(
                        userId = userId,
                        dateLocal = dayDto.dateLocal,
                        inTimeUtcEpochMs = dayDto.inTime?.let { parseIsoToEpochMs(it) },
                        outTimeUtcEpochMs = dayDto.outTime?.let { parseIsoToEpochMs(it) },
                        status = runCatching { DayStatus.valueOf(dayDto.status) }.getOrDefault(DayStatus.NO_RECORD),
                        timeUnverified = dayDto.timeUnverified,
                        shiftType = runCatching { ShiftType.valueOf(dayDto.shiftType) }.getOrDefault(ShiftType.GENERAL)
                    )
                }
                punchRepository.upsertDaySummaries(summaries)
            }
        }
    }

    private fun parseIsoToEpochMs(isoString: String): Long? {
        return runCatching {
            Instant.parse(isoString).toEpochMilli()
        }.getOrNull()
    }

    /**
     * Observes real-time clock flow from TrustedTimeProvider.
     * Continuously updates time/date display, greeting, and detects midnight rollover (00:00).
     */
    private fun observeTrustedClock() {
        viewModelScope.launch {
            trustedTimeProvider.currentTimeFlow.collectLatest { trustedTime ->
                val zoneId = ZoneId.of(trustedTime.timezoneId)
                val zonedDateTime = Instant.ofEpochMilli(trustedTime.epochMillis).atZone(zoneId)

                val greeting = when (zonedDateTime.hour) {
                    in 0..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    else -> "Good evening"
                }

                val formattedDate = zonedDateTime.format(dateFormatter)
                val formattedTime = zonedDateTime.format(timeFormatter)

                _uiState.update { current ->
                    current.copy(
                        greeting = greeting,
                        officeDateFormatted = formattedDate,
                        officeTimeFormatted = formattedTime,
                        isTimeUnverified = trustedTime.isUnverified
                    )
                }

                val localDateTime = zonedDateTime.toLocalDateTime()

                // Detect date rollover or shift cutoff expiry
                val shouldRebind = lastObservedDate != trustedTime.localDate || currentDaySummary?.let {
                    val isExpired = it.inTimeUtcEpochMs != null && it.outTimeUtcEpochMs == null &&
                            ShiftEvaluator.isSessionExpired(
                                it.shiftType,
                                LocalDate.parse(it.dateLocal),
                                localDateTime
                            )
                    isExpired
                } ?: false

                if (shouldRebind) {
                    lastObservedDate = trustedTime.localDate
                    val userId = tokenStorage.getUserId() ?: ""
                    if (userId.isNotBlank()) {
                        bindActiveShiftObservation(userId, trustedTime.localDate, zoneId, localDateTime)
                    }
                }
            }
        }
    }

    /**
     * Re-binds observation of the DaySummary taking shifts and rollover cutoffs into account.
     * If yesterday has an active, unclosed session whose cutoff has NOT yet passed (e.g. Night Shift crossing midnight),
     * that session remains the actively observed session!
     */
    private fun bindActiveShiftObservation(
        userId: String,
        localDate: LocalDate,
        zoneId: ZoneId,
        currentDateTime: LocalDateTime
    ) {
        daySummaryJob?.cancel()
        daySummaryJob = viewModelScope.launch {
            val todayStr = localDate.toString()
            val yesterdayStr = localDate.minusDays(1).toString()

            val yesterdaySummary = punchRepository.getDaySummary(userId, yesterdayStr)
            val isYesterdayActive = yesterdaySummary != null &&
                    yesterdaySummary.inTimeUtcEpochMs != null &&
                    yesterdaySummary.outTimeUtcEpochMs == null &&
                    !ShiftEvaluator.isSessionExpired(
                        yesterdaySummary.shiftType,
                        localDate.minusDays(1),
                        currentDateTime
                    )

            val targetDateStr = if (isYesterdayActive) yesterdayStr else todayStr

            if (yesterdaySummary != null && yesterdaySummary.inTimeUtcEpochMs != null && yesterdaySummary.outTimeUtcEpochMs == null &&
                ShiftEvaluator.isSessionExpired(yesterdaySummary.shiftType, localDate.minusDays(1), currentDateTime)) {
                punchRepository.resolvePastMissedPunches(userId, todayStr)
            }

            punchRepository.observeDaySummary(userId, targetDateStr).collectLatest { summary ->
                updateStateFromDaySummary(summary, zoneId, currentDateTime)
            }
        }
    }

    /**
     * Applies the Home State Machine business rules with shift awareness:
     * 1. No punch for shift: Punch In ENABLED, Punch Out DISABLED.
     * 2. Active Shift: Punch In BLOCKED (with time), Punch Out ENABLED until shift cutoff.
     * 3. Completed Shift: Both BLOCKED.
     * 4. Expired Cutoff: Marked MISSED_PUNCH and reset.
     */
    private fun updateStateFromDaySummary(summary: DaySummary?, zoneId: ZoneId, currentDateTime: LocalDateTime) {
        currentDaySummary = summary

        val shift = if (summary != null && summary.shiftType != ShiftType.GENERAL) {
            summary.shiftType
        } else if (summary?.inTimeUtcEpochMs != null) {
            val inDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(summary.inTimeUtcEpochMs), zoneId)
            ShiftEvaluator.determineShift(inDateTime).first
        } else {
            ShiftEvaluator.determineShift(currentDateTime).first
        }
        val shiftDisplayName = ShiftEvaluator.getShiftDisplayName(shift)

        // Check if active session cutoff has expired
        val isExpired = if (summary != null && summary.inTimeUtcEpochMs != null && summary.outTimeUtcEpochMs == null) {
            val workDate = runCatching { LocalDate.parse(summary.dateLocal) }.getOrDefault(currentDateTime.toLocalDate())
            ShiftEvaluator.isSessionExpired(summary.shiftType, workDate, currentDateTime)
        } else false

        val hasIn = summary?.inTimeUtcEpochMs != null && !isExpired
        val hasOut = summary?.outTimeUtcEpochMs != null || (summary != null && isExpired)

        val inTimeFormatted = summary?.inTimeUtcEpochMs?.let { epochMs ->
            Instant.ofEpochMilli(epochMs).atZone(zoneId).format(punchTimeFormatter)
        }
        val outTimeFormatted = summary?.outTimeUtcEpochMs?.let { epochMs ->
            Instant.ofEpochMilli(epochMs).atZone(zoneId).format(punchTimeFormatter)
        }

        val isPunchInEnabled = !hasIn
        val isPunchInBlocked = hasIn
        val punchInButtonText = if (hasIn) "Punched In at $inTimeFormatted" else "Punch In"

        val isPunchOutEnabled = hasIn && !hasOut
        val isPunchOutBlocked = hasOut || !hasIn
        val punchOutButtonText = if (hasOut) {
            if (isExpired && summary?.outTimeUtcEpochMs == null) "Shift Expired (Missed)" else "Punched Out at $outTimeFormatted"
        } else "Punch Out"

        val isNewLocationDay = summary?.locationType == "NEW_LOCATION"

        _uiState.update { current ->
            current.copy(
                inTimeFormatted = inTimeFormatted,
                outTimeFormatted = outTimeFormatted,
                isPunchInEnabled = isPunchInEnabled,
                isPunchInBlocked = isPunchInBlocked,
                punchInButtonText = punchInButtonText,
                isPunchOutEnabled = isPunchOutEnabled,
                isPunchOutBlocked = isPunchOutBlocked,
                punchOutButtonText = punchOutButtonText,
                isNewLocationDay = isNewLocationDay,
                shiftDisplayName = shiftDisplayName
            )
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            syncScheduler.syncState.collectLatest { syncUi ->
                val lastSyncedFormatted = syncUi.lastSyncedEpochMs?.let { epochMs ->
                    val zoneId = ZoneId.of(trustedTimeProvider.currentTimeFlow.value.timezoneId)
                    Instant.ofEpochMilli(epochMs).atZone(zoneId).format(syncTimeFormatter)
                }

                _uiState.update { current ->
                    current.copy(
                        pendingCount = syncUi.pendingCount,
                        lastSyncedText = lastSyncedFormatted,
                        isSyncing = syncUi.isSyncing
                    )
                }
            }
        }
    }

    /**
     * User initiated Punch In.
     * Checks Geofence (100m) against office coordinates before proceeding to Camera capture.
     * Also resolves past missed punches (FR-17) upon initiating punch on a new day.
     */
    fun onPunchInClicked() {
        if (!_uiState.value.isPunchInEnabled) return
        verifyGeofenceAndProceed(PunchType.IN)
    }

    /**
     * User initiated Punch In at a dynamic / on-site location.
     * Records current coordinates without requiring office geofence.
     */
    fun onPunchInNewLocationClicked() {
        if (!_uiState.value.isPunchInEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingGeofence = true) }

            val locationStatus = locationMonitor.freshFixOrRefresh(
                maxAgeMs = 90_000L,
                maxAccuracyMeters = 50f
            )

            _uiState.update { it.copy(isCheckingGeofence = false) }

            when (locationStatus) {
                is LocationStatus.Fix -> {
                    val userId = tokenStorage.getUserId() ?: ""
                    val todayDateLocal = trustedTimeProvider.getOfficeLocalDate().toString()
                    if (userId.isNotBlank()) {
                        punchRepository.resolvePastMissedPunches(userId, todayDateLocal)
                    }

                    _navigationEvents.send(
                        HomeNavigationEvent.NavigateToPunchCapture(
                            punchType = PunchType.IN,
                            latitude = locationStatus.latitude,
                            longitude = locationStatus.longitude,
                            distanceMeters = 0f,
                            locationType = "NEW_LOCATION"
                        )
                    )
                }
                is LocationStatus.MockLocationDetected -> {
                    _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.MockLocationDetected) }
                }
                is LocationStatus.PermissionDenied -> {
                    _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.PermissionRequired) }
                }
                is LocationStatus.ServicesDisabled -> {
                    _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationServicesDisabled) }
                }
                is LocationStatus.Unavailable -> {
                    _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable(locationStatus.reason)) }
                }
                is LocationStatus.Idle,
                is LocationStatus.Locating -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable("Location acquisition in progress. Please try again in a moment."))
                    }
                }
            }
        }
    }

    /**
     * User initiated Punch Out.
     * If user punched in at a NEW_LOCATION, punch-out is strictly locked to those anchor coordinates within 100m.
     * Otherwise, checks geofence against assigned office coordinates.
     */
    fun onPunchOutClicked() {
        if (!_uiState.value.isPunchOutEnabled) return
        viewModelScope.launch {
            val userId = tokenStorage.getUserId() ?: ""
            val todayDateLocal = trustedTimeProvider.getOfficeLocalDate().toString()
            val activeSummary = currentDaySummary ?: punchRepository.getDaySummary(userId, todayDateLocal)

            if (activeSummary?.locationType == "NEW_LOCATION") {
                val anchorLat = activeSummary.anchorLat
                val anchorLng = activeSummary.anchorLng
                if (anchorLat != null && anchorLng != null && (anchorLat != 0.0 || anchorLng != 0.0)) {
                    verifyNewLocationPunchOut(anchorLat, anchorLng)
                } else {
                    // Auto-heal corrupted (0.0, 0.0) anchor from previous navigation bug
                    healAndPunchOutNewLocation(activeSummary)
                }
            } else {
                verifyGeofenceAndProceed(PunchType.OUT)
            }
        }
    }

    private suspend fun healAndPunchOutNewLocation(todaySummary: DaySummary) {
        _uiState.update { it.copy(isCheckingGeofence = true) }

        val locationStatus = locationMonitor.freshFixOrRefresh(
            maxAgeMs = 90_000L,
            maxAccuracyMeters = 50f
        )

        _uiState.update { it.copy(isCheckingGeofence = false) }

        when (locationStatus) {
            is LocationStatus.Fix -> {
                // Heal day summary anchor coordinates in Room to current fix
                val healedSummary = todaySummary.copy(
                    anchorLat = locationStatus.latitude,
                    anchorLng = locationStatus.longitude
                )
                punchRepository.upsertDaySummaries(listOf(healedSummary))

                _navigationEvents.send(
                    HomeNavigationEvent.NavigateToPunchCapture(
                        punchType = PunchType.OUT,
                        latitude = locationStatus.latitude,
                        longitude = locationStatus.longitude,
                        distanceMeters = 0f,
                        locationType = "NEW_LOCATION"
                    )
                )
            }
            is LocationStatus.MockLocationDetected -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.MockLocationDetected) }
            }
            is LocationStatus.PermissionDenied -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.PermissionRequired) }
            }
            is LocationStatus.ServicesDisabled -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationServicesDisabled) }
            }
            is LocationStatus.Unavailable -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable(locationStatus.reason)) }
            }
            is LocationStatus.Idle,
            is LocationStatus.Locating -> {
                _uiState.update {
                    it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable("Location acquisition in progress. Please try again in a moment."))
                }
            }
        }
    }

    private suspend fun verifyNewLocationPunchOut(anchorLat: Double, anchorLng: Double) {
        _uiState.update { it.copy(isCheckingGeofence = true) }

        val locationStatus = locationMonitor.freshFixOrRefresh(
            maxAgeMs = 90_000L,
            maxAccuracyMeters = 50f
        )

        _uiState.update { it.copy(isCheckingGeofence = false) }

        when (locationStatus) {
            is LocationStatus.Fix -> {
                val distance = locationChecker.calculateDistanceMeters(
                    locationStatus.latitude,
                    locationStatus.longitude,
                    anchorLat,
                    anchorLng
                )
                if (distance <= 100f) {
                    _navigationEvents.send(
                        HomeNavigationEvent.NavigateToPunchCapture(
                            punchType = PunchType.OUT,
                            latitude = locationStatus.latitude,
                            longitude = locationStatus.longitude,
                            distanceMeters = distance,
                            locationType = "NEW_LOCATION"
                        )
                    )
                } else {
                    _uiState.update {
                        it.copy(
                            geofenceDialogState = GeofenceDialogState.OutsideRadius(
                                distanceMeters = distance,
                                allowedRadiusMeters = 100,
                                userLat = locationStatus.latitude,
                                userLng = locationStatus.longitude,
                                officeLat = anchorLat,
                                officeLng = anchorLng,
                                locationType = "NEW_LOCATION"
                            )
                        )
                    }
                }
            }
            is LocationStatus.MockLocationDetected -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.MockLocationDetected) }
            }
            is LocationStatus.PermissionDenied -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.PermissionRequired) }
            }
            is LocationStatus.ServicesDisabled -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationServicesDisabled) }
            }
            is LocationStatus.Unavailable -> {
                _uiState.update { it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable(locationStatus.reason)) }
            }
            is LocationStatus.Idle,
            is LocationStatus.Locating -> {
                _uiState.update {
                    it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable("Location acquisition in progress. Please try again in a moment."))
                }
            }
        }
    }

    private fun verifyGeofenceAndProceed(punchType: PunchType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingGeofence = true) }

            // Read config instantly from local database (0ms network delay)
            val config: AppConfig = remoteConfigRepository.getConfig()

            // Fetch location (instant if fresh fix within 90s, or quick 3s acquisition)
            val locationStatus = locationMonitor.freshFixOrRefresh(
                maxAgeMs = 90_000L,
                maxAccuracyMeters = 50f
            )

            _uiState.update { it.copy(isCheckingGeofence = false) }

            when (locationStatus) {
                is LocationStatus.Fix -> {
                    if (locationStatus.isInside) {
                        // If punching in, ensure any prior day's unclosed punches are resolved
                        val userId = tokenStorage.getUserId() ?: ""
                        val todayDateLocal = trustedTimeProvider.getOfficeLocalDate().toString()
                        if (userId.isNotBlank()) {
                            punchRepository.resolvePastMissedPunches(userId, todayDateLocal)
                        }

                        _navigationEvents.send(
                            HomeNavigationEvent.NavigateToPunchCapture(
                                punchType = punchType,
                                latitude = locationStatus.latitude,
                                longitude = locationStatus.longitude,
                                distanceMeters = locationStatus.distanceMeters
                            )
                        )
                    } else {
                        _uiState.update {
                            it.copy(
                                geofenceDialogState = GeofenceDialogState.OutsideRadius(
                                    distanceMeters = locationStatus.distanceMeters,
                                    allowedRadiusMeters = locationStatus.allowedRadiusMeters,
                                    userLat = locationStatus.latitude,
                                    userLng = locationStatus.longitude,
                                    officeLat = config.officeLat,
                                    officeLng = config.officeLng
                                )
                            )
                        }
                    }
                }
                is LocationStatus.MockLocationDetected -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.MockLocationDetected)
                    }
                }
                is LocationStatus.PermissionDenied -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.PermissionRequired)
                    }
                }
                is LocationStatus.ServicesDisabled -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.LocationServicesDisabled)
                    }
                }
                is LocationStatus.Unavailable -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable(locationStatus.reason))
                    }
                }
                is LocationStatus.Idle,
                is LocationStatus.Locating -> {
                    _uiState.update {
                        it.copy(geofenceDialogState = GeofenceDialogState.LocationUnavailable("Location acquisition in progress. Please try again in a moment."))
                    }
                }
            }
        }
    }

    /**
     * Immediate synchronization trigger from Home screen "Sync Now" button.
     */
    fun onSyncNowClicked() {
        viewModelScope.launch {
            syncScheduler.triggerSyncNow()
            val userId = tokenStorage.getUserId() ?: ""
            if (userId.isNotBlank()) {
                fetchAndCacheMonthlyHistory(userId)
            }
        }
    }

    fun dismissGeofenceDialog() {
        _uiState.update { it.copy(geofenceDialogState = null) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onProfileClicked() {
        viewModelScope.launch {
            _navigationEvents.send(HomeNavigationEvent.NavigateToProfile)
        }
    }
}
