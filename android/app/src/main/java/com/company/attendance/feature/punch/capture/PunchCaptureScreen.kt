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
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppTypographyTokens
import kotlinx.coroutines.flow.collectLatest
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.concurrent.Executors

@Composable
fun PunchCaptureScreen(
    punchType: PunchType,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    distanceMeters: Float = 0.0f,
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
            distanceMeters = distanceMeters
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

            // Oval Guide Overlay
            FaceOvalGuideOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Guidance Banner
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (punchType == PunchType.IN) "Punch In Verification" else "Punch Out Verification",
                    style = AppTypographyTokens.Title.copy(color = Color.White),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                Text(
                    text = if (uiState.step == CaptureStep.CAPTURING) {
                        "Recording face... Keep looking at the camera"
                    } else {
                        "Align your face inside the oval and tap Capture"
                    },
                    style = AppTypographyTokens.BodyMedium.copy(color = Color.White.copy(alpha = 0.85f)),
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
                        trackColor = Color.White.copy(alpha = 0.3f),
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
 * Canvas drawing an oval guide cutout with semi-transparent darkened backdrop.
 */
@Composable
private fun FaceOvalGuideOverlay(
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

        // Draw semi-transparent background with oval cutout
        drawPath(
            path = Path().apply {
                reset()
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(ovalRect)
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            },
            color = Color.Black.copy(alpha = 0.55f)
        )

        // Draw oval guide border
        drawOval(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(left, top),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
