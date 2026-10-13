package com.company.attendance.sync

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.company.attendance.core.interfaces.PunchRepository
import com.company.attendance.core.model.Punch
import com.company.attendance.core.model.PunchType
import com.company.attendance.core.model.SyncStatus
import com.company.attendance.core.model.TimeSource
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.PunchSyncResultDto
import com.company.attendance.core.network.model.SyncBatchResponseDto
import com.company.attendance.core.storage.TokenStorage
import com.company.attendance.sync.work.AttendanceSyncWorker
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.io.IOException

class AttendanceSyncWorkerTest {

    private val context: Context = mockk(relaxed = true)
    private val workerParams: WorkerParameters = mockk(relaxed = true)
    private val punchRepository: PunchRepository = mockk(relaxed = true)
    private val apiService: AttendanceApiService = mockk(relaxed = true)
    private val tokenStorage: TokenStorage = mockk(relaxed = true)

    private lateinit var worker: AttendanceSyncWorker

    @Before
    fun setUp() {
        worker = AttendanceSyncWorker(
            appContext = context,
            workerParams = workerParams,
            punchRepository = punchRepository,
            apiService = apiService,
            tokenStorage = tokenStorage
        )
    }

    @Test
    fun doWork_whenNoUserLoggedIn_returnsSuccessWithoutSync() = runTest {
        every { tokenStorage.getUserId() } returns null

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { punchRepository.getPendingPunches(any()) }
        coVerify(exactly = 0) { apiService.syncPunches(any(), any()) }
    }

    @Test
    fun doWork_whenNoPendingPunches_returnsSuccess() = runTest {
        every { tokenStorage.getUserId() } returns "EMP1001"
        coEvery { punchRepository.getPendingPunches("EMP1001") } returns emptyList()

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify(exactly = 0) { apiService.syncPunches(any(), any()) }
    }

    @Test
    fun doWork_whenPendingPunchesExist_uploadsAndAcknowledgesSuccessfully() = runTest {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val tempImage = File.createTempFile("worker_test_", ".jpg")
        tempImage.writeText("sample photo bytes")
        assertTrue(tempImage.exists())

        val punchIn = Punch(
            punchUuid = "uuid-in-1",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            imagePath = tempImage.absolutePath,
            syncStatus = SyncStatus.PENDING
        )

        val punchOut = Punch(
            punchUuid = "uuid-out-1",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.OUT,
            punchTimeUtcEpochMs = 1759230000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 18f,
            matchScore = 0.89f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punchIn, punchOut)

        val batchSlot = slot<MultipartBody.Part>()
        val imagesSlot = slot<List<MultipartBody.Part>>()

        val responseDto = SyncBatchResponseDto(
            batchId = "batch-123",
            processedCount = 2,
            results = listOf(
                PunchSyncResultDto(punchUuid = "uuid-in-1", status = "ACCEPTED"),
                PunchSyncResultDto(punchUuid = "uuid-out-1", status = "DUPLICATE")
            )
        )

        coEvery {
            apiService.syncPunches(capture(batchSlot), capture(imagesSlot))
        } returns Response.success(responseDto)

        val result = worker.doWork()

        assertEquals(Result.success(), result)

        // Verify batch JSON payload and image parts
        assertTrue(imagesSlot.isCaptured)
        assertEquals(1, imagesSlot.captured.size)

        // Verify Room database updates
        coVerify { punchRepository.markPunchesSynced(listOf("uuid-in-1", "uuid-out-1")) }

        // Verify temporary image cleanup
        assertEquals(false, tempImage.exists())
    }

    @Test
    fun doWork_whenRejectedPunchesInResponse_marksThemFailed() = runTest {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-rejected",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING,
            syncAttempts = 1
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)

        val responseDto = SyncBatchResponseDto(
            batchId = "batch-124",
            processedCount = 1,
            results = listOf(
                PunchSyncResultDto(
                    punchUuid = "uuid-rejected",
                    status = "REJECTED",
                    reason = "Corrupt payload"
                )
            )
        )

        coEvery { apiService.syncPunches(any(), any()) } returns Response.success(responseDto)

        val result = worker.doWork()

        assertEquals(Result.success(), result)
        coVerify { punchRepository.markPunchSyncFailed("uuid-rejected", 2) }
        coVerify(exactly = 0) { punchRepository.markPunchesSynced(any()) }
    }

    @Test
    fun doWork_whenServerReturns500_triggersRetry() = runTest {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-retry",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING,
            syncAttempts = 0
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)
        coEvery { apiService.syncPunches(any(), any()) } returns Response.error(
            503,
            "Service Unavailable".toResponseBody()
        )

        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }

    @Test
    fun doWork_whenNetworkIOException_triggersRetry() = runTest {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-io",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)
        coEvery { apiService.syncPunches(any(), any()) } throws IOException("Host unreachable")

        val result = worker.doWork()

        assertEquals(Result.retry(), result)
    }

    @Test
    fun doWork_whenClientError400_returnsFailureAndMarksAttempts() = runTest {
        val userId = "EMP1001"
        every { tokenStorage.getUserId() } returns userId

        val punch = Punch(
            punchUuid = "uuid-400",
            userId = userId,
            dateLocal = "2026-09-30",
            type = PunchType.IN,
            punchTimeUtcEpochMs = 1759200000000L,
            timeSource = TimeSource.NTP,
            timeUnverified = false,
            latitude = 28.6139,
            longitude = 77.2090,
            distanceMeters = 15f,
            matchScore = 0.85f,
            imagePath = null,
            syncStatus = SyncStatus.PENDING,
            syncAttempts = 2
        )

        coEvery { punchRepository.getPendingPunches(userId) } returns listOf(punch)
        coEvery { apiService.syncPunches(any(), any()) } returns Response.error(
            400,
            "Bad Request".toResponseBody()
        )

        val result = worker.doWork()

        assertEquals(Result.failure(), result)
        coVerify { punchRepository.markPunchSyncFailed("uuid-400", 3) }
    }
}
