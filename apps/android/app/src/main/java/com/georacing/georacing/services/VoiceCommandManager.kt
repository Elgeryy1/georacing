package com.georacing.georacing.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Servicio de comandos de voz para la navegación GeoRacing.
 *
 * Soporta comandos en español:
 * - "Buscar [lugar]" → Busca un POI o destino
 * - "Ir a [destino]" → Navega al destino
 * - "Evitar autopistas" → Recalcula sin autopistas
 * - "Evitar peajes" → Recalcula sin peajes
 * - "Gasolinera" → Busca gasolinera cercana
 * - "Parking" → Busca parking cercano
 * - "Parar navegación" → Detiene la navegación
 * - "Silencio" → Mutea las indicaciones de voz
 * - "Repetir" → Repite la última instrucción
 * - "¿Cuánto falta?" → Anuncia ETA y distancia restante
 */
class VoiceCommandManager(private val context: Context) {

    companion object {
        private const val TAG = "VoiceCommandManager"
    }

    // ── Estado ──

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var callback: VoiceCommandCallback? = null

    // ── Modelos ──

    sealed class VoiceCommand {
        data class Search(val query: String) : VoiceCommand()
        data class NavigateTo(val destination: String) : VoiceCommand()
        data object AvoidHighways : VoiceCommand()
        data object AvoidTolls : VoiceCommand()
        data object FindGasStation : VoiceCommand()
        data object FindParking : VoiceCommand()
        data object StopNavigation : VoiceCommand()
        data object Mute : VoiceCommand()
        data object Unmute : VoiceCommand()
        data object RepeatInstruction : VoiceCommand()
        data object QueryETA : VoiceCommand()
        data object FindRestaurant : VoiceCommand()
        data object FindWC : VoiceCommand()
        data object FindMedical : VoiceCommand()
        data class Unknown(val rawText: String) : VoiceCommand()
    }

    interface VoiceCommandCallback {
        fun onCommand(command: VoiceCommand)
        fun onError(message: String)
    }

    // ── API pública ──

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun setCallback(cb: VoiceCommandCallback) {
        callback = cb
    }

    fun startListening() {
        if (!isAvailable()) {
            Log.w(TAG, "Speech recognition not available")
            _error.value = "Reconocimiento de voz no disponible"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
            _error.value = null
            Log.d(TAG, "🎤 Listening started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _error.value = "Error al iniciar reconocimiento: ${e.message}"
            _isListening.value = false
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
        Log.d(TAG, "🎤 Listening stopped")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isListening.value = false
    }

    // ── Parseo de comandos ──

    private fun parseCommand(text: String): VoiceCommand {
        val lower = text.lowercase().trim()
        Log.d(TAG, "🎤 Parsing: '$lower'")

        return when {
            // Navegación
            lower.startsWith("ir a ") -> VoiceCommand.NavigateTo(lower.removePrefix("ir a ").trim())
            lower.startsWith("navegar a ") -> VoiceCommand.NavigateTo(lower.removePrefix("navegar a ").trim())
            lower.startsWith("llévame a ") -> VoiceCommand.NavigateTo(lower.removePrefix("llévame a ").trim())

            // Búsqueda
            lower.startsWith("buscar ") -> VoiceCommand.Search(lower.removePrefix("buscar ").trim())
            lower.startsWith("busca ") -> VoiceCommand.Search(lower.removePrefix("busca ").trim())
            lower.startsWith("dónde está ") -> VoiceCommand.Search(lower.removePrefix("dónde está ").trim())
            lower.startsWith("donde está ") -> VoiceCommand.Search(lower.removePrefix("donde está ").trim())

            // Evitar
            lower.contains("evitar autopista") || lower.contains("sin autopista") -> VoiceCommand.AvoidHighways
            lower.contains("evitar peaje") || lower.contains("sin peaje") -> VoiceCommand.AvoidTolls

            // POIs rápidos
            lower.contains("gasolinera") || lower.contains("gasolina") || lower.contains("combustible") -> VoiceCommand.FindGasStation
            lower.contains("parking") || lower.contains("aparcar") || lower.contains("aparcamiento") -> VoiceCommand.FindParking
            lower.contains("restaurante") || lower.contains("comida") || lower.contains("comer") -> VoiceCommand.FindRestaurant
            lower.contains("baño") || lower.contains("aseo") || lower.contains("lavabo") -> VoiceCommand.FindWC
            lower.contains("médico") || lower.contains("hospital") || lower.contains("urgencia") -> VoiceCommand.FindMedical

            // Control
            lower.contains("parar navegación") || lower.contains("detener navegación") || lower.contains("cancelar ruta") -> VoiceCommand.StopNavigation
            lower.contains("silencio") || lower.contains("mute") || lower.contains("calla") -> VoiceCommand.Mute
            lower.contains("activar voz") || lower.contains("unmute") || lower.contains("habla") -> VoiceCommand.Unmute
            lower.contains("repetir") || lower.contains("repite") || lower.contains("otra vez") -> VoiceCommand.RepeatInstruction
            lower.contains("cuánto falta") || lower.contains("cuanto falta") || lower.contains("tiempo") && lower.contains("llegar") -> VoiceCommand.QueryETA

            // No reconocido
            else -> VoiceCommand.Unknown(text)
        }
    }

    // ── Recognition Listener ──

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "🎤 Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "🎤 Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            Log.d(TAG, "🎤 Speech ended")
        }

        override fun onError(error: Int) {
            _isListening.value = false
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Sin permisos de micrófono"
                SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de red"
                SpeechRecognizer.ERROR_NO_MATCH -> "No se reconoció el comando"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
                else -> "Error desconocido ($error)"
            }
            Log.w(TAG, "🎤 Error: $errorMsg")
            _error.value = errorMsg
            callback?.onError(errorMsg)
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val bestMatch = matches[0]
                Log.d(TAG, "🎤 Result: '$bestMatch'")
                val command = parseCommand(bestMatch)
                _lastCommand.value = command
                callback?.onCommand(command)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!partial.isNullOrEmpty()) {
                Log.v(TAG, "🎤 Partial: '${partial[0]}'")
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
