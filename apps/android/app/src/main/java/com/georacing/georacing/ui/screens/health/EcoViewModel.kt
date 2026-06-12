package com.georacing.georacing.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.domain.model.EcoMetrics
import com.georacing.georacing.domain.usecase.CalculateEcoMetricsUseCase
import com.georacing.georacing.infrastructure.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la Vista
sealed class EcoUiState {
    object Loading : EcoUiState()
    object NotAvailable : EcoUiState() // Health Connect no instalado o soportado
    object PermissionsRequired : EcoUiState() // Falta conceder los permisos
    data class Success(val metrics: EcoMetrics) : EcoUiState()
    data class Error(val message: String) : EcoUiState()
}

class EcoViewModel(
    private val healthConnectManager: HealthConnectManager,
    private val calculateEcoMetricsUseCase: CalculateEcoMetricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<EcoUiState>(EcoUiState.Loading)
    val uiState: StateFlow<EcoUiState> = _uiState.asStateFlow()

    // Este es el contrato que enviaremos a la UI (Compose)
    val permissionsContract = healthConnectManager.requestPermissionsActivityContract()
    val requiredPermissions = healthConnectManager.requiredPermissions

    init {
        checkHealthConnectStatus()
    }

    /**
     * Verifica la disponibilidad y permisos. Compose lo llamará en el onResume
     * o al pulsar "Recargar" tras otorgar permisos.
     */
    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            _uiState.value = EcoUiState.Loading
            
            if (!healthConnectManager.isAvailable()) {
                _uiState.value = EcoUiState.NotAvailable
                return@launch
            }

            if (!healthConnectManager.hasAllPermissions()) {
                _uiState.value = EcoUiState.PermissionsRequired
                return@launch
            }

            // Tenemos permisos, leemos los datos silenciosamente
            loadEcoMetrics()
        }
    }

    /**
     * Callback que la UI invocará después de que el usuario vuelva
     * del popup de permisos de Health Connect.
     */
    fun onPermissionsResult(grantedPermissions: Set<String>) {
        if (grantedPermissions.containsAll(requiredPermissions)) {
            loadEcoMetrics()
        } else {
            // El usuario denegó los permisos
            _uiState.value = EcoUiState.PermissionsRequired
        }
    }

    private fun loadEcoMetrics() {
        viewModelScope.launch {
            try {
                val metrics = calculateEcoMetricsUseCase()
                _uiState.value = EcoUiState.Success(metrics)
            } catch (e: Exception) {
                _uiState.value = EcoUiState.Error("Error al calcular el progreso ecológico: ${e.message}")
            }
        }
    }
}
