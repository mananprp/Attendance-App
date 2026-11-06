package com.company.attendance.feature.tracking

import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.feature.tracking.repository.TrackingRepository
import com.company.attendance.feature.tracking.repository.TrackingTripSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val trackingRepository: TrackingRepository = mockk(relaxed = true)
    private val sessionFlow = MutableSharedFlow<TrackingSessionEntity?>()

    private lateinit var viewModel: TrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { trackingRepository.observeActiveSession() } returns sessionFlow
        viewModel = TrackingViewModel(trackingRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `activeSession emits when repository session flow emits`() = runTest {
        val testSession = TrackingSessionEntity(
            sessionId = "session-123",
            startTimeUtc = 1000L,
            status = "ACTIVE",
            totalDistanceM = 250f
        )

        sessionFlow.emit(testSession)
        advanceUntilIdle()

        assertEquals(testSession, viewModel.activeSession.value)
    }

    @Test
    fun `startTracking delegates to repository`() = runTest {
        viewModel.startTracking()
        advanceUntilIdle()

        coVerify { trackingRepository.startTracking() }
    }

    @Test
    fun `openAddStopDialog and dismissAddStopDialog toggle state properly`() {
        assertFalse(viewModel.showAddStopDialog.value)

        viewModel.openAddStopDialog()
        assertTrue(viewModel.showAddStopDialog.value)

        viewModel.dismissAddStopDialog()
        assertFalse(viewModel.showAddStopDialog.value)
    }

    @Test
    fun `confirmAddStop calls repository and closes dialog`() = runTest {
        viewModel.openAddStopDialog()
        assertTrue(viewModel.showAddStopDialog.value)

        viewModel.confirmAddStop("Client Office")
        advanceUntilIdle()

        coVerify { trackingRepository.addStop("Client Office") }
        assertFalse(viewModel.showAddStopDialog.value)
    }

    @Test
    fun `stopTracking populates completedTripSummary and resets isStopping`() = runTest {
        val summary = TrackingTripSummary(
            sessionId = "s1",
            startTimeUtc = 1000L,
            endTimeUtc = 2000L,
            totalDistanceM = 1500f,
            points = emptyList(),
            stops = emptyList()
        )
        coEvery { trackingRepository.stopTracking() } returns summary

        assertFalse(viewModel.isStopping.value)
        viewModel.stopTracking()
        advanceUntilIdle()

        coVerify { trackingRepository.stopTracking() }
        assertEquals(summary, viewModel.completedTripSummary.value)
        assertFalse(viewModel.isStopping.value)
    }

    @Test
    fun `resumeTracking calls repository resumeTracking`() {
        viewModel.resumeTracking()
        verify { trackingRepository.resumeTracking() }
    }

    @Test
    fun `clearCompletedTrip resets summary to null`() = runTest {
        val summary = TrackingTripSummary(
            sessionId = "s1",
            startTimeUtc = 1000L,
            endTimeUtc = 2000L,
            totalDistanceM = 1500f,
            points = emptyList(),
            stops = emptyList()
        )
        coEvery { trackingRepository.stopTracking() } returns summary
        viewModel.stopTracking()
        advanceUntilIdle()

        assertEquals(summary, viewModel.completedTripSummary.value)

        viewModel.clearCompletedTrip()
        assertNull(viewModel.completedTripSummary.value)
    }
}
