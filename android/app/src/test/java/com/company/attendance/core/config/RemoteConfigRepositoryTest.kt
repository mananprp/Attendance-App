package com.company.attendance.core.config

import com.company.attendance.core.database.dao.AppConfigDao
import com.company.attendance.core.database.entity.AppConfigEntity
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.AppConfigResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class RemoteConfigRepositoryTest {

    private val apiService: AttendanceApiService = mockk()
    private val appConfigDao: AppConfigDao = mockk(relaxed = true)

    private lateinit var repository: RemoteConfigRepositoryImpl

    private val sampleDto = AppConfigResponseDto(
        officeLat = 28.613939,
        officeLng = 77.209021,
        radiusM = 150,
        timezone = "Asia/Kolkata",
        syncWindows = listOf("16:00", "00:00"),
        syncJitterMinutes = 45,
        matchThreshold = 0.60f
    )

    @Before
    fun setUp() {
        repository = RemoteConfigRepositoryImpl(apiService, appConfigDao)
    }

    @Test
    fun `fetchAndCacheConfig on network success caches in Dao and returns domain config`() = runTest {
        coEvery { apiService.getConfig() } returns Response.success(sampleDto)

        val entitySlot = slot<AppConfigEntity>()
        coEvery { appConfigDao.insertOrUpdate(capture(entitySlot)) } returns Unit

        val result = repository.fetchAndCacheConfig()

        assertTrue(result.isSuccess)
        val config = result.getOrNull()
        assertEquals(28.613939, config?.officeLat ?: 0.0, 0.0001)
        assertEquals(150, config?.radiusMeters)
        assertEquals(0.60f, config?.matchThreshold ?: 0f, 0.001f)

        coVerify(exactly = 1) { appConfigDao.insertOrUpdate(any()) }
        assertEquals(150, entitySlot.captured.radiusMeters)
    }

    @Test
    fun `fetchAndCacheConfig on network failure returns cached Dao entity if present`() = runTest {
        coEvery { apiService.getConfig() } returns Response.error(500, "Server error".toResponseBody())
        
        val cachedEntity = AppConfigEntity(
            id = 1,
            officeLat = 28.613939,
            officeLng = 77.209021,
            radiusMeters = 100,
            timezone = "Asia/Kolkata",
            syncWindowsJson = "[\"16:00\", \"00:00\"]",
            syncJitterMinutes = 30,
            matchThreshold = 0.55f,
            lastFetchedAt = 1000L
        )
        coEvery { appConfigDao.getConfigDirect() } returns cachedEntity

        val result = repository.fetchAndCacheConfig()

        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull()?.radiusMeters)
    }
}
