package com.company.attendance.feature.punch.capture

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
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
import androidx.hilt.navigation.compose.hiltViewModel
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

    // Camera lens selection state: Guards default to BACK camera; video default to FRONT camera
    var selectedLensFacing by remember(uiState.captureMode) {
        mutableIntStateOf(
            if (uiState.captureMode == CaptureMode.GUARD_PHOTO) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
        )
    }

    // Active Camera instance holder for rear torch control
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    // Rear Torch hardware control
    LaunchedEffect(isFlashEnabled, boundCamera, selectedLensFacing) {
        if (selectedLensFacing == CameraSelector.LENS_FACING_BACK) {
            boundCamera?.cameraControl?.enableTorch(isFlashEnabled)
        }
    }

    // Front Screen Brightness Flash: 100% full screen brightness when front lens is active
    DisposableEffect(isFlashEnabled, selectedLensFacing) {
        val activity = context.findActivity()
        val originalBrightness = activity?.window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        if (isFlashEnabled && selectedLensFacing == CameraSelector.LENS_FACING_FRONT) {
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
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

    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    // 2. Continuous CameraX Lifecycle Binding (keyed only on lens facing and capture mode)
    LaunchedEffect(selectedLensFacing, uiState.captureMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val targetLens = if (selectedLensFacing == CameraSelector.LENS_FACING_FRONT) {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            } else {
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(targetLens)
                .build()

            try {
                cameraProvider.unbindAll()

                if (uiState.captureMode == CaptureMode.GUARD_PHOTO) {
                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCaptureUseCase = imageCapture

                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    boundCamera = cam
                    if (targetLens == CameraSelector.LENS_FACING_BACK) {
                        cam.cameraControl.enableTorch(isFlashEnabled)
                    }
                } else {
                    // Video / continuous frame analysis mode
                    var lastLuminanceCheckMs = 0L
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            val now = System.currentTimeMillis()
                            // Throttled auto low-light detection for front camera (at most once every 1000ms)
                            if (!userManuallyToggled && !isAutoTriggered && targetLens == CameraSelector.LENS_FACING_FRONT && (now - lastLuminanceCheckMs >= 1000L)) {
                                lastLuminanceCheckMs = now
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
                                        ContextCompat.getMainExecutor(context).execute {
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

                    val cam = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    boundCamera = cam
                }
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. CameraX Preview Layer (Hardware-accelerated SurfaceView via PERFORMANCE mode)
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Guide Overlays (visible during capture and ready stages)
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            if (uiState.captureMode == CaptureMode.GUARD_PHOTO) {
                GuardBodyGuideOverlay(
                    isScreenFlashActive = isFlashEnabled && selectedLensFacing == CameraSelector.LENS_FACING_FRONT,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                FaceOvalGuideOverlay(
                    isScreenFlashActive = isFlashEnabled && selectedLensFacing == CameraSelector.LENS_FACING_FRONT,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Header Bar: Cancel (top-left) and Title
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.CAPTURING || uiState.step == CaptureStep.INITIALIZING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            ) {
                // Top-Left: Close / Back
                Surface(
                    onClick = { viewModel.onCancelClicked() },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.CenterStart)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Top-Center: Title & Subtitle
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isFrontFlash = isFlashEnabled && selectedLensFacing == CameraSelector.LENS_FACING_FRONT
                    val titleColor = if (isFrontFlash) Color(0xFF0F172A) else Color.White
                    val subtitleColor = if (isFrontFlash) Color(0xFF334155) else Color.White.copy(alpha = 0.85f)

                    Text(
                        text = if (punchType == PunchType.IN) {
                            if (uiState.captureMode == CaptureMode.GUARD_PHOTO) "Guard Punch In" else "Punch In Verification"
                        } else {
                            if (uiState.captureMode == CaptureMode.GUARD_PHOTO) "Guard Punch Out" else "Punch Out Verification"
                        },
                        style = AppTypographyTokens.Title.copy(color = titleColor),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))
                    Text(
                        text = when {
                            uiState.step == CaptureStep.CAPTURING -> "Recording face... Keep looking at camera"
                            uiState.captureMode == CaptureMode.GUARD_PHOTO -> "Align body and face in frame"
                            else -> "Align face inside oval & tap Capture"
                        },
                        style = AppTypographyTokens.Caption.copy(color = subtitleColor),
                        textAlign = TextAlign.Center
                    )

                    if (uiState.step == CaptureStep.CAPTURING) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                        LinearProgressIndicator(
                            progress = { uiState.captureProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(4.dp),
                            color = AppAccent,
                            trackColor = if (isFrontFlash) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        // Camera Bottom Control Bar: Flash (Left), Shutter (Center), Flip Camera (Right)
        if (uiState.step == CaptureStep.READY || uiState.step == CaptureStep.INITIALIZING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 32.dp, vertical = 32.dp)
            ) {
                // Symmetrical 3-zone layout: Flash (Left), Shutter (Center), Flip Camera (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Flash / Torch Button
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            onClick = {
                                userManuallyToggled = true
                                isFlashEnabled = !isFlashEnabled
                            },
                            shape = CircleShape,
                            color = if (isFlashEnabled) Color(0xFFFEF08A) else Color.White.copy(alpha = 0.2f),
                            contentColor = if (isFlashEnabled) Color(0xFF854D0E) else Color.White,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFlashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = "Flash",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Center: Shutter Button
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                .padding(6.dp)
                                .clickable(enabled = uiState.step == CaptureStep.READY) {
                                    if (uiState.captureMode == CaptureMode.GUARD_PHOTO) {
                                        val imageCapture = imageCaptureUseCase
                                        if (imageCapture != null) {
                                            imageCapture.takePicture(
                                                ContextCompat.getMainExecutor(context),
                                                object : ImageCapture.OnImageCapturedCallback() {
                                                    override fun onCaptureSuccess(image: ImageProxy) {
                                                        try {
                                                            val bmp = CameraCaptureUtils.imageProxyToRotatedBitmap(image)
                                                            viewModel.processGuardPhoto(bmp)
                                                        } finally {
                                                            image.close()
                                                        }
                                                    }

                                                    override fun onError(exception: ImageCaptureException) {
                                                    }
                                                }
                                            )
                                        }
                                    } else {
                                        viewModel.startCapture()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }

                    // Right: Switch Camera (Flip) Button
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            onClick = {
                                selectedLensFacing = if (selectedLensFacing == CameraSelector.LENS_FACING_FRONT) {
                                    CameraSelector.LENS_FACING_BACK
                                } else {
                                    CameraSelector.LENS_FACING_FRONT
                                }
                            },
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
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
                        text = if (uiState.captureMode == CaptureMode.GUARD_PHOTO) "Verifying Guard Photo..." else "Verifying Face & Liveness...",
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
 * Canvas drawing an oval guide cutout for facial video mode.
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

        val borderColor = if (isScreenFlashActive) {
            AppAccent
        } else {
            Color.White.copy(alpha = 0.8f)
        }

        drawOval(
            color = borderColor,
            topLeft = ovalRect.topLeft,
            size = ovalRect.size,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/**
 * Canvas drawing a full-body guideline overlay for guards.
 */
@Composable
private fun GuardBodyGuideOverlay(
    isScreenFlashActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val guideWidth = size.width * 0.82f
        val guideHeight = size.height * 0.70f
        val left = (size.width - guideWidth) / 2f
        val top = size.height * 0.14f

        val bodyRect = Rect(
            offset = Offset(left, top),
            size = Size(guideWidth, guideHeight)
        )

        val backdropColor = if (isScreenFlashActive) {
            Color.White.copy(alpha = 0.94f)
        } else {
            Color.Black.copy(alpha = 0.50f)
        }

        // Rounded body frame cutout
        drawPath(
            path = Path().apply {
                reset()
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(
                    RoundRect(
                        bodyRect,
                        CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                )
                fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
            },
            color = backdropColor
        )

        // Outer guide border
        drawRoundRect(
            color = if (isScreenFlashActive) AppAccent else Color.White.copy(alpha = 0.85f),
            topLeft = bodyRect.topLeft,
            size = bodyRect.size,
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
            style = Stroke(width = 2.5.dp.toPx())
        )

        // Subtle head circle guide near the top of the body frame
        val headRadius = guideWidth * 0.22f
        val headCenter = Offset(size.width / 2f, top + headRadius + 16.dp.toPx())
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = headRadius,
            center = headCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
