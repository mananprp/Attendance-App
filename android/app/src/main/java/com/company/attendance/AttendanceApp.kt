package com.company.attendance

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.core.storage.TokenStorage
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AttendanceApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tokenStorage: TokenStorage

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // If employee is logged in, ensure background sync windows (16:00, 00:00) are enqueued with WorkManager
        if (!tokenStorage.getUserId().isNullOrBlank()) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val config = remoteConfigRepository.getConfig()
                    syncScheduler.scheduleConfiguredWindows(config.syncWindows, config.syncJitterMinutes)
                } catch (e: Exception) {
                    Log.w("AttendanceApp", "Failed to schedule sync windows on app start", e)
                }
            }
        }
    }
}
