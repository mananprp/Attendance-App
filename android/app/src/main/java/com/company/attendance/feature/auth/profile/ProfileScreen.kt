package com.company.attendance.feature.auth.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.components.DangerButton
import com.company.attendance.design.components.PrimaryButton
import com.company.attendance.design.components.SecondaryButton
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppAccentTint
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.design.theme.AppDimensions
import com.company.attendance.design.theme.AppDisabled
import com.company.attendance.design.theme.AppDisabledText
import com.company.attendance.design.theme.AppError
import com.company.attendance.design.theme.AppErrorTint
import com.company.attendance.design.theme.AppPrimaryText
import com.company.attendance.design.theme.AppSecondaryText
import com.company.attendance.design.theme.AppSuccess
import com.company.attendance.design.theme.AppSuccessTint
import com.company.attendance.design.theme.AppSurface
import com.company.attendance.design.theme.AppTypographyTokens
import com.company.attendance.design.theme.AppWarning
import com.company.attendance.design.theme.AppWarningTint
import com.company.attendance.feature.auth.profile.model.ProfileNavigationEvent
import com.company.attendance.feature.auth.profile.model.ProfileUiState
import com.company.attendance.feature.onboarding.components.FaceGuideOverlay

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateToGallery: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToAdminPanel: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                ProfileNavigationEvent.NavigateToLogin -> onSignOut()
            }
        }
    }

    ProfileScreenContent(
        uiState = uiState,
        onChangePhotoClick = viewModel::openPhotoCapture,
        onSignOutClick = viewModel::signOut,
        onNavigateToGallery = onNavigateToGallery,
        onNavigateToAdminPanel = onNavigateToAdminPanel,
        onDismissSuccess = viewModel::dismissSuccessMessage,
        modifier = modifier
    )

    if (uiState.isPhotoCaptureOpen) {
        PhotoReplacementDialog(
            uiState = uiState,
            onClose = viewModel::closePhotoCapture,
            onPhotoCaptured = viewModel::onNewPhotoCaptured,
            onRetakePhoto = viewModel::onRetakeNewPhoto,
            onConfirmReplacement = viewModel::confirmPhotoReplacement
        )
    }
}

