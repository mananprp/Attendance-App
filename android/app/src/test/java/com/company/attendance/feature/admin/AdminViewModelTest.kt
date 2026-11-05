package com.company.attendance.feature.admin

import com.company.attendance.core.interfaces.LocationChecker
import com.company.attendance.core.network.AttendanceApiService
import com.company.attendance.core.network.model.AdminSiteDto
import com.company.attendance.core.network.model.AdminUserDto
import com.company.attendance.core.network.model.CreateSiteRequestDto
import com.company.attendance.core.storage.TokenStorage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val apiService: AttendanceApiService = mockk(relaxed = true)
    private val locationChecker: LocationChecker = mockk(relaxed = true)
    private val tokenStorage: TokenStorage = mockk(relaxed = true)

    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { apiService.getAdminSites() } returns Response.success(
            listOf(
                AdminSiteDto(
                    officeId = "DEFAULT_OFFICE",
                    name = "Headquarters",
                    latitude = 28.490293,
                    longitude = 77.060544,
                    radiusM = 100,
                    timezone = "Asia/Kolkata",
                    matchThreshold = 0.46f
                )
            )
        )
        coEvery { apiService.getAdminUsers() } returns Response.success(
            listOf(
                AdminUserDto(
                    userId = "EMP1001",
                    name = "Alice",
                    role = "EMPLOYEE",
                    siteCode = "DEFAULT_OFFICE",
                    siteName = "Headquarters",
                    status = "ACTIVE",
                    photoChangeUsed = false
                )
            )
        )
        viewModel = AdminViewModel(apiService, locationChecker, tokenStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadData_populatesSitesAndUsers() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.sites.size)
        assertEquals("Headquarters", state.sites[0].name)
        assertEquals(1, state.users.size)
    }

    @Test
    fun openAndCloseCreateSiteDialog_togglesState() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isCreateSiteDialogOpen)

        viewModel.openCreateSiteDialog()
        assertTrue(viewModel.uiState.value.isCreateSiteDialogOpen)

        viewModel.closeCreateSiteDialog()
        assertFalse(viewModel.uiState.value.isCreateSiteDialogOpen)
    }

    @Test
    fun createSite_success_appendsNewSiteAndClosesDialog() = runTest {
        advanceUntilIdle()

        val newSiteDto = AdminSiteDto(
            officeId = "SITE_NOIDA",
            name = "Noida Branch",
            latitude = 28.535516,
            longitude = 77.391026,
            radiusM = 150,
            timezone = "Asia/Kolkata",
            matchThreshold = 0.46f
        )
        coEvery {
            apiService.createAdminSite(
                CreateSiteRequestDto(
                    officeId = "SITE_NOIDA",
                    name = "Noida Branch",
                    latitude = 28.535516,
                    longitude = 77.391026,
                    radiusM = 150,
                    timezone = "Asia/Kolkata",
                    matchThreshold = 0.46f
                )
            )
        } returns Response.success(newSiteDto)

        viewModel.openCreateSiteDialog()
        viewModel.createSite(
            officeId = "SITE_NOIDA",
            name = "Noida Branch",
            latitude = 28.535516,
            longitude = 77.391026,
            radiusM = 150
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreateSiteDialogOpen)
        assertFalse(state.isActionLoading)
        assertEquals(2, state.sites.size)
        assertTrue(state.sites.any { it.officeId == "SITE_NOIDA" })
        assertTrue(state.successMessage?.contains("SITE_NOIDA") == true)
    }

    @Test
    fun fetchCurrentLocation_callsLocationChecker() = runTest {
        coEvery { locationChecker.getCurrentCoordinates() } returns Pair(28.535516, 77.391026)

        var fetchedLat: Double? = null
        var fetchedLng: Double? = null

        viewModel.fetchCurrentLocation { lat, lng ->
            fetchedLat = lat
            fetchedLng = lng
        }
        advanceUntilIdle()

        assertEquals(28.535516, fetchedLat ?: 0.0, 0.0001)
        assertEquals(77.391026, fetchedLng ?: 0.0, 0.0001)
    }
}
