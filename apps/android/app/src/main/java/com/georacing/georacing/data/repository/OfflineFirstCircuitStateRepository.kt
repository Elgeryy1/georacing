package com.georacing.georacing.data.repository

import android.content.Context
import android.util.Log
import com.georacing.georacing.domain.model.AppMode
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.model.DriverInfo
import com.georacing.georacing.domain.model.RaceSessionInfo
import com.georacing.georacing.domain.repository.CircuitStateRepository
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Offline-First wrapper para CircuitStateRepository.
 *
 * # Estrategia:
 * - El estado del circuito se polleja cada 5s (datos volátiles).
 * - Cada respuesta exitosa se cachea con timestamp.
 * - Si la red falla, emite el último estado conocido desde SharedPreferences.
 * - Incluye indicador de frescura para que la UI distinga datos live vs cacheados.
 *
 * # Flujo:
 * ```
 * loop {
 *   [Network API] --OK--> emit(data) + cache en SharedPreferences
 *         |
 *        FAIL --> leer SharedPreferences --> emit(cached con "⚡ Datos en caché")
 *                      |
 *                     VACÍO --> emit(default UNKNOWN state)
 *   delay(5000)
 * }
 * ```
 */
class OfflineFirstCircuitStateRepository(
    private val networkRepository: CircuitStateRepository,
    context: Context
) : CircuitStateRepository {

    companion object {
        private const val TAG = "OfflineFirstCircuitRepo"
        private const val PREFS_NAME = "circuit_state_cache"
        private const val KEY_MODE = "mode"
        private const val KEY_MESSAGE = "message"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_SESSION_JSON = "session_json"
        private const val KEY_CACHED_AT = "cached_at"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun getCircuitState(): Flow<CircuitState> = flow {
        while (true) {
            try {
                // Intentar obtener de la red (un solo valor del polling flow)
                val state = networkRepository.getCircuitState().firstOrNull()
                if (state != null && state.mode != CircuitMode.UNKNOWN) {
                    // Red OK → cachear y emitir
                    cacheState(state)
                    Log.d(TAG, "Red OK: estado=${state.mode}, msg=${state.message}")
                    emit(state)
                } else {
                    // Red devolvió UNKNOWN → usar caché
                    val cached = getCachedState()
                    if (cached != null) {
                        Log.d(TAG, "Red UNKNOWN, usando caché: ${cached.mode}")
                        emit(cached.copy(message = "⚡ ${cached.message ?: "Datos en caché"}"))
                    } else {
                        emit(state ?: CircuitState(CircuitMode.UNKNOWN, "Sin Conexión", null, ""))
                    }
                }
            } catch (e: Exception) {
                // Red falló → fallback a caché
                Log.e(TAG, "Error de red, usando caché: ${e.message}")
                val cached = getCachedState()
                if (cached != null) {
                    Log.d(TAG, "Fallback caché: ${cached.mode}")
                    emit(cached.copy(message = "⚡ ${cached.message ?: "Datos en caché"}"))
                } else {
                    Log.w(TAG, "Sin caché, emitiendo estado por defecto")
                    emit(CircuitState(CircuitMode.UNKNOWN, "Sin Conexión", null, ""))
                }
            }
            delay(5000)
        }
    }

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        // Delegar al repo de red
        networkRepository.setCircuitState(mode, message)
    }

    override val appMode: Flow<AppMode> = networkRepository.appMode

    override val debugInfo: Flow<String> = flow {
        val cacheAge = getCacheAgeMs()
        val cacheInfo = if (cacheAge >= 0) {
            "Cache: ${cacheAge / 1000}s ago"
        } else {
            "Cache: vacío"
        }
        emit("OfflineFirst | $cacheInfo")
    }

    // =========================================================================
    // Cache (SharedPreferences)
    // =========================================================================

    private fun cacheState(state: CircuitState) {
        try {
            prefs.edit()
                .putString(KEY_MODE, state.mode.name)
                .putString(KEY_MESSAGE, state.message)
                .putString(KEY_TEMPERATURE, state.temperature)
                .putString(KEY_UPDATED_AT, state.updatedAt)
                .putString(KEY_SESSION_JSON, state.sessionInfo?.let { gson.toJson(it) })
                .putLong(KEY_CACHED_AT, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cachear estado del circuito", e)
        }
    }

    private fun getCachedState(): CircuitState? {
        return try {
            val modeName = prefs.getString(KEY_MODE, null) ?: return null
            val mode = try { CircuitMode.valueOf(modeName) } catch (_: Exception) { CircuitMode.UNKNOWN }
            val message = prefs.getString(KEY_MESSAGE, null)
            val temperature = prefs.getString(KEY_TEMPERATURE, null)
            val updatedAt = prefs.getString(KEY_UPDATED_AT, "") ?: ""
            val sessionJson = prefs.getString(KEY_SESSION_JSON, null)
            val sessionInfo = sessionJson?.let {
                try { gson.fromJson(it, RaceSessionInfo::class.java) } catch (_: Exception) { null }
            }

            CircuitState(
                mode = mode,
                message = message,
                temperature = temperature,
                updatedAt = updatedAt,
                sessionInfo = sessionInfo
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer caché de estado del circuito", e)
            null
        }
    }

    /**
     * Edad del caché en milisegundos. -1 si no hay caché.
     */
    fun getCacheAgeMs(): Long {
        val cachedAt = prefs.getLong(KEY_CACHED_AT, -1)
        return if (cachedAt > 0) System.currentTimeMillis() - cachedAt else -1
    }

    /**
     * Limpia el caché local del estado del circuito.
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Caché de estado del circuito limpiado")
    }
}
