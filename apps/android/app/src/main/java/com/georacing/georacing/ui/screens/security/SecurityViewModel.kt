package com.georacing.georacing.ui.screens.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.infrastructure.security.MedicalWallpaperGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado UI para la generación del fondo médico.
 */
sealed class SecurityUiState {
    object Idle : SecurityUiState()
    object Loading : SecurityUiState()
    object Success : SecurityUiState()
    data class Error(val message: String) : SecurityUiState()
}

/**
 * ViewModel que conecta el generador de infraestructura con la UI de Settings o SOS.
 */
class SecurityViewModel(
    private val medicalWallpaperGenerator: MedicalWallpaperGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    /**
     * Inicia el proceso de sobreescribir el Lock Screen.
     */
    fun activateMedicalLockScreen(bloodType: String, emergencyContact: String, ticketId: String) {
        // Validación básica
        if (bloodType.isBlank() || emergencyContact.isBlank()) {
            _uiState.value = SecurityUiState.Error("Faltan datos médicos requeridos.")
            return
        }

        _uiState.value = SecurityUiState.Loading

        viewModelScope.launch {
            val success = medicalWallpaperGenerator.applyEmergencyWallpaper(
                bloodType = bloodType,
                emergencyContact = emergencyContact,
                ticketId = ticketId
            )
            
            if (success) {
                _uiState.value = SecurityUiState.Success
            } else {
                _uiState.value = SecurityUiState.Error("No se pudo aplicar el fondo de pantalla. Verifica los permisos de SET_WALLPAPER.")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = SecurityUiState.Idle
    }
}
