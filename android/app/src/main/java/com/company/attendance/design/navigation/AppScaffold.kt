package com.company.attendance.design.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.company.attendance.design.components.AttendanceBottomNavBar
import com.company.attendance.design.theme.AppBackground
import com.company.attendance.design.theme.AttendanceTheme

/**
 * AppScaffold: Root UI scaffold managing theme, bottom navigation, and top-level NavHost.
 */
@Composable
fun AttendanceAppScaffold(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
    isAdmin: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    AttendanceTheme {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = Screen.shouldShowBottomBar(currentRoute)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showBottomBar) {
                    AttendanceBottomNavBar(
                        currentRoute = currentRoute,
                        isAdmin = isAdmin,
                        onNavigateToRoute = { targetRoute ->
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            AttendanceNavGraph(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
