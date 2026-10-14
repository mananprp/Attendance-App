package com.company.attendance.design.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.company.attendance.core.model.PunchType
import com.company.attendance.design.gallery.ComponentGalleryScreen
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AppTypographyTokens

/**
 * AttendanceNavGraph: Connects all app destinations and allows feature screens to be plugged in.
 */
@Composable
fun AttendanceNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Login.route,
    loginScreen: (@Composable (onLoginSuccess: () -> Unit) -> Unit)? = null,
    onboardingScreen: (@Composable (onOnboardingComplete: () -> Unit) -> Unit)? = null,
    modelDownloadScreen: (@Composable (onDownloadComplete: () -> Unit) -> Unit)? = null,
    homeScreen: (@Composable (onNavigateToPunch: (PunchType) -> Unit, onNavigateToProfile: () -> Unit) -> Unit)? = null,
    punchCaptureScreen: (@Composable (punchType: PunchType, onCaptureComplete: () -> Unit) -> Unit)? = null,
    calendarScreen: (@Composable () -> Unit)? = null,
    profileSettingsScreen: (@Composable (onSignOut: () -> Unit, onNavigateToGallery: () -> Unit) -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            if (loginScreen != null) {
                loginScreen {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            } else {
                com.company.attendance.feature.auth.LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.OnboardingWizard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.OnboardingWizard.route) {
            if (onboardingScreen != null) {
                onboardingScreen {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                com.company.attendance.feature.onboarding.OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.ModelDownload.route) {
            if (modelDownloadScreen != null) {
                modelDownloadScreen {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ModelDownload.route) { inclusive = true }
                    }
                }
            } else {
                PlaceholderScreen(
                    title = "Model Download Screen",
                    onAction = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }
        }

        composable(Screen.Home.route) {
            if (homeScreen != null) {
                homeScreen(
                    onNavigateToPunch = { punchType ->
                        navController.navigate(Screen.PunchCapture.createRoute(punchType))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileSettings.route)
                    }
                )
            } else {
                com.company.attendance.feature.punch.HomeScreen(
                    onNavigateToPunchCapture = { punchType, locationType, lat, lng, dist ->
                        navController.navigate(Screen.PunchCapture.createRoute(punchType, locationType, lat, lng, dist))
                    },
                    onNavigateToProfile = {
                        navController.navigate(Screen.ProfileSettings.route)
                    }
                )
            }
        }

        composable(
            route = Screen.PunchCapture.route,
            arguments = Screen.PunchCapture.arguments
        ) { backStackEntry ->
            val punchTypeName = backStackEntry.arguments?.getString(Screen.ARG_PUNCH_TYPE) ?: PunchType.IN.name
            val locationType = backStackEntry.arguments?.getString(Screen.ARG_LOCATION_TYPE) ?: "OFFICE"
            val lat = backStackEntry.arguments?.getString(Screen.ARG_LAT)?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString(Screen.ARG_LNG)?.toDoubleOrNull() ?: 0.0
            val dist = backStackEntry.arguments?.getString(Screen.ARG_DIST)?.toFloatOrNull() ?: 0.0f
            val punchType = runCatching { PunchType.valueOf(punchTypeName) }.getOrDefault(PunchType.IN)

            if (punchCaptureScreen != null) {
                punchCaptureScreen(punchType) {
                    navController.popBackStack()
                }
            } else {
                com.company.attendance.feature.punch.capture.PunchCaptureScreen(
                    punchType = punchType,
                    latitude = lat,
                    longitude = lng,
                    distanceMeters = dist,
                    locationType = locationType,
                    onCaptureComplete = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.Calendar.route) {
            if (calendarScreen != null) {
                calendarScreen()
            } else {
                com.company.attendance.feature.calendar.CalendarScreen()
            }
        }

        composable(Screen.ProfileSettings.route) {
            if (profileSettingsScreen != null) {
                profileSettingsScreen(
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.ComponentGallery.route)
                    }
                )
            } else {
                com.company.attendance.feature.auth.profile.ProfileScreen(
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToGallery = {
                        navController.navigate(Screen.ComponentGallery.route)
                    },
                    onNavigateToAdminPanel = {
                        navController.navigate(Screen.AdminPanel.route)
                    }
                )
            }
        }

        composable(Screen.AdminPanel.route) {
            com.company.attendance.feature.admin.AdminScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ComponentGallery.route) {
            ComponentGalleryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    onAction: (() -> Unit)? = null,
    actionLabel: String = "Continue"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            modifier = Modifier.padding(com.company.attendance.design.theme.AppDimensions.ScreenPadding)
        ) {
            Text(
                text = title,
                style = AppTypographyTokens.Title
            )
            if (onAction != null) {
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.height(com.company.attendance.design.theme.AppDimensions.Spacing16)
                )
                com.company.attendance.design.components.PrimaryButton(
                    text = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}
