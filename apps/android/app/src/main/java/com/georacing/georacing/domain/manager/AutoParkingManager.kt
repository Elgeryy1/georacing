package com.georacing.georacing.domain.manager

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.speech.tts.TextToSpeech
import android.util.Log
import com.georacing.georacing.data.parking.ParkingLocation
import com.georacing.georacing.data.parking.ParkingRepository
import com.georacing.georacing.infrastructure.car.CarTransitionManager
import com.georacing.georacing.utils.TTSManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

/**
 * Gestiona el guardado automático de la ubicación del coche cuando se desconecta de Android Auto.
 * 
 * Funcionalidades:
 * - GPS de alta precisión (PRIORITY_HIGH_ACCURACY)
 * - Retry automático si precisión > 50m
 * - Asignación inteligente de puerta peatonal
 * - Anuncio de voz personalizado con TTS
 */
class AutoParkingManager(
    private val context: Context,
    private val parkingRepository: ParkingRepository,
    private val carTransitionManager: CarTransitionManager
) {

    companion object {
        private const val TAG = "AutoParkingManager"
        private const val MAX_ACCEPTABLE_ACCURACY_METERS = 50f
        private const val RETRY_DELAY_MS = 2000L
        private const val USER_NAME = "Gery" // Hardcoded for now - TODO: Read from user profile
    }

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _showParkingConfirmation = MutableStateFlow<Location?>(null)
    val showParkingConfirmation: StateFlow<Location?> = _showParkingConfirmation.asStateFlow()

    // State for gate assignment result
    private val _lastGateAssignment = MutableStateFlow<GateAssignment?>(null)
    val lastGateAssignment: StateFlow<GateAssignment?> = _lastGateAssignment.asStateFlow()

    // TTS Engine
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        setupTransitionListener()
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))
                isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
                Log.i(TAG, "TTS initialized: ${if (isTtsReady) "OK" else "ERROR"}")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun setupTransitionListener() {
        carTransitionManager.onParkingTransitionDetected = {
             attemptToCaptureLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun attemptToCaptureLocation() {
        // Permission check
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        scope.launch {
            try {
                // 🎯 First attempt: Get fresh high-accuracy location
                var location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

                // 🔍 Check accuracy - if too low precision, retry after delay
                if (location != null && location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
                    Log.d(TAG, "GPS accuracy ${location.accuracy}m > ${MAX_ACCEPTABLE_ACCURACY_METERS}m, waiting for better fix...")
                    
                    delay(RETRY_DELAY_MS)
                    
                    // Second attempt after waiting
                    val betterLocation = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                    
                    // Use the better one
                    if (betterLocation != null && betterLocation.accuracy < location.accuracy) {
                        Log.d(TAG, "Got better accuracy: ${betterLocation.accuracy}m")
                        location = betterLocation
                    }
                }

                if (location != null) {
                    Log.i(TAG, "Captured parking location: ${location.latitude}, ${location.longitude} (accuracy: ${location.accuracy}m)")
                    _showParkingConfirmation.value = location
                } else {
                    // Fallback to last known
                    val lastLocation = fusedLocationClient.lastLocation.await()
                    if (lastLocation != null) {
                        Log.i(TAG, "Using last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                        _showParkingConfirmation.value = lastLocation
                    } else {
                        Log.w(TAG, "No location available")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capturing location", e)
            }
        }
    }

    /**
     * Confirma el parking y ejecuta la lógica de asignación de puerta + TTS.
     * 
     * 1. Guarda la ubicación en el repositorio
     * 2. Calcula la puerta peatonal óptima
     * 3. Anuncia por voz la información al usuario
     */
    fun confirmParking(location: Location, photoUri: String? = null) {
        scope.launch {
            // 1. Guardar ubicación
            val parkingLoc = ParkingLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis(),
                photoUri = photoUri
            )
            parkingRepository.saveParkingLocation(parkingLoc)
            Log.i(TAG, "Parking saved: ${location.latitude}, ${location.longitude}")

            // 2. Calcular puerta óptima
            val gateAssignment = GateAssignmentManager.assignGate(location)
            _lastGateAssignment.value = gateAssignment

            // 3. Anunciar por voz
            if (gateAssignment != null && isTtsReady) {
                val voiceMessage = buildVoiceAnnouncement(gateAssignment)
                TTSManager.speak(voiceMessage, tts, interruptCurrent = true)
                Log.i(TAG, "TTS announcement: $voiceMessage")
            } else if (gateAssignment == null) {
                // Usuario fuera del perímetro del circuito
                TTSManager.speak(
                    "Coche guardado, $USER_NAME. Parece que estás fuera del circuito.",
                    tts, 
                    interruptCurrent = true
                )
            }

            // 4. Cerrar diálogo
            _showParkingConfirmation.value = null
        }
    }

    /**
     * Construye el mensaje de voz personalizado.
     * 
     * Formato: "Aparcado en Parking [X]. Tu puerta óptima es la [GATE Y], [Nombre]."
     */
    private fun buildVoiceAnnouncement(gateAssignment: GateAssignment): String {
        val parkingInfo = if (gateAssignment.parkingName != null) {
            "Aparcado en ${gateAssignment.parkingName}."
        } else {
            "Coche guardado."
        }
        
        return "$parkingInfo " +
               "Tu puerta óptima es la ${gateAssignment.gateName}, $USER_NAME. " +
               "A unos ${gateAssignment.walkingTimeMinutes} minutos caminando."
    }

    fun dismissParkingDialog() {
        _showParkingConfirmation.value = null
    }

    /**
     * Obtiene la última asignación de puerta para mostrarla en la UI.
     */
    fun getLastGateAssignment(): GateAssignment? = _lastGateAssignment.value

    /**
     * Limpia recursos al destruir.
     */
    fun cleanup() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
