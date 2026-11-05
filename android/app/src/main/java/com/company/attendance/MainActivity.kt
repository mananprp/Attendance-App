package com.company.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.design.navigation.AttendanceAppScaffold
import com.company.attendance.design.navigation.PunchCoordinator
import com.company.attendance.design.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var punchCoordinator: PunchCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDestination = if (tokenStorage.getToken() != null) {
            if (tokenStorage.hasPhoto()) {
                Screen.Home.route
            } else {
                Screen.OnboardingWizard.route
            }
        } else {
            Screen.Login.route
        }

        setContent {
            val session by tokenStorage.sessionFlow.collectAsState()
            val isAdmin = session?.role?.equals("ADMIN", ignoreCase = true) == true

            AttendanceAppScaffold(
                startDestination = startDestination,
                isAdmin = isAdmin,
                punchCoordinator = punchCoordinator
            )
        }
    }
}
