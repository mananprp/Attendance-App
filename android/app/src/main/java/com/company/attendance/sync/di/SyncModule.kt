package com.company.attendance.sync.di

import android.content.Context
import androidx.work.WorkManager
import com.company.attendance.core.interfaces.SyncScheduler
import com.company.attendance.sync.scheduler.SyncSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        impl: SyncSchedulerImpl
    ): SyncScheduler

    companion object {

        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context
        ): WorkManager {
            return WorkManager.getInstance(context)
        }

        @Provides
        fun provideIoDispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }
    }
}
