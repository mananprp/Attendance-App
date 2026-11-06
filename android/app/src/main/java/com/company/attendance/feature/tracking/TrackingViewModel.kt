package com.company.attendance.feature.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.attendance.core.database.entity.TrackingSessionEntity
import com.company.attendance.core.database.entity.TrackingStopEntity
import com.company.attendance.feature.tracking.repository.TrackingRepository
import com.company.attendance.feature.tracking.repository.TrackingTripSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val trackingRepository: TrackingRepository
) : ViewModel() {

    private val _activeSession = MutableStateFlow<TrackingSessionEntity?>(null)
    val activeSession: StateFlow<TrackingSessionEntity?> = _activeSession.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeStops: StateFlow<List<TrackingStopEntity>> = _activeSession
        .flatMapLatest { session ->
            if (session != null) {
                trackingRepository.observeStopsForSession(session.sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _completedTripSummary = MutableStateFlow<TrackingTripSummary?>(null)
    val completedTripSummary: StateFlow<TrackingTripSummary?> = _completedTripSummary.asStateFlow()

    private val _showAddStopDialog = MutableStateFlow(false)
    val showAddStopDialog: StateFlow<Boolean> = _showAddStopDialog.asStateFlow()

    private val _isStopping = MutableStateFlow(false)
    val isStopping: StateFlow<Boolean> = _isStopping.asStateFlow()

    init {
        viewModelScope.launch {
            trackingRepository.observeActiveSession().collect { session ->
                _activeSession.value = session
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            trackingRepository.startTracking()
        }
    }

    fun openAddStopDialog() {
        _showAddStopDialog.value = true
    }

    fun dismissAddStopDialog() {
        _showAddStopDialog.value = false
    }

    fun confirmAddStop(stopName: String) {
        viewModelScope.launch {
            trackingRepository.addStop(stopName)
            _showAddStopDialog.value = false
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            _isStopping.value = true
            try {
                val summary = trackingRepository.stopTracking()
                if (summary != null) {
                    _completedTripSummary.value = summary
                }
            } finally {
                _isStopping.value = false
            }
        }
    }

    fun resumeTracking() {
        trackingRepository.resumeTracking()
    }

    fun clearCompletedTrip() {
        _completedTripSummary.value = null
    }
}
