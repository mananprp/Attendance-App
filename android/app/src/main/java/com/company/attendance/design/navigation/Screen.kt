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
    data object ProfileSettings : Screen("profile_settings")
    data object AdminPanel : Screen("admin_panel")
    data object ComponentGallery : Screen("component_gallery")

    data object PunchCapture : Screen(
        route = "punch_capture/{$ARG_PUNCH_TYPE}",
        arguments = listOf(
            navArgument(ARG_PUNCH_TYPE) {
                type = NavType.StringType
                nullable = false
            }
        )
    ) {
        fun createRoute(punchType: PunchType): String = "punch_capture/${punchType.name}"
    }

    companion object {
        const val ARG_PUNCH_TYPE = "punchType"

        /**
         * List of top-level destinations that display the bottom navigation bar.
         */
        val bottomNavRoutes: Set<String> by lazy { setOf(Home.route, Calendar.route, AdminPanel.route) }

        fun shouldShowBottomBar(route: String?): Boolean {
            if (route == null) return false
            return route in bottomNavRoutes
        }
    }
}
