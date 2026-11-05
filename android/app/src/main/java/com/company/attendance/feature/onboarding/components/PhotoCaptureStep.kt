package com.company.attendance.feature.onboarding.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.feature.onboarding.model.OnboardingUiState

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun PhotoCaptureStep(
    uiState: OnboardingUiState,
    onPhotoCaptured: (Bitmap) -> Unit,
    onRetakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Screen brightness override: sets 100% full brightness during profile photo capture
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val originalBrightness = activity?.window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        activity?.window?.let { window ->
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL // 1.0f
            window.attributes = params
        }

        onDispose {
            activity?.window?.let { window ->
                val params = window.attributes
                params.screenBrightness = originalBrightness
                window.attributes = params
            }
        }
    }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val capturedBitmap = uiState.capturedBitmap
    val isFaceVerified = uiState.extractedEmbedding != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedBitmap == null || uiState.faceValidationError != null) {
            // Live Camera Preview View
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            // Fallback to back camera if front camera is not available
                            try {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (_: Exception) {}
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Oval face guide overlay
            FaceGuideOverlay(
                instruction = "Position your face within the oval guide",
                borderColor = if (uiState.faceValidationError != null) AppError else AppAccent
            )

            // Error banner if single face check failed
            if (!uiState.faceValidationError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = AppDimensions.Spacing48, start = AppDimensions.Spacing16, end = AppDimensions.Spacing16),
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppErrorTint
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimensions.Spacing12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = AppError,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                        Text(
                            text = uiState.faceValidationError,
                            style = AppTypographyTokens.CaptionMedium,
                            color = AppError
                        )
                    }
                }
            }

            // Capture button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimensions.Spacing32),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isAnalyzingFace || isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = AppAccent,
                        strokeWidth = 3.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            val capture = imageCapture ?: return@IconButton
                            isCapturing = true

                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = imageProxyToBitmap(image)
                                        image.close()
                                        isCapturing = false
                                        if (bitmap != null) {
                                            onPhotoCaptured(bitmap)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isCapturing = false
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(AppAccent, CircleShape)
                            .border(4.dp, AppBorder, CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AppAccent, CircleShape)
                        )
                    }
                }
            }
        } else {
            // Confirmation Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(AppDimensions.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Profile Photo Captured",
                    style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                    color = AppPrimaryText
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                // Captured photo preview
                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        bitmap = capturedBitmap.asImageBitmap(),
                        contentDescription = "Captured profile photo",
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .border(3.dp, AppAccent, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Surface(
                        shape = CircleShape,
                        color = AppSuccessTint,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = AppSuccess,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                Text(
                    text = "Face recognized successfully. This photo will be used to verify your attendance punches.",
                    style = AppTypographyTokens.Body,
                    color = AppSecondaryText,
                    textAlign = TextAlign.Center
                )

                if (!uiState.uploadError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
                    Text(
                        text = uiState.uploadError,
                        style = AppTypographyTokens.CaptionMedium,
                        color = AppError,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

                PrimaryButton(
                    text = "Confirm & Upload Photo",
                    onClick = onUploadPhoto,
                    isLoading = uiState.isUploading,
                    enabled = !uiState.isUploading && isFaceVerified
                )

                Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                SecondaryButton(
                    text = "Retake Photo",
                    onClick = onRetakePhoto,
                    enabled = !uiState.isUploading
                )
            }
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val bitmap = imageProxy.toBitmap()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}
