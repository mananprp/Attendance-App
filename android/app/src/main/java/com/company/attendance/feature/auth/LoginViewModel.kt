package com.company.attendance.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.database.entity.UserLocalEntity
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.DayStatus
import com.company.attendance.core.model.DaySummary
import com.company.attendance.core.model.UserSession
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.network.model.LoginRequestDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.feature.auth.model.LoginNavigationEvent
import com.company.attendance.feature.auth.model.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.content.Context
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.core.util.ImageStorageUtils
import com.company.attendance.face.reference.FaceReferenceManager
import com.company.attendance.feature.punch.capture.CameraCaptureUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: AttendanceApiService,
    private val okHttpClient: OkHttpClient,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao,
    private val faceVerifier: FaceVerifier,
    private val faceReferenceManager: FaceReferenceManager,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val punchRepository: PunchRepository,
    private val locationMonitor: com.company.attendance.core.location.LocationMonitor? = null,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<LoginNavigationEvent>()
    val navigationEvent: SharedFlow<LoginNavigationEvent> = _navigationEvent.asSharedFlow()

    fun onUserIdChanged(userId: String) {
        val trimmed = userId.trim()
        _uiState.update {
            it.copy(
                userId = userId,
                userIdError = if (trimmed.isEmpty() && userId.isNotEmpty()) "User ID cannot be blank" else null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null
            )
        }
    }

    fun login() {
        val currentState = _uiState.value
        val userIdTrimmed = currentState.userId.trim()
        val password = currentState.password

        var hasError = false
        var userIdErr: String? = null
        var passwordErr: String? = null

        if (userIdTrimmed.isBlank()) {
            userIdErr = "Please enter your User ID"
            hasError = true
        }

        if (password.isBlank()) {
            passwordErr = "Please enter your password"
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    userIdError = userIdErr,
                    passwordError = passwordErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }

            try {
                val response = apiService.login(LoginRequestDto(userIdTrimmed, password))
                val result = NetworkHelper.parseResponse(response)

                result.onSuccess { loginResponse ->
                    val userDto = loginResponse.user
                    // 0. Pre-login purge: purge any stale user_local rows and profile photos of other users
                    userLocalDao.deleteAllExcept(userDto.userId)
                    context.filesDir.listFiles()
                        ?.filter { it.name.startsWith("profile_") && !it.name.equals("profile_${userDto.userId}.jpg") }
                        ?.forEach { it.delete() }

                    val userSession = UserSession(
                        userId = userDto.userId,
                        displayName = userDto.displayName,
                        token = loginResponse.token,
                        role = userDto.role,
                        siteCode = userDto.siteCode,
                        photoChangeUsed = userDto.photoChangeUsed,
                        hasPhoto = userDto.hasPhoto,
                        activePhotoVersion = userDto.activePhotoVersion,
                        activePhotoHash = userDto.activePhotoHash
                    )

                    // 1. Save session in EncryptedSharedPreferences
                    tokenStorage.saveSession(userSession)

                    // Warm up location monitor asynchronously
                    locationMonitor?.warmUp()

                    // 2. Cache user profile in Room user_local (validate proof before preserving existing embedding)
                    val existingUser = userLocalDao.getUserDirect(userDto.userId)
                    val isProofValid = existingUser != null &&
                        existingUser.ownerUserId?.equals(userDto.userId, ignoreCase = true) == true &&
                        existingUser.referenceEmbedding != null &&
                        existingUser.referenceEmbedding.isNotEmpty() &&
                        (userDto.activePhotoVersion == null || existingUser.photoVersion == userDto.activePhotoVersion) &&
                        (!userDto.activePhotoHash.isNullOrBlank() && existingUser.photoHash?.equals(userDto.activePhotoHash, ignoreCase = true) == true)

                    val userEntity = UserLocalEntity(
                        userId = userDto.userId,
                        displayName = userDto.displayName,
                        photoChangeUsed = userDto.photoChangeUsed,
                        hasPhoto = userDto.hasPhoto,
                        referenceEmbedding = if (isProofValid) existingUser?.referenceEmbedding else null,
                        ownerUserId = if (isProofValid) existingUser?.ownerUserId else null,
                        photoVersion = if (isProofValid) existingUser?.photoVersion else null,
                        photoHash = if (isProofValid) existingUser?.photoHash else null,
                        updatedAt = System.currentTimeMillis()
                    )
                    userLocalDao.insertOrUpdate(userEntity)

                    // 3. Fetch and cache remote config & schedule background sync windows
                    runCatching {
                        val configResult = remoteConfigRepository.fetchAndCacheConfig()
                        val config = configResult.getOrNull() ?: remoteConfigRepository.getConfig()
                        syncScheduler?.scheduleConfiguredWindows(config.syncWindows, config.syncJitterMinutes)
                    }

                    // 4. Branch based on onboarding status
                    if (userDto.hasPhoto) {
                        // User has photo -> pull initial history & profile photo embedding from server
                        fetchAndCacheMonthlyHistory(userDto.userId)
                        fetchAndCacheProfilePhotoAndEmbedding(userDto.userId)
                        _uiState.update { it.copy(isLoading = false) }
                        _navigationEvent.emit(LoginNavigationEvent.NavigateToHome)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                        _navigationEvent.emit(LoginNavigationEvent.NavigateToOnboarding)
                    }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = throwable.message ?: "Authentication failed. Please check your credentials."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        generalError = e.message ?: "An unexpected network error occurred."
                    )
                }
            }
        }
    }

    private suspend fun fetchAndCacheMonthlyHistory(userId: String) {
        runCatching {
            val now = YearMonth.now()
            (0L..3L).forEach { offset ->
                val targetMonth = now.minusMonths(offset).format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val response = apiService.getMonthlyAttendance(targetMonth)
                val parsed = NetworkHelper.parseResponse(response)
                parsed.onSuccess { attendanceDto ->
                    val summaries = attendanceDto.days.map { dayDto ->
                        DaySummary(
                            userId = userId,
                            dateLocal = dayDto.dateLocal,
                            inTimeUtcEpochMs = dayDto.inTime?.let { parseIsoToEpochMs(it) },
                            outTimeUtcEpochMs = dayDto.outTime?.let { parseIsoToEpochMs(it) },
                            status = runCatching { DayStatus.valueOf(dayDto.status) }.getOrDefault(DayStatus.NO_RECORD),
                            timeUnverified = dayDto.timeUnverified
                        )
                    }
                    punchRepository.upsertDaySummaries(summaries)
                }
            }
        }.onFailure { e ->
            android.util.Log.e("LoginViewModel", "fetchAndCacheMonthlyHistory failed", e)
        }
    }

    private fun parseIsoToEpochMs(isoString: String): Long? {
        return runCatching {
            Instant.parse(isoString).toEpochMilli()
        }.getOrNull()
    }

    private suspend fun fetchAndCacheProfilePhotoAndEmbedding(userId: String) {
        runCatching {
            if (!faceReferenceManager.validateLocalReference(userId)) {
                android.util.Log.d("LoginViewModel", "Ensuring reference face proof for $userId")
                faceReferenceManager.ensureReference(userId)
            } else {
                android.util.Log.d("LoginViewModel", "Local reference face proof valid for $userId")
            }
        }.onFailure { e ->
            android.util.Log.e("LoginViewModel", "fetchAndCacheProfilePhotoAndEmbedding failed", e)
        }
    }
}
