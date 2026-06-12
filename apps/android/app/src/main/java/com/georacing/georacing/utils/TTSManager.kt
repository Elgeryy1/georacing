package com.georacing.georacing.utils

import android.speech.tts.TextToSpeech
import android.util.Log

/**
 * Gestor de instrucciones de voz progresivas estilo Google Maps.
 * 
 * Comportamiento:
 * - 500m antes: "En 500 metros, gira a la derecha"
 * - 200m antes: "En 200 metros, gira a la derecha"
 * - 50m antes: "En 50 metros, gira a la derecha"
 * - 0m: "Gira a la derecha ahora"
 * 
 * Características:
 * - No repite la misma instrucción en el mismo umbral
 * - Solo habla cuando cambia de umbral o cambia la instrucción
 * - Cancela instrucciones anteriores si hay una nueva urgente
 */
object TTSManager {
    
    private const val TAG = "TTSManager"
    
    /**
     * Umbrales de distancia para instrucciones progresivas (en metros)
     */
    private const val THRESHOLD_FAR = 500.0
    private const val THRESHOLD_MEDIUM = 200.0
    private const val THRESHOLD_NEAR = 50.0
    private const val THRESHOLD_NOW = 0.0
    
    /**
     * FASE 1.3: Flags anti-saltos de umbral
     * Previenen que se pierdan mensajes si el usuario pasa de 600m a 150m en 1 tick
     * (por ejemplo en túnel GPS o alta velocidad)
     */
    private var spoken500 = false
    private var spoken200 = false
    private var spoken50 = false
    private var spokenNow = false
    
    /**
     * Cooldown para evitar TTS repetitivo cuando el usuario está quieto.
     * Mínimo 10 segundos entre mensajes con el mismo contenido.
     */
    private var lastSpokenMessage: String? = null
    private var lastSpokenTime: Long = 0L
    private const val TTS_COOLDOWN_MS = 10000L  // 10 segundos
    
    /**
     * Estado interno para evitar repeticiones (LEGACY - ahora usamos flags)
     */
    @Deprecated("Use spoken500/200/50 flags instead")
    private var lastSpokenThreshold = -1
    private var lastInstruction: String? = null
    
