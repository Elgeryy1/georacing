package com.georacing.georacing.data.repository

import android.content.Context
import android.util.Log
import com.georacing.georacing.domain.model.AppMode
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.model.RaceSessionInfo
import com.georacing.georacing.domain.repository.CircuitStateRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Offline-first wrapper para CircuitStateRepository.
 *
 * Usa la señal de red como fuente primaria, cachea respuestas válidas y
 * recurre al último estado conocido cuando la red falla o devuelve UNKNOWN.
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

        private val FALLBACK_STATE = CircuitState(
            mode = CircuitMode.UNKNOWN,
            message = "Sin conexion",
            temperature = null,
            updatedAt = ""
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sharedState = networkRepository.getCircuitState()
        .map { state ->
            if (state.mode != CircuitMode.UNKNOWN) {
                cacheState(state)
                Log.d(TAG, "Red OK: estado=${state.mode}, msg=${state.message}")
                state
            } else {
                getCachedState()?.copy(
                    message = buildCachedMessage(getCachedState()?.message)
                ) ?: FALLBACK_STATE
            }
        }
        .catch { e ->
            Log.e(TAG, "Error de red, usando cache: ${e.message}", e)
            emit(
                getCachedState()?.copy(
                    message = buildCachedMessage(getCachedState()?.message)
                ) ?: FALLBACK_STATE
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = getCachedState() ?: FALLBACK_STATE
        )

    override fun getCircuitState(): Flow<CircuitState> = sharedState

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        networkRepository.setCircuitState(mode, message)
    }

    override val appMode: Flow<AppMode> = networkRepository.appMode

    override val debugInfo: Flow<String> = flow {
        val cacheAge = getCacheAgeMs()
        val cacheInfo = if (cacheAge >= 0) {
            "Cache: ${cacheAge / 1000}s ago"
        } else {
            "Cache: vacio"
        }
        emit("OfflineFirst | $cacheInfo")
    }

    private fun buildCachedMessage(originalMessage: String?): String {
        return "Cache local: ${originalMessage ?: "datos guardados"}"
    }

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
            val mode = try {
                CircuitMode.valueOf(modeName)
            } catch (_: Exception) {
                CircuitMode.UNKNOWN
            }
            val message = prefs.getString(KEY_MESSAGE, null)
            val temperature = prefs.getString(KEY_TEMPERATURE, null)
            val updatedAt = prefs.getString(KEY_UPDATED_AT, "") ?: ""
            val sessionJson = prefs.getString(KEY_SESSION_JSON, null)
            val sessionInfo = sessionJson?.let {
                try {
                    gson.fromJson(it, RaceSessionInfo::class.java)
                } catch (_: Exception) {
                    null
                }
            }

            CircuitState(
                mode = mode,
                message = message,
                temperature = temperature,
                updatedAt = updatedAt,
                sessionInfo = sessionInfo
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer cache de estado del circuito", e)
            null
        }
    }

    fun getCacheAgeMs(): Long {
        val cachedAt = prefs.getLong(KEY_CACHED_AT, -1)
        return if (cachedAt > 0) System.currentTimeMillis() - cachedAt else -1
    }

    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cache de estado del circuito limpiado")
    }
}
