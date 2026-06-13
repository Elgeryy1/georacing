package com.georacing.georacing.domain.manager

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Detector automático de transición de modo de transporte.
 *
 * Detecta cuándo el usuario:
 * - Sale del coche y empieza a caminar → cambiar a modo peatón
 * - Entra en el coche → cambiar a modo coche
 * - Está quieto (probablemente en grada) → modo espectador
 *
 * Usa Google Activity Recognition Transition API para detecciones fiables.
 * Fallback: análisis de velocidad GPS (< 5 km/h = peatón, > 15 km/h = vehículo).
 */
class TransitionModeDetector(private val context: Context) {

    companion object {
        private const val TAG = "TransitionMode"
        private const val ACTION_TRANSITION =
            "com.georacing.georacing.ACTIVITY_TRANSITION"
        private const val SPEED_WALKING_MAX_KMH = 7.0
        private const val SPEED_VEHICLE_MIN_KMH = 15.0
        private const val TRANSITION_CONFIRM_MS = 10_000L // 10s de confirmación
    }

    // ── Modos ──

    enum class TransportMode {
        VEHICLE,     // En coche
        WALKING,     // Caminando
        STILL,       // Quieto (en grada, esperando)
        CYCLING,     // En bicicleta
        UNKNOWN      // Indeterminado
    }

    data class ModeTransition(
        val from: TransportMode,
        val to: TransportMode,
        val timestamp: Long,
        val confidence: Int // 0-100
    )

    interface TransitionCallback {
        fun onModeChanged(transition: ModeTransition)
    }

    // ── Estado ──

    private val _currentMode = MutableStateFlow(TransportMode.UNKNOWN)
    val currentMode: StateFlow<TransportMode> = _currentMode.asStateFlow()

    private val _transitions = MutableStateFlow<List<ModeTransition>>(emptyList())
    val transitions: StateFlow<List<ModeTransition>> = _transitions.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var activityClient: ActivityRecognitionClient? = null
    private var pendingIntent: PendingIntent? = null
    private var callback: TransitionCallback? = null
    private var lastSpeedBasedMode: TransportMode? = null
    private var speedModeStartTime: Long = 0L

    // ── API Pública ──

    fun setCallback(cb: TransitionCallback) {
        callback = cb
    }

    /**
     * Inicia monitoring con Activity Recognition API.
     * Requiere ACTIVITY_RECOGNITION permission (Android Q+).
     */
    fun startMonitoring(): Boolean {
        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "⚠️ ACTIVITY_RECOGNITION permission not granted")
                return false
            }
        }

        try {
            activityClient = ActivityRecognition.getClient(context)

            // Transiciones que nos interesan
            val transitions = listOf(
                // Entrar en vehículo
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                // Salir del vehículo
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                // Empezar a caminar
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                // Dejar de caminar
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
                // Quedarse quieto
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                // Bicicleta
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            val request = ActivityTransitionRequest(transitions)

            val intent = Intent(ACTION_TRANSITION)
            pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Registrar receiver
            val filter = IntentFilter(ACTION_TRANSITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(transitionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(transitionReceiver, filter)
            }

            activityClient?.requestActivityTransitionUpdates(request, pendingIntent!!)
                ?.addOnSuccessListener {
                    _isMonitoring.value = true
                    Log.i(TAG, "✅ Activity transition monitoring started")
                }
                ?.addOnFailureListener { e ->
                    Log.e(TAG, "❌ Failed to start monitoring: ${e.message}")
                }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting transition monitoring", e)
            return false
        }
    }

    /**
     * Detiene el monitoring.
     */
    fun stopMonitoring() {
        pendingIntent?.let { pi ->
            activityClient?.removeActivityTransitionUpdates(pi)
        }

        try {
            context.unregisterReceiver(transitionReceiver)
        } catch (_: Exception) { }

        _isMonitoring.value = false
        Log.i(TAG, "🛑 Activity transition monitoring stopped")
    }

    /**
     * Fallback: actualizar modo basándose en velocidad GPS.
     * Llamar cada actualización de ubicación.
     */
    fun updateFromGpsSpeed(speedMs: Float) {
        val speedKmh = speedMs * 3.6

        val detectedMode = when {
            speedKmh > SPEED_VEHICLE_MIN_KMH -> TransportMode.VEHICLE
            speedKmh < 1.0 -> TransportMode.STILL
            speedKmh <= SPEED_WALKING_MAX_KMH -> TransportMode.WALKING
            else -> TransportMode.CYCLING // Entre 7-15 km/h puede ser bici
        }

        if (detectedMode != lastSpeedBasedMode) {
            lastSpeedBasedMode = detectedMode
            speedModeStartTime = System.currentTimeMillis()
        } else if (System.currentTimeMillis() - speedModeStartTime > TRANSITION_CONFIRM_MS) {
            // Confirmado por tiempo → aplicar transición
            if (detectedMode != _currentMode.value) {
                applyModeChange(detectedMode, confidence = 60) // Menos confianza que Activity API
            }
        }
    }

    /**
     * Obtiene la descripción del modo actual en español.
     */
    fun getModeDescription(): String {
        return when (_currentMode.value) {
            TransportMode.VEHICLE -> "🚗 En vehículo"
            TransportMode.WALKING -> "🚶 Caminando"
            TransportMode.STILL -> "🧍 Quieto"
            TransportMode.CYCLING -> "🚴 En bicicleta"
            TransportMode.UNKNOWN -> "❓ Detectando..."
        }
    }

    /**
     * Fuerza un modo manualmente (p.ej. desde un botón de la UI).
     */
    fun forceMode(mode: TransportMode) {
        applyModeChange(mode, confidence = 100)
    }

    fun destroy() {
        stopMonitoring()
    }

    // ── Lógica interna ──

    private fun applyModeChange(newMode: TransportMode, confidence: Int) {
        val oldMode = _currentMode.value
        if (oldMode == newMode) return

        val transition = ModeTransition(
            from = oldMode,
            to = newMode,
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )

        _currentMode.value = newMode
        _transitions.value = _transitions.value + transition

        Log.i(TAG, "🔄 Mode: ${oldMode.name} → ${newMode.name} (confidence=$confidence%)")
        callback?.onModeChanged(transition)
    }

    private fun handleActivityTransitionEvent(event: ActivityTransitionEvent) {
        val newMode = when (event.activityType) {
            DetectedActivity.IN_VEHICLE -> {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    TransportMode.VEHICLE
                else TransportMode.WALKING // Salir del coche = probablemente caminando
            }
            DetectedActivity.WALKING -> {
                if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    TransportMode.WALKING
                else TransportMode.STILL
            }
            DetectedActivity.STILL -> TransportMode.STILL
            DetectedActivity.ON_BICYCLE -> TransportMode.CYCLING
            else -> null
        }

        if (newMode != null) {
            applyModeChange(newMode, confidence = 85) // Activity API confianza alta
        }
    }

    // ── Broadcast Receiver ──

    private val transitionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent) ?: return
                for (event in result.transitionEvents) {
                    handleActivityTransitionEvent(event)
                }
            }
        }
    }
}
