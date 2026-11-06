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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.outlined.ChevronRight
import com.company.attendance.design.theme.AppIconCircle
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
import androidx.compose.ui.unit.sp
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

            // 1. Corporate ID Card Formation
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Accent Stripe (Micro indicator like an ID card smartchip band)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(AppAccent.copy(alpha = 0.70f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Badge Photo (Squircle 76x76dp)
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.clickable(enabled = uiState.canChangePhoto) { onChangePhotoClick() }
                        ) {
                            if (uiState.profilePhotoBitmap != null) {
                                Image(
                                    bitmap = uiState.profilePhotoBitmap.asImageBitmap(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(1.5.dp, AppAccent, RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AppAccentTint,
                                    modifier = Modifier
                                        .size(76.dp)
                                        .border(1.5.dp, AppAccent, RoundedCornerShape(14.dp))
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

                            if (uiState.canChangePhoto) {
                                Surface(
                                    shape = CircleShape,
                                    color = AppAccent,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.5.dp, AppSurface, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Take Photo",
                                            tint = AppBackground,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: ID Card Details (Name, ID, plain text Role - no location, no pills)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "EMPLOYEE IDENTITY CARD",
                                style = AppTypographyTokens.Caption.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = AppAccent
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = uiState.displayName.ifBlank { "Employee" },
                                style = AppTypographyTokens.Title.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppPrimaryText
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "ID: " + uiState.userId,
                                style = AppTypographyTokens.BodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = AppSecondaryText
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val roleText = if (uiState.isAdmin) "Role: Administrator" else "Role: Employee"
                            Text(
                                text = roleText,
                                style = AppTypographyTokens.BodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = if (uiState.isAdmin) AppAccent else AppSecondaryText
                            )
                        }
                    }
                }
            }

            // 2. Settings Group: Account & Biometrics
            Text(
                text = "Account & Biometrics",
                style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppSecondaryText,
                modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                Column {
                    ProfileOptionRow(
                        icon = Icons.Default.PhotoCamera,
                        title = if (uiState.profilePhotoBitmap != null) "Update Profile Photo" else "Enroll Face / Take Photo",
                        subtitle = if (uiState.photoChangeUsed) "Photo change limit reached (1/1)" else "Capture and verify photo for presence matching",
                        enabled = uiState.canChangePhoto,
                        actionText = if (uiState.canChangePhoto) (if (uiState.profilePhotoBitmap != null) "Update" else "Capture") else "Locked",
                        onClick = { if (uiState.canChangePhoto) onChangePhotoClick() }
                    )

                    if (uiState.isAdmin) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppBorder)
                        )
                        ProfileOptionRow(
                            icon = Icons.Default.Business,
                            title = "Admin Console",
                            subtitle = "Manage sites, employees & credentials",
                            onClick = onNavigateToAdminPanel
                        )
                    }
                }
            }

            // 3. Settings Group: Session
            Text(
                text = "Session",
                style = AppTypographyTokens.CaptionMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppSecondaryText,
                modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.CardCornerRadius),
                color = AppSurface,
                border = BorderStroke(1.dp, AppBorder)
            ) {
                ProfileOptionRow(
                    icon = Icons.Default.ExitToApp,
                    title = "Sign Out",
                    subtitle = "Log out from this device",
                    iconTint = AppError,
                    titleColor = AppError,
                    showChevron = false,
                    onClick = onSignOutClick
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.Spacing32))
        }
    }
}

@Composable
private fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    actionText: String? = null,
    iconTint: Color = AppPrimaryText,
    titleColor: Color = AppPrimaryText,
    showChevron: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (iconTint == AppError) AppErrorTint else AppIconCircle,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) iconTint else AppDisabledText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypographyTokens.BodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) titleColor else AppDisabledText
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = AppTypographyTokens.Caption,
                    color = AppSecondaryText
                )
            }
        }

        if (actionText != null) {
            Surface(
                shape = RoundedCornerShape(AppDimensions.ChipCornerRadius),
                color = if (enabled) AppAccentTint else AppSurface,
                border = BorderStroke(1.dp, if (enabled) AppAccent else AppBorder)
            ) {
                Text(
                    text = actionText,
                    style = AppTypographyTokens.CaptionMedium,
                    color = if (enabled) AppAccent else AppSecondaryText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = if (enabled) AppSecondaryText else AppDisabledText,
                modifier = Modifier.size(18.dp)
            )
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
