package com.company.attendance.face.di

import android.content.Context
import com.company.attendance.core.interfaces.FaceVerifier
import com.company.attendance.face.download.ModelDownloader
import com.company.attendance.face.verifier.FaceVerifierImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FaceModule {

    @Provides
    @Singleton
    fun provideModelDownloader(
        @ApplicationContext context: Context
    ): ModelDownloader {
        return ModelDownloader(
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideFaceVerifier(
        @ApplicationContext context: Context,
        modelDownloader: ModelDownloader
    ): FaceVerifier {
        return FaceVerifierImpl(
            context = context,
            modelDownloader = modelDownloader
        )
    }
}
