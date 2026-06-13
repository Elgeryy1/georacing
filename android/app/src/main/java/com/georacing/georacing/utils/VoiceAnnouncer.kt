package com.georacing.georacing.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.georacing.georacing.debug.ScenarioSimulator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * VoiceAnnouncer: Observa eventos del ScenarioSimulator y los anuncia por TTS.
 *
 * Diseñado para accesibilidad y demos inmersivas.
 * Utiliza TTSManager.speak() que ya incluye cooldown de 10s para evitar repeticiones.
 */
class VoiceAnnouncer(context: Context) {

    private val TAG = "VoiceAnnouncer"
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    // Debounce flags para evitar repeticiones al cambiar rápidamente
    private var lastNetworkDead = false
    private var lastCrowdHigh = false
    private var lastAtGate = false
    private var lastSurvivalMode = false
    
    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("es", "ES")
                isTtsReady = true
                Log.d(TAG, "TTS initialized successfully")
                startObserving()
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }
    
    private fun startObserving() {
        // 1. Observar fallo de red
        scope.launch {
            ScenarioSimulator.isNetworkDead
                .collectLatest { isDead ->
                    if (isDead && !lastNetworkDead) {
                        announce("Atención. Fallo crítico de red. Activando protocolo de supervivencia offline.")
                    }
                    lastNetworkDead = isDead
                }
        }
        
        // 2. Observar intensidad de multitud
        scope.launch {
            ScenarioSimulator.crowdIntensity
                .collectLatest { intensity ->
                    val isHigh = intensity > 0.5f
                    if (isHigh && !lastCrowdHigh) {
                        announce("Alerta de flujo. Alta densidad detectada en acceso principal. Sugiriendo ruta alternativa.")
                    }
                    lastCrowdHigh = isHigh
                }
        }
        
        // 3. Observar llegada a puerta
        scope.launch {
            ScenarioSimulator.isAtGate
                .collectLatest { atGate ->
                    if (atGate && !lastAtGate) {
                        announce("Llegada a puerta detectada. Desplegando entrada inteligente.")
                    }
                    lastAtGate = atGate
                }
        }
        
        // 4. Observar batería crítica (Survival Mode = battery <= 20%)
        scope.launch {
            ScenarioSimulator.forcedBatteryLevel
                .collectLatest { batteryLevel ->
                    val isSurvival = batteryLevel != null && batteryLevel <= 20
                    if (isSurvival && !lastSurvivalMode) {
                        announce("Batería crítica. Optimizando sistemas para garantizar el retorno.")
                    }
                    lastSurvivalMode = isSurvival
                }
        }
        
        Log.d(TAG, "Started observing ScenarioSimulator states")
    }
    
    private fun announce(message: String) {
        if (isTtsReady) {
            Log.d(TAG, "Announcing: $message")
            TTSManager.speak(message, tts, interruptCurrent = true)
        } else {
            Log.w(TAG, "TTS not ready, skipping: $message")
        }
    }
    
    /**
     * Liberar recursos TTS cuando ya no se necesite.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false
        Log.d(TAG, "VoiceAnnouncer shutdown")
    }
}
