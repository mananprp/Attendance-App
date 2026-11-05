package com.company.attendance.feature.punch.capture

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.company.attendance.core.model.PunchType
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.components.TimeUnverifiedBadge
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppTypographyTokens
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.Executors

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun PunchCaptureScreen(
    punchType: PunchType,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    distanceMeters: Float = 0.0f,
    locationType: String = "OFFICE",
    onCaptureComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PunchCaptureViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initSession(
            punchType = punchType,
            latitude = latitude,
            longitude = longitude,
            distanceMeters = distanceMeters,
            locationType = locationType
        )
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collectLatest { event ->
            when (event) {
                is PunchCaptureNavigationEvent.FinishSuccess -> onCaptureComplete()
                is PunchCaptureNavigationEvent.FinishCancel -> onCaptureComplete()
            }
        }
    }

    val context = LocalContext.current
    var isFlashEnabled by remember { mutableStateOf(false) }
    var isAutoTriggered by remember { mutableStateOf(false) }
    var userManuallyToggled by remember { mutableStateOf(false) }

    // Screen brightness override: sets 100% full brightness when flash is enabled to light up face
    DisposableEffect(isFlashEnabled) {
        val activity = context.findActivity()
        val originalBrightness = activity?.window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        if (isFlashEnabled) {
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL // 1.0f
                window.attributes = params
            }
        } else {
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
            }
        }

        onDispose {
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = originalBrightness
                window.attributes = params
            }
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. CameraX Preview Layer
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            try {
                                // Auto low-light detection: sample Y-plane luminance if user hasn't manually toggled
                                if (!userManuallyToggled && !isAutoTriggered) {
                                    val yPlane = imageProxy.planes.getOrNull(0)
                                    if (yPlane != null) {
                                        val buffer = yPlane.buffer
                                        val rowStride = yPlane.rowStride
                                        val pixelStride = yPlane.pixelStride
                                        val w = imageProxy.width
                                        val h = imageProxy.height
                                        var sum = 0L
                                        var count = 0
                                        val step = 16
                                        for (y in 0 until h step step) {
                                            val rowOffset = y * rowStride
                                            for (x in 0 until w step step) {
                                                val idx = rowOffset + x * pixelStride
                                                if (idx < buffer.limit()) {
                                                    sum += (buffer.get(idx).toInt() and 0xFF)
                                                    count++
                                                }
                                            }
                                        }
                                        if (count > 0 && (sum.toDouble() / count) < 45.0) {
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                if (!userManuallyToggled) {
                                                    isFlashEnabled = true
                                                    isAutoTriggered = true
                                                }
                                            }
                                        }
                                    }
                                }

                                if (viewModel.shouldSampleNow()) {
                                    val bitmap = CameraCaptureUtils.imageProxyToRotatedBitmap(imageProxy)
                                    viewModel.onFrameSampled(bitmap)
                                }
                            } catch (_: Exception) {
                            } finally {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Oval Guide Overlay with Screen Flash Softbox
            FaceOvalGuideOverlay(
                isScreenFlashActive = isFlashEnabled,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Controls: Guidance Banner
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleColor = if (isFlashEnabled) AppBackground else AppPrimaryText
                val subtitleColor = if (isFlashEnabled) AppSecondaryText else AppSecondaryText

                Text(
                    text = if (punchType == PunchType.IN) "Punch In Verification" else "Punch Out Verification",
                    style = AppTypographyTokens.Title.copy(color = titleColor),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                Text(
                    text = if (uiState.step == CaptureStep.CAPTURING) {
                        "Recording face... Keep looking at the camera"
                    } else {
                        "Align your face inside the oval and tap Capture"
                    },
                    style = AppTypographyTokens.BodyMedium.copy(color = subtitleColor),
                    textAlign = TextAlign.Center
                )

                if (uiState.step == CaptureStep.CAPTURING) {
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                    // Capture Progress Bar
                    LinearProgressIndicator(
                        progress = { uiState.captureProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(6.dp),
                        color = AppAccent,
                        trackColor = AppBorder,
                    )
                }
            }
        }

        // Bottom Controls: Single-line Native Camera Deck (Flash | Shutter | Cancel)
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.INITIALIZING || uiState.step == CaptureStep.CAPTURING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 36.dp, end = 36.dp, bottom = 44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Left: Screen Flash Toggle (52dp circular glass button)
                Surface(
                    onClick = {
                        userManuallyToggled = true
                        isFlashEnabled = !isFlashEnabled
                    },
                    shape = CircleShape,
                    color = if (isFlashEnabled) AppWarningTint else Color.Black.copy(alpha = 0.55f),
                    contentColor = if (isFlashEnabled) AppWarning else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isFlashEnabled) AppWarning else Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = if (isFlashEnabled) "Screen Flash ON" else "Screen Flash OFF",
                            modifier = Modifier.size(24.dp),
                            tint = if (isFlashEnabled) AppWarning else Color.White
                        )
                    }
                }

                // 2. Center: Camera Shutter Trigger (78dp dual-ring circular button)
                val isReady = uiState.step == CaptureStep.READY
                val isCapturing = uiState.step == CaptureStep.CAPTURING
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .border(3.5.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isReady || isCapturing) AppAccent else AppAccent.copy(alpha = 0.5f))
                        .clickable(enabled = isReady) {
                            viewModel.startCapture()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = Color.Black,
                            strokeWidth = 3.dp
                        )
                    }
                }

                // 3. Right: Cancel / Dismiss (52dp circular glass button)
                Surface(
                    onClick = { viewModel.onCancelClicked() },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Processing State Overlay
        AnimatedVisibility(
            visible = uiState.step == CaptureStep.PROCESSING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(AppDimensions.ScreenPadding)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        color = AppAccent,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
                    Text(
                        text = "Verifying Face & Liveness...",
                        style = AppTypographyTokens.Title.copy(color = Color.White),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                    Text(
                        text = "Running on-device neural face pipeline",
                        style = AppTypographyTokens.BodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                    )
                }
            }
        }

        // Success State Screen
        AnimatedVisibility(
            visible = uiState.step == CaptureStep.SUCCESS,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(AppDimensions.ScreenPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Success",
                        tint = AppSuccess,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                    Text(
                        text = if (punchType == PunchType.IN) "Punch In Recorded" else "Punch Out Recorded",
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                    Text(
                        text = uiState.recordedTimeFormatted ?: "--:--:--",
                        style = AppTypographyTokens.ClockTime,
                        color = AppPrimaryText
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))

                    Text(
                        text = uiState.recordedDateFormatted ?: "",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText
                    )

                    if (uiState.matchScore != null) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))
                        Text(
                            text = "Match Confidence: ${(uiState.matchScore!! * 100).toInt()}%",
                            style = AppTypographyTokens.CaptionMedium.copy(color = AppSuccess)
                        )
                    }

                    if (uiState.isTimeUnverified) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing12))
                        TimeUnverifiedBadge(text = "Time unverified (offline clock)")
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

                    PrimaryButton(
                        text = "Back to Home",
                        onClick = { viewModel.onDoneClicked() }
                    )
                }
            }
        }

        // Failure State Screen
        AnimatedVisibility(
            visible = uiState.step == CaptureStep.FAILURE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(AppDimensions.ScreenPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = "Failed",
                        tint = AppError,
                        modifier = Modifier.size(72.dp)
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                    Text(
                        text = uiState.failureTitle,
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing8))

                    Text(
                        text = uiState.failureMessage ?: "Face verification could not be completed.",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText,
                        textAlign = TextAlign.Center
                    )

                    if (uiState.failureReason != null) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                        Text(
                            text = uiState.failureReason!!,
                            style = AppTypographyTokens.Caption.copy(color = AppError),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

                    PrimaryButton(
                        text = "Try Again",
                        onClick = { viewModel.onTryAgain() },
                        leadingIcon = Icons.Outlined.Refresh
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                    SecondaryButton(
                        text = "Cancel",
                        onClick = { viewModel.onCancelClicked() }
                    )
                }
            }
        }
    }
}

/**
 * Canvas drawing an oval guide cutout with either white fill light (flash active) or darkened backdrop.
 */
@Composable
private fun FaceOvalGuideOverlay(
    isScreenFlashActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val ovalWidth = size.width * 0.72f
        val ovalHeight = ovalWidth * 1.35f
        val left = (size.width - ovalWidth) / 2f
        val top = size.height * 0.22f

        val ovalRect = Rect(
            offset = Offset(left, top),
            size = Size(ovalWidth, ovalHeight)
        )

        // Draw background with oval cutout
        // When flash is active: bright white backdrop casts strong diffuse light onto face
        val backdropColor = if (isScreenFlashActive) {
            Color.White.copy(alpha = 0.94f)
        } else {
            Color.Black.copy(alpha = 0.55f)
        }

        drawPath(
            path = Path().apply {
                reset()
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(ovalRect)
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            },
            color = backdropColor
        )

        // Draw oval guide border
        val borderColor = AppAccent

        drawOval(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
