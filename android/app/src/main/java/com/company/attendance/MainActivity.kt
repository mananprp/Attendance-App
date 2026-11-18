package com.company.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.design.navigation.AttendanceAppScaffold
import com.company.attendance.design.navigation.PunchCoordinator
import com.company.attendance.design.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var punchCoordinator: PunchCoordinator

    @Inject
    lateinit var apiService: AttendanceApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On app launch, silently check if user's role on server was promoted
        if (!tokenStorage.getToken().isNullOrBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val currentUserId = tokenStorage.getUserId() ?: return@launch
                    val response = apiService.getAdminUsers()
                    if (response.isSuccessful && response.body() != null) {
                        val users = response.body()!!
                        val myRecord = users.find { it.userId.equals(currentUserId, ignoreCase = true) }
                        if (myRecord != null && !myRecord.role.equals(tokenStorage.getRole(), ignoreCase = true)) {
                            tokenStorage.updateRole(myRecord.role)
                        }
                    }
                } catch (_: Exception) {
                    // Silently ignore 403 Forbidden or offline errors (for standard employees)
                }
            }
        }

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
