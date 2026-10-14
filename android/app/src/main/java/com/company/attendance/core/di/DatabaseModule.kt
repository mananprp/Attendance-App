package com.company.attendance.core.di

import android.content.Context
import androidx.room.Room
import com.company.attendance.core.database.AppDatabase
import com.company.attendance.core.database.dao.AppConfigDao
import com.company.attendance.core.database.dao.DaySummaryDao
import com.company.attendance.core.database.dao.PunchDao
import com.company.attendance.core.database.dao.UserLocalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserLocalDao(database: AppDatabase): UserLocalDao {
        return database.userLocalDao()
    }

    @Provides
    @Singleton
    fun providePunchDao(database: AppDatabase): PunchDao {
        return database.punchDao()
    }

    @Provides
    @Singleton
    fun provideDaySummaryDao(database: AppDatabase): DaySummaryDao {
        return database.daySummaryDao()
    }

    @Provides
    @Singleton
    fun provideAppConfigDao(database: AppDatabase): AppConfigDao {
        return database.appConfigDao()
    }
}
