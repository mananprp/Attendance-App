package com.company.attendance.core.config

import com.company.attendance.core.database.EntityMappers.toDomain
import com.company.attendance.core.database.dao.AppConfigDao
import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.NetworkHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface RemoteConfigRepository {
    fun observeConfig(): Flow<AppConfig>
    suspend fun getConfig(): AppConfig
    suspend fun fetchAndCacheConfig(): Result<AppConfig>
}

@Singleton
class RemoteConfigRepositoryImpl @Inject constructor(
    private val apiService: AttendanceApiService,
    private val appConfigDao: AppConfigDao
) : RemoteConfigRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observeConfig(): Flow<AppConfig> {
        return appConfigDao.getConfig().map { entity ->
            entity?.toDomain() ?: DEFAULT_CONFIG
        }
    }

    override suspend fun getConfig(): AppConfig {
        return appConfigDao.getConfigDirect()?.toDomain() ?: DEFAULT_CONFIG
    }

    override suspend fun fetchAndCacheConfig(): Result<AppConfig> {
        return try {
            val response = apiService.getConfig()
            val parsedResult = NetworkHelper.parseResponse(response)
            
            parsedResult.mapCatching { dto ->
                val entity = AppConfigEntity(
                    id = 1,
                    officeLat = dto.officeLat,
                    officeLng = dto.officeLng,
                    radiusMeters = dto.radiusM,
                    timezone = dto.timezone,
                    syncWindowsJson = json.encodeToString(dto.syncWindows),
                    syncJitterMinutes = dto.syncJitterMinutes,
                    matchThreshold = dto.matchThreshold,
                    lastFetchedAt = System.currentTimeMillis()
                )
                appConfigDao.insertOrUpdate(entity)
                entity.toDomain()
            }
        } catch (e: Exception) {
            val cached = appConfigDao.getConfigDirect()?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    companion object {
        val DEFAULT_CONFIG = AppConfig(
            officeLat = 28.490293,
            officeLng = 77.060544,
            radiusMeters = 100,
            timezone = "Asia/Kolkata",
            syncWindows = listOf("16:00", "00:00"),
            syncJitterMinutes = 30,
            matchThreshold = 0.46f,
            lastFetchedAt = 0L
        )
    }
}
