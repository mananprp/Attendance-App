package com.company.attendance.sync

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.company.attendance.core.config.RemoteConfigRepository
import com.company.attendance.core.interfaces.ManualSyncResult
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.AppConfig
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PunchSyncResultDto
import com.company.attendance.core.network.model.SyncBatchResponseDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.sync.scheduler.SyncSchedulerImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SyncSchedulerTest {

    private val context: Context = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)
    private val apiService: AttendanceApiService = mockk(relaxed = true)
    private val tokenStorage: TokenStorage = mockk(relaxed = true)
    private val remoteConfigRepository: RemoteConfigRepository = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var scheduler: SyncSchedulerImpl

    @Before
    fun setUp() {
        coEvery { remoteConfigRepository.getConfig() } returns AppConfig(
            officeLat = 28.613939,
            officeLng = 77.209021,
            radiusMeters = 100,
            timezone = "Asia/Kolkata",
            syncWindows = listOf("16:00", "00:00"),
            syncJitterMinutes = 30,
            matchThreshold = 0.55f
        )

        scheduler = SyncSchedulerImpl(
            context = context,
            workManager = workManager,
            punchRepository = punchRepository,
            apiService = apiService,
            tokenStorage = tokenStorage,
            remoteConfigRepository = remoteConfigRepository
        )
    }

    @Test
    fun scheduleConfiguredWindows_enqueuesPeriodicWorkForEachWindow() {
        val workNameSlot = mutableListOf<String>()
        val policySlot = slot<ExistingPeriodicWorkPolicy>()
        val requestSlot = mutableListOf<PeriodicWorkRequest>()

        every {
            workManager.enqueueUniquePeriodicWork(
                capture(workNameSlot),
                capture(policySlot),
                capture(requestSlot)
            )
        } returns mockk(relaxed = true)

        scheduler.scheduleConfiguredWindows(listOf("16:00", "00:00"), jitterMinutes = 30)

        // Allow background launch to complete
        var waited = 0
        while (workNameSlot.size < 2 && waited < 2000) {
            Thread.sleep(50)
            waited += 50
        }

        assertEquals(2, workNameSlot.size)
        assertTrue(workNameSlot.contains("attendance_sync_window_16_00"))
        assertTrue(workNameSlot.contains("attendance_sync_window_00_00"))
        assertEquals(ExistingPeriodicWorkPolicy.KEEP, policySlot.captured)
    }

    @Test
    fun triggerSyncNow_whenNotLoggedIn_returnsFailure() = runTest(testDispatcher) {
        every { tokenStorage.getUserId() } returns null

        val result = scheduler.triggerSyncNow()

        assertTrue(result is ManualSyncResult.Failure)
        assertEquals("User not logged in", (result as ManualSyncResult.Failure).errorMessage)
        assertEquals(false, scheduler.syncState.value.isSyncing)
    }

    @Test
    fun triggerSyncNow_whenNoPendingPunches_returnsNoData() = runTest(testDispatcher) {
        every { tokenStorage.getUserId() } returns "EMP1001"
        coEvery { punchRepository.getPendingPunches("EMP1001") } returns emptyList()

        val result = scheduler.triggerSyncNow()

        assertTrue(result is ManualSyncResult.NoData)
        assertEquals(0, scheduler.syncState.value.pendingCount)
        assertEquals(false, scheduler.syncState.value.isSyncing)
    }

    @Test
    fun triggerSyncNow_whenPendingPunchesExist_successfulSyncAcknowledgesAndCleansImages() = runTest(testDispatcher) {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val tempImage = File.createTempFile("test_punch_", ".jpg")
        tempImage.writeText("fake image bytes")
        assertTrue(tempImage.exists())

        val punch1 = Punch(
            punchUuid = "uuid-111",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 10f,
            matchScore = 0.88f,
            imagePath = tempImage.absolutePath,
            syncStatus = SyncStatus.PENDING
        )

        val punch2 = Punch(
            punchUuid = "uuid-222",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.OUT,
            punchTimeUtcEpochMs = 1759230000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 12f,
            matchScore = 0.91f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { punchRepository.getPendingPunches(userId) } returnsMany listOf(
            listOf(punch1, punch2), // initial query
            emptyList()             // after sync
        )

        val syncResponse = SyncBatchResponseDto(
            batchId = "batch-999",
            processedCount = 2,
            results = listOf(
                PunchSyncResultDto(punchUuid = "uuid-111", status = "ACCEPTED"),
                PunchSyncResultDto(punchUuid = "uuid-222", status = "DUPLICATE")
            )
        )

        coEvery { apiService.syncPunches(any(), any()) } returns Response.success(syncResponse)

        val result = scheduler.triggerSyncNow()

        assertTrue(result is ManualSyncResult.Success)
        assertEquals(2, (result as ManualSyncResult.Success).uploadedCount)

        coVerify { punchRepository.markPunchesSynced(listOf("uuid-111", "uuid-222")) }
        assertEquals(0, scheduler.syncState.value.pendingCount)
        assertEquals(false, scheduler.syncState.value.isSyncing)
        assertNotNull(scheduler.syncState.value.lastSyncedEpochMs)
        assertNull(scheduler.syncState.value.lastError)

        // Verify temporary image was cleaned up
        assertEquals(false, tempImage.exists())
    }

    @Test
    fun triggerSyncNow_whenServerErrorOccurs_marksFailedAndUpdatesState() = runTest(testDispatcher) {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-111",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 10f,
            matchScore = 0.88f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING,
            syncAttempts = 0
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)
        coEvery { apiService.syncPunches(any(), any()) } returns Response.error(
            500,
            "Internal Server Error".toResponseBody()
        )

        val result = scheduler.triggerSyncNow()

        assertTrue(result is ManualSyncResult.Failure)
        coVerify { punchRepository.markPunchSyncFailed("uuid-111", 1) }
        assertEquals(false, scheduler.syncState.value.isSyncing)
        assertNotNull(scheduler.syncState.value.lastError)
    }

    @Test
    fun triggerSyncNow_whenNetworkException_returnsFailure() = runTest(testDispatcher) {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-111",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 10f,
            matchScore = 0.88f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)
        coEvery { apiService.syncPunches(any(), any()) } throws IOException("Connection timed out")

        val result = scheduler.triggerSyncNow()

        assertTrue(result is ManualSyncResult.Failure)
        assertEquals("Connection timed out", (result as ManualSyncResult.Failure).errorMessage)
        assertEquals(false, scheduler.syncState.value.isSyncing)
        assertEquals("Connection timed out", scheduler.syncState.value.lastError)
    }

    @Test
    fun refreshPendingCount_updatesStateCorrectly() = runTest(testDispatcher) {
        every { tokenStorage.getUserId() } returns "EMP1001"
        coEvery { punchRepository.getPendingPunches("EMP1001") } returns listOf(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        scheduler.refreshPendingCount()

        assertEquals(3, scheduler.syncState.value.pendingCount)
    }
}
