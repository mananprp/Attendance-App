package com.company.attendance.core.di

import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.config.RemoteConfigRepositoryImpl
import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.interfaces.TrustedTimeProvider
import com.company.attendance.core.location.LocationCheckerImpl
import com.company.attendance.core.repository.PunchRepositoryImpl
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.core.storage.TokenStorageImpl
import com.company.attendance.core.time.ClockWrapper
import com.company.attendance.core.time.SntpClient
import com.company.attendance.core.time.SntpClientImpl
import com.company.attendance.core.time.SystemClockWrapper
import com.company.attendance.core.time.TrustedTimeProviderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindTokenStorage(
        impl: TokenStorageImpl
    ): TokenStorage

    @Binds
    @Singleton
    abstract fun bindClockWrapper(
        impl: SystemClockWrapper
    ): ClockWrapper

    @Binds
    @Singleton
    abstract fun bindSntpClient(
        impl: SntpClientImpl
    ): SntpClient

    @Binds
    @Singleton
    abstract fun bindTrustedTimeProvider(
        impl: TrustedTimeProviderImpl
    ): TrustedTimeProvider

    @Binds
    @Singleton
    abstract fun bindLocationChecker(
        impl: LocationCheckerImpl
    ): LocationChecker

    @Binds
    @Singleton
    abstract fun bindLocationMonitor(
        impl: com.company.attendance.core.location.LocationMonitorImpl
    ): com.company.attendance.core.location.LocationMonitor

    @Binds
    @Singleton
    abstract fun bindRemoteConfigRepository(
        impl: RemoteConfigRepositoryImpl
    ): RemoteConfigRepository

    @Binds
    @Singleton
    abstract fun bindPunchRepository(
        impl: PunchRepositoryImpl
    ): PunchRepository
}
