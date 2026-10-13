package com.company.attendance.feature.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.company.attendance.design.components.AttendanceTopAppBar
import com.company.attendance.design.theme.AppAccent
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppBorder
import com.company.attendance.feature.onboarding.components.CameraPermissionStep
import com.company.attendance.feature.onboarding.components.LocationPermissionStep
import com.company.attendance.feature.onboarding.components.OnboardingCompleteStep
import com.company.attendance.feature.onboarding.components.PhotoCaptureStep
import com.company.attendance.feature.onboarding.model.OnboardingStep
import com.company.attendance.feature.onboarding.model.OnboardingUiState

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onboardingCompletedEvent.collect {
            onOnboardingComplete()
        }
    }

    OnboardingScreenContent(
        uiState = uiState,
        onCameraPermissionGranted = { viewModel.onCameraPermissionResult(true) },
        onPhotoCaptured = viewModel::onPhotoCaptured,
        onRetakePhoto = viewModel::onRetakePhoto,
        onUploadPhoto = viewModel::uploadPhoto,
        onLocationPermissionGranted = { viewModel.onLocationPermissionResult(true) },
        onComplete = viewModel::completeOnboarding,
        modifier = modifier
    )
}

@Composable
fun OnboardingScreenContent(
    uiState: OnboardingUiState,
    onCameraPermissionGranted: () -> Unit,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit,
    onRetakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = when (uiState.currentStep) {
        OnboardingStep.CAMERA_PERMISSION -> 0.2f
        OnboardingStep.PHOTO_CAPTURE -> 0.4f
        OnboardingStep.PHOTO_CONFIRMATION -> 0.6f
        OnboardingStep.LOCATION_PERMISSION -> 0.8f
        OnboardingStep.COMPLETED -> 1.0f
    }

    val stepTitle = when (uiState.currentStep) {
        OnboardingStep.CAMERA_PERMISSION -> "Step 1: Camera Access"
        OnboardingStep.PHOTO_CAPTURE -> "Step 2: Profile Photo"
        OnboardingStep.PHOTO_CONFIRMATION -> "Step 2: Verify Photo"
        OnboardingStep.LOCATION_PERMISSION -> "Step 3: Location Access"
        OnboardingStep.COMPLETED -> "Setup Complete"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AttendanceTopAppBar(
            title = stepTitle,
            showBottomDivider = false
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = AppAccent,
            trackColor = AppBorder,
        )

        Crossfade(
            targetState = uiState.currentStep,
            label = "OnboardingStepCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { step ->
            when (step) {
                OnboardingStep.CAMERA_PERMISSION -> {
                    CameraPermissionStep(
                        onPermissionGranted = onCameraPermissionGranted
                    )
                }
                OnboardingStep.PHOTO_CAPTURE, OnboardingStep.PHOTO_CONFIRMATION -> {
                    PhotoCaptureStep(
                        uiState = uiState,
                        onPhotoCaptured = onPhotoCaptured,
                        onRetakePhoto = onRetakePhoto,
                        onUploadPhoto = onUploadPhoto
                    )
                }
                OnboardingStep.LOCATION_PERMISSION -> {
                    LocationPermissionStep(
                        onPermissionGranted = onLocationPermissionGranted
                    )
                }
                OnboardingStep.COMPLETED -> {
                    OnboardingCompleteStep(
                        onComplete = onComplete
                    )
                }
            }
        }
    }
}