@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onChangePhotoClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onDismissSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AttendanceTopAppBar(
            title = "Profile & Settings",
            showBottomDivider = true
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.ScreenPadding)
        ) {
            // Success alert banner
            if (!uiState.successMessage.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppDimensions.Spacing16),
                    shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                    color = AppSuccessTint,
                    border = BorderStroke(AppDimensions.BorderWidth, AppSuccess)
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimensions.Spacing12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.Spacing8))
                        Text(
                            text = uiState.successMessage,
                            style = AppTypographyTokens.CaptionMedium,
                            color = AppSuccess,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissSuccess, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = AppSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // User Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                color = AppSurface,
                border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
            ) {
                Column(
                    modifier = Modifier.padding(AppDimensions.Spacing20),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar (Actual photo or initials fallback)
                    if (uiState.profilePhotoBitmap != null) {
                        Image(
                            bitmap = uiState.profilePhotoBitmap.asImageBitmap(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(2.dp, AppAccent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = AppAccentTint,
                            modifier = Modifier
                                .size(88.dp)
                                .border(2.dp, AppAccent, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = uiState.displayName.take(2).uppercase().ifBlank { "EM" },
                                    style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                                    color = AppAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                    Text(
                        text = uiState.displayName.ifBlank { "Employee" },
                        style = AppTypographyTokens.Title,
                        color = AppPrimaryText,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))

                    Text(
                        text = "ID: ${uiState.userId}",
                        style = AppTypographyTokens.BodyMedium,
                        color = AppSecondaryText
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing4))

                    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.Spacing8)) {
                        Surface(
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = AppBackground,
                            border = BorderStroke(1.dp, AppBorder)
                        ) {
                            Text(
                                text = "Site: ${uiState.siteCode.ifBlank { "DEFAULT_OFFICE" }}",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppSecondaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (uiState.isAdmin) {
                            Surface(
                                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                                color = AppAccentTint
                            ) {
                                Text(
                                    text = "ADMIN",
                                    style = AppTypographyTokens.CaptionMedium,
                                    color = AppAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (uiState.activePhotoVersion != null) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing8))
                        Surface(
                            shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                            color = AppAccentTint
                        ) {
                            Text(
                                text = "Photo Version ${uiState.activePhotoVersion}",
                                style = AppTypographyTokens.CaptionMedium,
                                color = AppAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing20))

            // Photo Management Section
            Text(
                text = "Biometrics & Photo",
                style = AppTypographyTokens.SectionHeading,
                color = AppPrimaryText,
                modifier = Modifier.padding(bottom = AppDimensions.Spacing8)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                color = AppSurface,
                border = BorderStroke(AppDimensions.BorderWidth, AppBorder)
            ) {
                Column(modifier = Modifier.padding(AppDimensions.Spacing16)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (uiState.canChangePhoto) AppAccentTint else AppDisabled,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (uiState.canChangePhoto) Icons.Default.PhotoCamera else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (uiState.canChangePhoto) AppAccent else AppDisabledText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(AppDimensions.Spacing12))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile Photo",
                                style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppPrimaryText
                            )
                            Text(
                                text = if (uiState.photoChangeUsed) {
                                    "Photo change used (1 change limit reached)"
                                } else {
                                    "1 photo change allowed"
                                },
                                style = AppTypographyTokens.Caption,
                                color = if (uiState.photoChangeUsed) AppWarning else AppSecondaryText
                            )
                        }
                    }

                    if (uiState.canChangePhoto) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
                        SecondaryButton(
                            text = "Change Profile Photo",
                            onClick = onChangePhotoClick,
                            leadingIcon = Icons.Default.CameraAlt
                        )
                    }
                }
            }

            if (uiState.isAdmin) {
                Spacer(modifier = Modifier.height(AppDimensions.Spacing24))

                Text(
                    text = "Administration",
                    style = AppTypographyTokens.SectionHeading,
                    color = AppPrimaryText,
                    modifier = Modifier.padding(bottom = AppDimensions.Spacing8)
                )

                PrimaryButton(
                    text = "Open Admin Panel",
                    onClick = onNavigateToAdminPanel
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

            // Sign Out
            DangerButton(
                text = "Sign Out",
                onClick = onSignOutClick
            )

            Spacer(modifier = Modifier.height(AppDimensions.Spacing24))
        }
    }
}

@Composable
private fun PhotoReplacementDialog(
    uiState: ProfileUiState,
    onClose: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit,
    onRetakePhoto: () -> Unit,
    onConfirmReplacement: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (uiState.capturedNewBitmap == null || uiState.faceValidationError != null) {
                // Live camera preview
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

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_FRONT_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
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

                // Face guide
                FaceGuideOverlay(
                    instruction = "Position your face within the oval guide",
                    borderColor = if (uiState.faceValidationError != null) AppError else AppAccent
                )

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = AppDimensions.Spacing32, end = AppDimensions.Spacing16)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                // Error banner
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

                // Shutter button
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
                            color = Color.White,
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
                                .background(Color.White, CircleShape)
                                .border(4.dp, AppAccent, CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            } else {
                // Confirmation screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                        .padding(AppDimensions.ScreenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Verify Replacement Photo",
                        style = AppTypographyTokens.Title.copy(fontWeight = FontWeight.Bold),
                        color = AppPrimaryText
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing16))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Image(
                            bitmap = uiState.capturedNewBitmap.asImageBitmap(),
                            contentDescription = "New Profile Photo",
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
                        text = "Face recognized. Please confirm replacement (this is your only allowed photo change).",
                        style = AppTypographyTokens.Body,
                        color = AppSecondaryText,
                        textAlign = TextAlign.Center
                    )

                    if (!uiState.replacePhotoError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(AppDimensions.Spacing16))
                        Text(
                            text = uiState.replacePhotoError,
                            style = AppTypographyTokens.CaptionMedium,
                            color = AppError,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing32))

                    PrimaryButton(
                        text = "Confirm Replacement (Version 2)",
                        onClick = onConfirmReplacement,
                        isLoading = uiState.isReplacingPhoto,
                        enabled = !uiState.isReplacingPhoto && uiState.extractedNewEmbedding != null
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                    SecondaryButton(
                        text = "Retake Photo",
                        onClick = onRetakePhoto,
                        enabled = !uiState.isReplacingPhoto
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.Spacing12))

                    SecondaryButton(
                        text = "Cancel",
                        onClick = onClose,
                        enabled = !uiState.isReplacingPhoto
                    )
                }
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
