package com.company.attendance.feature.punch.capture

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.database.dao.UserLocalDao
import com.company.attendance.core.interfaces.FaceVerificationResult
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import com.company.attendance.core.util.ImageStorageUtils
import com.company.attendance.face.reference.FaceReferenceManager
import com.company.attendance.face.reference.ReferenceResult
import okhttp3.OkHttpClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PunchCaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStorage: TokenStorage,
    private val userLocalDao: UserLocalDao,
    private val faceVerifier: FaceVerifier,
    private val apiService: AttendanceApiService,
    private val okHttpClient: OkHttpClient,
    private val trustedTimeProvider: TrustedTimeProvider,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val punchRepository: PunchRepository,
    private val faceReferenceManager: FaceReferenceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PunchCaptureUiState())
    val uiState: StateFlow<PunchCaptureUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<PunchCaptureNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var targetPunchType: PunchType = PunchType.IN
    private var targetLatitude: Double = 0.0
    private var targetLongitude: Double = 0.0
    private var targetDistanceMeters: Float = 0.0f
    private var targetLocationType: String = "OFFICE"

    private var currentStreamingSession: com.company.attendance.face.verifier.StreamingPunchSession? = null
    private var sampledFramesCount: Int = 0
    private var captureStartTimeMs: Long = 0L
    private var lastSampleTimeMs: Long = 0L

    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())

    companion object {
        const val CAPTURE_DURATION_MS = 2500L // 2.5 seconds capture window
        const val MIN_REQUIRED_FRAMES = 8
        const val TARGET_SAMPLE_INTERVAL_MS = 120L // ~8-10 FPS
    }

    /**
     * Initializes the capture session with punch type and verified geofence coordinates.
     */
    fun initSession(
        punchType: PunchType,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        distanceMeters: Float = 0.0f,
        locationType: String = "OFFICE"
    ) {
        targetPunchType = punchType
        targetLatitude = latitude
        targetLongitude = longitude
        targetDistanceMeters = distanceMeters
        targetLocationType = locationType

        _uiState.update {
            it.copy(
                punchType = punchType,
                step = CaptureStep.INITIALIZING,
                failureMessage = null,
                failureReason = null
            )
        }

        prepareCaptureSession()
    }

    private fun prepareCaptureSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(step = CaptureStep.INITIALIZING) }

            // 1. Verify ONNX models are present on device and pre-warm sessions
            if (!faceVerifier.areModelsReady()) {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Models Missing",
                        failureMessage = "Facial recognition models are not downloaded. Please download models first.",
                        failureReason = "ONNX model files missing on device storage."
                    )
                }
                return@launch
            }

            // Warm up ONNX runtime sessions in parallel with embedding retrieval
            faceVerifier.prepare()

            // 2. Validate reference face proof via FaceReferenceManager (Fail-closed)
            val userId = tokenStorage.getUserId() ?: ""
            if (userId.isBlank()) {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Authentication Required",
                        failureMessage = "Please log in again.",
                        failureReason = "User ID missing."
                    )
                }
                return@launch
            }

            val isValid = faceReferenceManager.validateLocalReference(userId)
            if (!isValid) {
                val ensureResult = faceReferenceManager.ensureReference(userId)
                if (ensureResult !is ReferenceResult.Success) {
                    _uiState.update {
                        it.copy(
                            step = CaptureStep.FAILURE,
                            failureTitle = "Face Profile Required",
                            failureMessage = "Your face profile needs to refresh. Connect to the internet once and try again.",
                            failureReason = "Face profile proof could not be verified."
                        )
                    }
                    return@launch
                }
            }

            sampledFramesCount = 0
            captureStartTimeMs = 0L
            lastSampleTimeMs = 0L

            _uiState.update {
                it.copy(
                    step = CaptureStep.READY,
                    captureProgress = 0f,
                    sampledFramesCount = 0,
                    failureMessage = null,
                    failureReason = null
                )
            }
        }
    }

    /**
     * Checks if a new camera frame should be sampled and converted to bitmap.
     */
    fun shouldSampleNow(): Boolean {
        if (_uiState.value.step != CaptureStep.CAPTURING) return false
        val now = System.currentTimeMillis()
        return (now - lastSampleTimeMs >= TARGET_SAMPLE_INTERVAL_MS)
    }

    /**
     * User clicks the Capture button to start 2.5s face verification recording.
     */
    fun startCapture() {
        if (_uiState.value.step != CaptureStep.READY) return

        currentStreamingSession?.cancel()
        currentStreamingSession = faceVerifier.startStreamingSession(viewModelScope)

        sampledFramesCount = 0
        captureStartTimeMs = System.currentTimeMillis()
        lastSampleTimeMs = 0L

        _uiState.update {
            it.copy(
                step = CaptureStep.CAPTURING,
                captureProgress = 0f,
                sampledFramesCount = 0,
                failureMessage = null,
                failureReason = null
            )
        }
    }

    /**
     * Ingests a camera preview frame from CameraX analyzer directly into the streaming pipeline.
     */
    fun onFrameSampled(bitmap: Bitmap) {
        if (_uiState.value.step != CaptureStep.CAPTURING) return

        val now = System.currentTimeMillis()
        if (now - lastSampleTimeMs < TARGET_SAMPLE_INTERVAL_MS) {
            return
        }
        lastSampleTimeMs = now
        sampledFramesCount++

        currentStreamingSession?.submit(bitmap)

        val elapsedMs = now - captureStartTimeMs
        val progress = (elapsedMs.toFloat() / CAPTURE_DURATION_MS).coerceIn(0f, 1f)

        _uiState.update {
            it.copy(
                captureProgress = progress,
                sampledFramesCount = sampledFramesCount
            )
        }

        // If capture window elapsed and we have sufficient frames, trigger verification finish
        if (elapsedMs >= CAPTURE_DURATION_MS && sampledFramesCount >= MIN_REQUIRED_FRAMES) {
            processCapturedFrames()
        }
    }

    /**
     * Completes face verification pipeline on the streaming session.
     */
    fun processCapturedFrames(overrideReferenceEmbeddingBytes: ByteArray? = null) {
        if (_uiState.value.step == CaptureStep.PROCESSING || _uiState.value.step == CaptureStep.SUCCESS) return

        _uiState.update { it.copy(step = CaptureStep.PROCESSING, captureProgress = 1f) }

        viewModelScope.launch {
            val currentUserId = tokenStorage.getUserId() ?: ""
            if (currentUserId.isBlank()) {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Authentication Required",
                        failureMessage = "Please log in again.",
                        failureReason = "User ID missing."
                    )
                }
                return@launch
            }

            // Strictly validate reference against proof
            if (overrideReferenceEmbeddingBytes == null) {
                val isValid = faceReferenceManager.validateLocalReference(currentUserId)
                if (!isValid) {
                    val ensureResult = faceReferenceManager.ensureReference(currentUserId)
                    if (ensureResult !is ReferenceResult.Success) {
                        _uiState.update {
                            it.copy(
                                step = CaptureStep.FAILURE,
                                failureTitle = "Face Profile Required",
                                failureMessage = "Your face profile needs to refresh. Connect to the internet once and try again.",
                                failureReason = "Face profile proof could not be verified."
                            )
                        }
                        return@launch
                    }
                }
            }

            val userEntity = userLocalDao.getUserDirect(currentUserId)

            // Direct Employee ID cross-check against profile owner
            if (overrideReferenceEmbeddingBytes == null && userEntity != null && !userEntity.ownerUserId.equals(currentUserId, ignoreCase = true)) {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Account Mismatch",
                        failureMessage = "Stored face profile does not belong to logged-in employee ($currentUserId). Connect to the internet once to refresh.",
                        failureReason = "Employee ID mismatch: expected $currentUserId, found ${userEntity.ownerUserId ?: "none"}."
                    )
                }
                return@launch
            }

            val finalEmbeddingBytes = overrideReferenceEmbeddingBytes ?: userEntity?.referenceEmbedding
            if (finalEmbeddingBytes == null || finalEmbeddingBytes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Profile Photo Missing",
                        failureMessage = "No reference facial embedding found for your account. Please complete onboarding.",
                        failureReason = "Reference embedding not found in local storage."
                    )
                }
                return@launch
            }

            val referenceEmbedding = CameraCaptureUtils.byteArrayToFloatArray(finalEmbeddingBytes)
            val config = remoteConfigRepository.getConfig()
            val threshold = config.matchThreshold

            val streamingSession = currentStreamingSession
            val result = if (streamingSession != null) {
                streamingSession.finish(referenceEmbedding, threshold)
            } else {
                faceVerifier.verifyPunchFrames(emptyList(), referenceEmbedding, threshold)
            }

            handleVerificationResult(result, threshold)
        }
    }

    private suspend fun handleVerificationResult(
        result: FaceVerificationResult,
        threshold: Float
    ) {
        when (result) {
            is FaceVerificationResult.Match -> {
                // 1. Save aligned face image to app-private storage
                val imagePath = try {
                    CameraCaptureUtils.savePunchImageToInternalStorage(context, result.alignedFaceBitmap)
                } catch (e: Exception) {
                    null
                }

                // 2. Obtain authoritative trusted time
                val trustedTime = trustedTimeProvider.getTrustedTime()
                val zoneId = ZoneId.of(trustedTime.timezoneId)
                val zonedDateTime = Instant.ofEpochMilli(trustedTime.epochMillis).atZone(zoneId)

                val formattedTime = zonedDateTime.format(timeFormatter)
                val formattedDate = zonedDateTime.format(dateFormatter)

                // 3. Record punch in Room database
                val userId = tokenStorage.getUserId() ?: ""
                val userEntity = if (userId.isNotBlank()) userLocalDao.getUserDirect(userId) else null
                _uiState.update { it.copy(isSavingPunch = true) }

                val recordResult = punchRepository.recordPunch(
                    userId = userId,
                    punchType = targetPunchType,
                    time = trustedTime,
                    latitude = targetLatitude,
                    longitude = targetLongitude,
                    distanceMeters = targetDistanceMeters,
                    matchScore = result.score,
                    capturedImagePath = imagePath,
                    locationType = targetLocationType,
                    refUserId = userEntity?.ownerUserId ?: userId,
                    refPhotoHash = userEntity?.photoHash,
                    refPhotoVersion = userEntity?.photoVersion
                )

                _uiState.update { it.copy(isSavingPunch = false) }

                if (recordResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            step = CaptureStep.SUCCESS,
                            matchScore = result.score,
                            requiredThreshold = result.threshold,
                            recordedTimeFormatted = formattedTime,
                            recordedDateFormatted = formattedDate,
                            isTimeUnverified = trustedTime.isUnverified,
                            capturedImagePreview = result.alignedFaceBitmap
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            step = CaptureStep.FAILURE,
                            failureTitle = "Storage Error",
                            failureMessage = "Failed to save punch record to database: ${recordResult.exceptionOrNull()?.message}",
                            failureReason = "Database insert failure."
                        )
                    }
                }
            }

            is FaceVerificationResult.Mismatch -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Face Mismatch",
                        failureMessage = "The captured face does not match your registered profile photo.",
                        failureReason = "Match score: ${(result.score * 100).toInt()}% (Required: ${(result.threshold * 100).toInt()}%)",
                        matchScore = result.score,
                        requiredThreshold = result.threshold
                    )
                }
            }

            is FaceVerificationResult.NoFaceDetected -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "No Face Detected",
                        failureMessage = "No face was detected. Please ensure your face is clearly visible inside the oval frame.",
                        failureReason = "No bounding box detected above confidence threshold."
                    )
                }
            }

            is FaceVerificationResult.MultipleFacesDetected -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Multiple Faces Detected",
                        failureMessage = "Detected ${result.faceCount} faces in the camera frame. Only the employee should be visible.",
                        failureReason = "Single person constraint violated (${result.faceCount} faces detected)."
                    )
                }
            }

            is FaceVerificationResult.LivenessFailed -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Liveness Check Failed",
                        failureMessage = "Anti-spoofing / liveness check failed (${result.reason}). Please look straight at the camera, ensure good lighting, and blink naturally.",
                        failureReason = result.reason
                    )
                }
            }

            is FaceVerificationResult.ModelMissingOrCorrupt -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Model Missing",
                        failureMessage = "ONNX models missing: ${result.missingModels.joinToString()}",
                        failureReason = "Model files missing or failed checksum."
                    )
                }
            }

            is FaceVerificationResult.Error -> {
                _uiState.update {
                    it.copy(
                        step = CaptureStep.FAILURE,
                        failureTitle = "Processing Error",
                        failureMessage = "An unexpected error occurred during face verification: ${result.message}",
                        failureReason = result.throwable.localizedMessage
                    )
                }
            }
        }
    }

    /**
     * Restarts the capture session without consuming punch attempts.
     */
    fun onTryAgain() {
        prepareCaptureSession()
    }

    fun onDoneClicked() {
        viewModelScope.launch {
            _navigationEvents.send(PunchCaptureNavigationEvent.FinishSuccess)
        }
    }

    fun onCancelClicked() {
        currentStreamingSession?.cancel()
        currentStreamingSession = null
        viewModelScope.launch {
            _navigationEvents.send(PunchCaptureNavigationEvent.FinishCancel)
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentStreamingSession?.cancel()
        currentStreamingSession = null
        faceVerifier.releaseResources()
    }
}
