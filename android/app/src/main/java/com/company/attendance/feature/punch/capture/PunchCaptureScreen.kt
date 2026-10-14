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

        // Top Controls: Guidance Banner & Screen Flash Toggle
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Screen Flash Toggle Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        onClick = { isFlashEnabled = !isFlashEnabled },
                        shape = CircleShape,
                        color = if (isFlashEnabled) Color(0xFFFEF08A) else Color.Black.copy(alpha = 0.5f),
                        contentColor = if (isFlashEnabled) Color(0xFF854D0E) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isFlashEnabled) Color(0xFFFACC15) else Color.White.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Screen Flash",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFlashEnabled) "Screen Flash ON" else "Flash OFF",
                                style = AppTypographyTokens.Caption.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val titleColor = if (isFlashEnabled) Color(0xFF0F172A) else Color.White
                val subtitleColor = if (isFlashEnabled) Color(0xFF334155) else Color.White.copy(alpha = 0.85f)

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
                        trackColor = if (isFlashEnabled) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.3f),
                    )
                }
            }
        }

        // Bottom Controls (Capture button when READY)
        if (uiState.step == CaptureStep.READY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton(
                    text = "Capture & Verify",
                    onClick = { viewModel.startCapture() }
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                SecondaryButton(
                    text = "Cancel",
                    onClick = { viewModel.onCancelClicked() }
                )
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
        val borderColor = if (isScreenFlashActive) {
            AppAccent
        } else {
            Color.White.copy(alpha = 0.85f)
        }

        drawOval(
            color = borderColor,
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