    /**
     * Maneja las instrucciones de voz progresivas.
     * 
     * FASE 2.4: Refinamiento de lógica anti-saltos.
     * - Solo dispara el umbral MÁS CERCANO aplicable en cada momento.
     * - No fuerza umbrales innecesarios cuando ya pasaste de largo.
     * - Ejemplo: 600m→150m solo dice "200m", no insiste con "500m".
     * 
     * @param instruction Texto base de la instrucción (ej: "gira a la derecha")
     * @param distanceToManeuver Distancia en metros hasta la maniobra
     * @param tts Instancia de TextToSpeech configurada
     * @param forceSpeak Forzar hablar incluso si ya se dijo en este umbral
     */
    fun handleProgressiveTTS(
        instruction: String,
        distanceToManeuver: Double,
        tts: TextToSpeech?,
        forceSpeak: Boolean = false
    ) {
        if (tts == null) {
            Log.w(TAG, "TTS not initialized")
            return
        }
        
        // FASE 2.4: Resetear flags si cambia la instrucción
        if (instruction != lastInstruction) {
            spoken500 = false
            spoken200 = false
            spoken50 = false
            spokenNow = false
            lastInstruction = instruction
            Log.d(TAG, "FASE 2.4 - Nueva instrucción: '$instruction', flags reseteados")
        }
        
        // FASE 2.4: Lógica refinada - solo dispara el umbral MÁS CERCANO aplicable
        // Se evalúa de CERCA a LEJOS para priorizar urgencia
        var phrase: String? = null
        
        when {
            // Umbral INMEDIATO (≤10m)
            distanceToManeuver <= 10 && !spokenNow -> {
                phrase = "$instruction ahora"
                spokenNow = true
                Log.d(TAG, "FASE 2.4 - Umbral NOW (≤10m) alcanzado")
            }
            
            // Umbral CERCA (≤50m)
            distanceToManeuver <= THRESHOLD_NEAR && !spoken50 -> {
                phrase = "En 50 metros, $instruction"
                spoken50 = true
                spokenNow = true  // Marcar también "now" para evitar repetir
                Log.d(TAG, "FASE 2.4 - Umbral 50m alcanzado (dist=${distanceToManeuver.toInt()}m)")
            }
            
            // Umbral MEDIO (≤200m)
            distanceToManeuver <= THRESHOLD_MEDIUM && !spoken200 -> {
                phrase = "En 200 metros, $instruction"
                spoken200 = true
                spoken50 = true    // Marcar umbrales más cercanos
                spokenNow = true
                Log.d(TAG, "FASE 2.4 - Umbral 200m alcanzado (dist=${distanceToManeuver.toInt()}m)")
            }
            
            // Umbral LEJOS (≤500m)
            distanceToManeuver <= THRESHOLD_FAR && !spoken500 -> {
                phrase = "En 500 metros, $instruction"
                spoken500 = true
                spoken200 = true   // Marcar todos los umbrales más cercanos
                spoken50 = true
                spokenNow = true
                Log.d(TAG, "FASE 2.4 - Umbral 500m alcanzado (dist=${distanceToManeuver.toInt()}m)")
            }
        }
        
        // Hablar solo si hay frase nueva o se fuerza
        if (phrase != null || forceSpeak) {
            val finalPhrase = phrase ?: buildPhrase(instruction, 0)
            Log.d(TAG, "Speaking: $finalPhrase (distance: ${distanceToManeuver.toInt()}m)")
            tts.speak(finalPhrase, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            // Log de debug: por qué no se habló
            Log.v(TAG, "No speech: dist=${distanceToManeuver.toInt()}m, flags=[500:$spoken500, 200:$spoken200, 50:$spoken50, now:$spokenNow]")
        }
    }
    
    /**
     * Construye la frase según el umbral de distancia.
     */
    private fun buildPhrase(baseInstruction: String, threshold: Int): String {
        return when (threshold) {
            THRESHOLD_FAR.toInt() -> "En 500 metros, $baseInstruction"
            THRESHOLD_MEDIUM.toInt() -> "En 200 metros, $baseInstruction"
            THRESHOLD_NEAR.toInt() -> "En 50 metros, $baseInstruction"
            else -> "$baseInstruction ahora"
        }
    }
    
    /**
     * Habla un mensaje único (no progresivo).
     * Útil para anuncios como "Ruta recalculada", "Has llegado", etc.
     * 
     * Incluye cooldown de 10 segundos para evitar repeticiones molestas.
     */
    fun speak(
        message: String,
        tts: TextToSpeech?,
        interruptCurrent: Boolean = true
    ) {
        if (tts == null) {
            Log.w(TAG, "TTS not initialized")
            return
        }
        
        // 🛡️ Cooldown: No repetir el mismo mensaje en 10 segundos
        val now = System.currentTimeMillis()
        if (message == lastSpokenMessage && (now - lastSpokenTime) < TTS_COOLDOWN_MS) {
            Log.d(TAG, "TTS cooldown active, skipping: $message")
            return
        }
        
        val queueMode = if (interruptCurrent) {
            TextToSpeech.QUEUE_FLUSH
        } else {
            TextToSpeech.QUEUE_ADD
        }
        
        Log.d(TAG, "Speaking: $message")
        tts.speak(message, queueMode, null, null)
        
        // Guardar para cooldown
        lastSpokenMessage = message
        lastSpokenTime = now
    }
    
    /**
     * Resetea el estado interno.
     * Llamar cuando:
     * - Se inicia una nueva navegación
     * - Se recalcula la ruta
     * - Se avanza al siguiente paso
     */
    fun reset() {
        // FASE 1.3: Resetear flags nuevos
        spoken500 = false
        spoken200 = false
        spoken50 = false
        spokenNow = false
        lastInstruction = null
        lastSpokenThreshold = -1  // Legacy
        // Resetear cooldown
        lastSpokenMessage = null
        lastSpokenTime = 0L
        Log.d(TAG, "TTS state reset (flags + cooldown cleared)")
    }
    
    /**
     * Cancela cualquier instrucción en cola.
     */
    fun stopSpeaking(tts: TextToSpeech?) {
        tts?.stop()
        Log.d(TAG, "TTS stopped")
    }
    
    /**
     * Traduce el tipo de maniobra a texto en español.
     * 
     * @param maneuverType Tipo de OSRM (turn, roundabout, etc.)
     * @param modifier Modificador (left, right, sharp, slight, etc.)
     * @param streetName Nombre de la calle/vial (opcional)
     */
    fun getInstructionText(
        maneuverType: String,
        modifier: String?,
        streetName: String?
    ): String {
        val direction = when (modifier) {
            "left" -> "a la izquierda"
            "right" -> "a la derecha"
            "sharp left" -> "bruscamente a la izquierda"
            "sharp right" -> "bruscamente a la derecha"
            "slight left" -> "ligeramente a la izquierda"
            "slight right" -> "ligeramente a la derecha"
            else -> ""
        }
        
        val action = when (maneuverType) {
            "turn" -> "gira $direction"
            "depart" -> "sal $direction"
            "arrive" -> "has llegado a tu destino"
            "merge" -> "incorpórate $direction"
            "on ramp" -> "toma la rampa $direction"
            "off ramp" -> "sal por la rampa $direction"
            "fork" -> "toma el desvío $direction"
            "end of road" -> "al final de la vía, gira $direction"
            "continue" -> "continúa"
            "roundabout" -> "en la rotonda, toma la salida $direction"
            "rotary" -> "en la rotonda, toma la salida $direction"
            else -> "continúa $direction"
        }
        
        return if (!streetName.isNullOrBlank() && maneuverType != "arrive") {
            "$action hacia $streetName"
        } else {
            action
        }
    }
    
    /**
     * Helper para anunciar llegada al destino.
     */
    fun announceArrival(destinationName: String, tts: TextToSpeech?) {
        speak("Has llegado a $destinationName", tts, interruptCurrent = true)
    }
    
    /**
     * Helper para anunciar recalculo de ruta.
     */
    fun announceRouteRecalculation(tts: TextToSpeech?) {
        speak("Recalculando ruta", tts, interruptCurrent = true)
    }
    
    /**
     * Helper para anunciar ruta calculada con información inicial.
     */
    fun announceRouteCalculated(
        distance: Double,
        duration: Double,
        firstInstruction: String,
        tts: TextToSpeech?
    ) {
        val distanceKm = (distance / 1000).toInt()
        val durationMin = (duration / 60).toInt()
        
        val message = "Ruta calculada: $distanceKm kilómetros, $durationMin minutos. $firstInstruction"
        speak(message, tts, interruptCurrent = true)
    }
}
