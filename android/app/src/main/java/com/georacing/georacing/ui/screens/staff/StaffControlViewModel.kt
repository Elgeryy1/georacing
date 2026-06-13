package com.georacing.georacing.ui.screens.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.domain.usecase.BroadcastEmergencyUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel del Panel de Control del Staff.
 * Gestiona el estado de emisión de la "Baliza Humana" (Dynamic Infrastructure).
 */
class StaffControlViewModel(
    private val broadcastEmergencyUseCase: BroadcastEmergencyUseCase
) : ViewModel() {

    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    private val _broadcastError = MutableStateFlow<String?>(null)
    val broadcastError: StateFlow<String?> = _broadcastError.asStateFlow()

    private var broadcastJob: Job? = null

    /**
     * Alterna la emisión BLE ON/OFF.
     * (Asume que la UI ya ha validado permisos BLUETOOTH_ADVERTISE).
     * @param isEvacuation Determina si el payload es EVACUATE o DANGER.
     */
    fun toggleBeacon(isEvacuation: Boolean = true) {
        if (_isBroadcasting.value) {
            stopBeacon()
        } else {
            startBeacon(isEvacuation)
        }
    }

    private fun startBeacon(isEvacuation: Boolean) {
        // Cancelar trabajo anterior si existiera
        broadcastJob?.cancel()
        _broadcastError.value = null

        broadcastJob = viewModelScope.launch {
            broadcastEmergencyUseCase(isEvacuation).collect { isSuccess ->
                if (isSuccess) {
                    _isBroadcasting.value = true
                } else {
                    _isBroadcasting.value = false
                    _broadcastError.value = "Error al iniciar la Baliza BLE. Asegúrate de tener el Bluetooth activado."
                }
            }
        }
    }

    private fun stopBeacon() {
        broadcastJob?.cancel()
        _isBroadcasting.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopBeacon()
    }
}
