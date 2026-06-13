package com.georacing.georacing.ui.screens.incidents

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.domain.model.IncidentCategory
import com.georacing.georacing.domain.model.IncidentReport
import com.georacing.georacing.domain.repository.IncidentsRepository
import com.georacing.georacing.utils.ImageUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class IncidentViewModel(
    application: Application,
    private val incidentsRepository: IncidentsRepository
) : AndroidViewModel(application) {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    // Loading state for compression
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // Selected photo URI
    private val _selectedPhotoUri = MutableStateFlow<Uri?>(null)
    val selectedPhotoUri: StateFlow<Uri?> = _selectedPhotoUri.asStateFlow()

    fun setPhotoUri(uri: Uri?) {
        _selectedPhotoUri.value = uri
    }

    // Ubicación actual para geolocalizar incidencias
    private val _currentLocation = MutableStateFlow<android.location.Location?>(null)
    
    fun updateLocation(location: android.location.Location) {
        _currentLocation.value = location
    }
    
    init {
        // Obtener última ubicación conocida al inicializar
        try {
            val fusedClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(application)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                location?.let { _currentLocation.value = it }
            }
        } catch (e: SecurityException) {
            // Sin permisos de ubicación
        }
    }
    
    fun sendIncident(category: IncidentCategory, description: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Compress image if present
                var compressedImageData: ByteArray? = null
                val photoUri = _selectedPhotoUri.value
                
                if (photoUri != null) {
                    compressedImageData = ImageUtils.compressImage(
                        getApplication<Application>().applicationContext,
                        photoUri
                    )
                    
                    if (compressedImageData == null) {
                        _uiEvent.send(UiEvent.Error("Error al procesar la imagen"))
                        _isProcessing.value = false
                        return@launch
                    }
                }
                
                // Geolocalización automática del incidente
                val location = _currentLocation.value
                val zoneDescription = location?.let {
                    "Lat: ${String.format("%.6f", it.latitude)}, Lon: ${String.format("%.6f", it.longitude)}"
                }
                
                val incident = IncidentReport(
                    category = category,
                    description = description,
                    beaconId = location?.let { "GPS:${String.format("%.4f", it.latitude)},${String.format("%.4f", it.longitude)}" },
                    zone = zoneDescription,
                    timestamp = System.currentTimeMillis(),
                    photoData = compressedImageData
                )
                
                incidentsRepository.reportIncident(incident)
                _isProcessing.value = false
                _uiEvent.send(UiEvent.Success)
                
            } catch (e: Exception) {
                _isProcessing.value = false
                _uiEvent.send(UiEvent.Error(e.message ?: "Error desconocido"))
            }
        }
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class Error(val message: String = "Error al enviar incidencia") : UiEvent()
    }
}
