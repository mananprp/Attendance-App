package com.company.attendance.design.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.company.attendance.core.model.PunchType

/**
 * All navigation routes and destinations in Attendance App.
 */
sealed class Screen(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList()
) {
    data object Login : Screen("login")
    data object OnboardingWizard : Screen("onboarding")
    data object ModelDownload : Screen("model_download")
    data object Home : Screen("home")
    data object Calendar : Screen("calendar")
    data object Tracker : Screen("tracker")
    data object ProfileSettings : Screen("profile_settings")
    data object AdminPanel : Screen("admin_panel")
    data object ComponentGallery : Screen("component_gallery")

    data object PunchCapture : Screen(
        route = "punch_capture/{$ARG_PUNCH_TYPE}?locationType={$ARG_LOCATION_TYPE}&lat={$ARG_LAT}&lng={$ARG_LNG}&dist={$ARG_DIST}",
        arguments = listOf(
            navArgument(ARG_PUNCH_TYPE) {
                type = NavType.StringType
                nullable = false
            },
            navArgument(ARG_LOCATION_TYPE) {
                type = NavType.StringType
                defaultValue = "OFFICE"
            },
            navArgument(ARG_LAT) {
                type = NavType.StringType
                defaultValue = "0.0"
            },
            navArgument(ARG_LNG) {
                type = NavType.StringType
                defaultValue = "0.0"
            },
            navArgument(ARG_DIST) {
                type = NavType.StringType
                defaultValue = "0.0"
            }
        )
    ) {
        fun createRoute(
            punchType: PunchType,
            locationType: String = "OFFICE",
            latitude: Double = 0.0,
            longitude: Double = 0.0,
            distanceMeters: Float = 0.0f
        ): String =
            "punch_capture/${punchType.name}?locationType=$locationType&lat=$latitude&lng=$longitude&dist=$distanceMeters"
    }

    companion object {
        const val ARG_PUNCH_TYPE = "punchType"
        const val ARG_LOCATION_TYPE = "locationType"
        const val ARG_LAT = "lat"
        const val ARG_LNG = "lng"
        const val ARG_DIST = "dist"

        /**
         * List of top-level destinations that display the bottom navigation bar (Findez 4-slot bar).
         */
        val bottomNavRoutes: Set<String> by lazy { setOf(Home.route, Calendar.route, Tracker.route, ProfileSettings.route, AdminPanel.route) }

        fun shouldShowBottomBar(route: String?): Boolean {
            if (route == null) return false
            return route in bottomNavRoutes
        }
    }
}
