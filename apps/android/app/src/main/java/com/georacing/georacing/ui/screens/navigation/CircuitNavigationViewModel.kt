package com.georacing.georacing.ui.screens.navigation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.car.PoiRepository
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.navigation.NavigationEngine
import com.georacing.georacing.navigation.NavigationState
import com.georacing.georacing.utils.TTSManager
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * ViewModel para la pantalla de navegación móvil.
 * 
 * Gestiona:
 * - Motor de navegación compartido (NavigationEngine)
 * - Actualizaciones de ubicación GPS
 * - Gestión de permisos
 * - Text-to-Speech para instrucciones
 * - Estado de UI (mapa, información de navegación)
 */
class CircuitNavigationViewModel(
    application: Application
) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    
    private val TAG = "CircuitNavViewModel"
    
    // Motor de navegación compartido
    private val navigationEngine = NavigationEngine()
    
    // Exposición del estado de navegación
    val navigationState: StateFlow<NavigationState> = navigationEngine.navigationState
    
    // Ubicación GPS
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    // Estado de permisos
    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()
    
    // Text-to-Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    
    // Estado de UI
    private val _isFollowMode = MutableStateFlow(true)
    val isFollowMode: StateFlow<Boolean> = _isFollowMode.asStateFlow()
    
    private val _showArrivalDialog = MutableStateFlow(false)
    val showArrivalDialog: StateFlow<Boolean> = _showArrivalDialog.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // ==========================================
    // WAZE-STYLE DATA FLOWS
    // ==========================================
    
    // Circuit State (Green/Red Flag)
    private val _circuitMode = MutableStateFlow(CircuitMode.GREEN_FLAG)
    val circuitMode: StateFlow<CircuitMode> = _circuitMode.asStateFlow()
    
    // Active Hazards (for pop-up alerts)
    private val _activeHazards = MutableStateFlow<List<ScenarioSimulator.RoadHazard>>(emptyList())
    val activeHazards: StateFlow<List<ScenarioSimulator.RoadHazard>> = _activeHazards.asStateFlow()
    
    // Speed tracking
    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()
    
    private val _speedLimit = MutableStateFlow<Int?>(null)
    val speedLimit: StateFlow<Int?> = _speedLimit.asStateFlow()
    
    // Callback de ubicación
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                handleLocationUpdate(location)
            }
        }
    }
    
    init {
        // Inicializar TTS
        tts = TextToSpeech(application, this)
        
        // Verificar permisos
        checkLocationPermission()
        
        // Observar estado de navegación para detectar llegada
        viewModelScope.launch {
            navigationState.collect { state ->
                when (state) {
                    is NavigationState.Arrived -> {
                        _showArrivalDialog.value = true
                        stopLocationUpdates()
                    }
                    is NavigationState.Error -> {
                        _errorMessage.value = state.message
                    }
                    else -> {
                        // Estados normales, no hacer nada especial
                    }
                }
            }
        }
        
        // Manejar instrucciones de voz
        viewModelScope.launch {
            navigationState.collect { state ->
                if (state is NavigationState.Active && state.currentStep != null) {
                    handleVoiceInstruction(state)
                }
            }
        }
        
        // ==========================================
        // WAZE-STYLE OBSERVERS
        // ==========================================
        
        // Observar estado del circuito desde ScenarioSimulator
        viewModelScope.launch {
            ScenarioSimulator.crowdIntensity.collect { intensity ->
                _circuitMode.value = when {
                    intensity > 0.9f -> CircuitMode.RED_FLAG
                    intensity > 0.7f -> CircuitMode.SAFETY_CAR
                    intensity > 0.5f -> CircuitMode.YELLOW_FLAG
                    else -> CircuitMode.GREEN_FLAG
                }
            }
        }
        
        // Observar hazards activos
        viewModelScope.launch {
            ScenarioSimulator.activeHazards.collect { hazards ->
                _activeHazards.value = hazards
            }
        }
        
        // Observar límite de velocidad (inicialmente simulado)
        viewModelScope.launch {
            ScenarioSimulator.speedLimit.collect { limit ->
                _speedLimit.value = limit.toInt()
            }
        }
    }
    
    /**
     * Verifica si el permiso de ubicación está concedido.
     */
    private fun checkLocationPermission() {
        val context = getApplication<Application>()
        _locationPermissionGranted.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Inicia la navegación hacia un POI del circuito.
     */
    fun startNavigationToPoi(poiId: String) {
        val poi = PoiRepository.getById(poiId)
        if (poi == null) {
            _errorMessage.value = "Punto de interés no encontrado"
            return
        }
        
        startNavigation(
            destination = LatLng(poi.latitude, poi.longitude),
            destinationName = poi.name
        )
    }
    
    /**
     * Inicia la navegación hacia coordenadas específicas.
     */
    fun startNavigation(destination: LatLng, destinationName: String) {
        Log.i(TAG, "Iniciando navegación a $destinationName")
        
        if (!_locationPermissionGranted.value) {
            _errorMessage.value = "Se requiere permiso de ubicación para navegar"
            return
        }
        
        viewModelScope.launch {
            val currentLoc = _currentLocation.value
            val success = navigationEngine.startNavigation(
                destination = destination,
                destinationName = destinationName,
                currentLocation = currentLoc
            )
            
            if (success) {
                startLocationUpdates()
            }
        }
    }
    
    /**
     * Inicia las actualizaciones de ubicación GPS.
     */
    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        if (!_locationPermissionGranted.value) {
            Log.w(TAG, "No se puede iniciar actualizaciones de ubicación sin permiso")
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // Actualización cada segundo
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setMaxUpdateDelayMillis(2000L)
        }.build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            android.os.Looper.getMainLooper()
        )
        
        Log.i(TAG, "Actualizaciones de ubicación iniciadas")
    }
    
    /**
     * Detiene las actualizaciones de ubicación GPS.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.i(TAG, "Actualizaciones de ubicación detenidas")
    }
    
    /**
     * Maneja una nueva actualización de ubicación.
     */
    private fun handleLocationUpdate(location: Location) {
        // Filtrar señales GPS de mala calidad
        if (location.accuracy > 50f) {
            Log.d(TAG, "GPS poco preciso (accuracy=${location.accuracy}m), ignorando")
            return
        }
        
        _currentLocation.value = location
        
        // Actualizar velocidad actual (para velocímetro)
        _currentSpeed.value = if (location.hasSpeed()) {
            location.speed * 3.6f // m/s to km/h
        } else {
            0f
        }
        
        // Actualizar motor de navegación
        viewModelScope.launch {
            navigationEngine.updateLocation(location)
        }
    }
    
    /**
     * Maneja las instrucciones de voz progresivas.
     */
    private fun handleVoiceInstruction(state: NavigationState.Active) {
        if (!isTtsReady || state.currentStep == null) return
        
        val instruction = TTSManager.getInstructionText(
            maneuverType = state.currentStep.maneuver.type,
            modifier = state.currentStep.maneuver.modifier,
            streetName = state.currentStep.name
        )
        
        TTSManager.handleProgressiveTTS(
            instruction = instruction,
            distanceToManeuver = state.distanceToNextManeuver,
            tts = tts,
            forceSpeak = false
        )
    }
    
    /**
     * Callback de inicialización de TTS.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            Log.i(TAG, "TTS inicializado: ${if (isTtsReady) "OK" else "ERROR"}")
        } else {
            Log.e(TAG, "Error al inicializar TTS")
        }
    }
    
    /**
     * Alterna el modo de seguimiento del mapa.
     */
    fun toggleFollowMode() {
        _isFollowMode.value = !_isFollowMode.value
    }
    
    /**
     * Actualiza el estado del permiso de ubicación.
     */
    fun onPermissionResult(granted: Boolean) {
        _locationPermissionGranted.value = granted
        if (granted) {
            // Intentar obtener última ubicación conocida
            requestLastKnownLocation()
        } else {
            _errorMessage.value = "Se requiere permiso de ubicación para usar la navegación"
        }
    }
    
    /**
     * Solicita la última ubicación conocida.
     */
    @Suppress("MissingPermission")
    private fun requestLastKnownLocation() {
        if (!_locationPermissionGranted.value) return
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _currentLocation.value = it
                Log.d(TAG, "Última ubicación conocida: ${it.latitude}, ${it.longitude}")
            }
        }
    }
    
    /**
     * Cierra el diálogo de llegada.
     */
    fun dismissArrivalDialog() {
        _showArrivalDialog.value = false
    }
    
    /**
     * Limpia el mensaje de error.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Detiene la navegación.
     */
    fun stopNavigation() {
        navigationEngine.stopNavigation()
        stopLocationUpdates()
    }
    
    /**
     * Abre Google Maps con el destino actual.
     */
    fun openInGoogleMaps(context: Context) {
        val state = navigationState.value
        if (state is NavigationState.Active) {
            val uri = android.net.Uri.parse(
                "google.navigation:q=${state.destination.latitude},${state.destination.longitude}"
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback a navegación web
                val webUri = android.net.Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=${state.destination.latitude},${state.destination.longitude}"
                )
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, webUri))
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        tts?.stop()
        tts?.shutdown()
    }
}
