package com.georacing.georacing.ui.screens.eco

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.health.HealthConnectManager
import com.georacing.georacing.domain.calculator.EcoCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import com.georacing.georacing.data.local.UserPreferencesDataStore

data class EcoUiState(
    val steps: Long = 0,
    val distanceMeters: Double = 0.0,
    val co2SavedGrams: Double = 0.0,
    val isHealthConnectAvailable: Boolean = false,
    val hasPermissions: Boolean = false,
    val isLoading: Boolean = false
)

class EcoViewModel(
    private val healthManager: HealthConnectManager,
    private val userPrefs: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EcoUiState())
    val uiState: StateFlow<EcoUiState> = _uiState.asStateFlow()

    // Event to trigger permission request in UI (Activity)
    private val _requestPermissionEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val requestPermissionEvent: kotlinx.coroutines.flow.SharedFlow<Unit> = _requestPermissionEvent.asSharedFlow()

    fun checkAvailabilityAndLoad() {
        _uiState.value = _uiState.value.copy(
            isHealthConnectAvailable = healthManager.isAvailable()
        )
        refreshData()
    }

    fun checkAndRequestPermissions() {
        viewModelScope.launch {
            if (healthManager.isAvailable()) {
                if (healthManager.hasPermissions()) {
                    android.util.Log.d("EcoViewModel", "Permisos ya concedidos. Refrescando datos.")
                    refreshData() // Already have them
                } else {
                    android.util.Log.d("EcoViewModel", "Solicitando permisos via Evento.")
                    _requestPermissionEvent.emit(Unit) // Trigger Activity Launcher
                }
            } else {
                android.util.Log.e("EcoViewModel", "Health Connect no disponible.")
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val hasPerms = healthManager.hasPermissions()
            
            if (hasPerms) {
                // Get the circuit arrival time if any
                val arrivalTimeMs = userPrefs.circuitArrivalTime.first()
                val startTime = arrivalTimeMs?.let { Instant.ofEpochMilli(it) }

                val metrics = healthManager.readDailyMetrics(startTime)
                val co2 = EcoCalculator.calculateCo2Saved(metrics.distanceMeters)
                
                _uiState.value = _uiState.value.copy(
                    steps = metrics.steps,
                    distanceMeters = metrics.distanceMeters,
                    co2SavedGrams = co2,
                    hasPermissions = true,
                    isLoading = false
                )
            } else {
                 _uiState.value = _uiState.value.copy(
                    hasPermissions = false,
                    isLoading = false
                )
            }
        }
    }

    class Factory(
        private val healthConnectManager: HealthConnectManager,
        private val userPrefs: UserPreferencesDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EcoViewModel(healthConnectManager, userPrefs) as T
        }
    }
}
