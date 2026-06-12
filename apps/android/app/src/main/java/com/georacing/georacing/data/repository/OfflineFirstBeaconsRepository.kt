package com.georacing.georacing.data.repository

import android.content.Context
import android.util.Log
import com.georacing.georacing.domain.model.ArrowDirection
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.repository.BeaconsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Offline-First wrapper para BeaconsRepository.
 *
 * # Estrategia:
 * - Los beacons son datos estáticos que raramente cambian.
 * - Se cachean como JSON en SharedPreferences.
 * - Network-first: intenta la red, cachea el resultado, y ante fallo usa caché.
 *
 * # Flujo:
 * ```
 * [Network API] --OK--> emit(data) + cache en SharedPreferences
 *       |
 *      FAIL --> leer SharedPreferences --> emit(cached)
 *                    |
 *                   VACÍO --> emit(emptyList)
 * ```
 */
class OfflineFirstBeaconsRepository(
    private val networkRepository: BeaconsRepository,
    context: Context
) : BeaconsRepository {

    companion object {
        private const val TAG = "OfflineFirstBeaconsRepo"
        private const val PREFS_NAME = "beacons_cache"
        private const val KEY_BEACONS_JSON = "beacons_json"
        private const val KEY_CACHED_AT = "cached_at"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun getBeacons(): Flow<List<BeaconConfig>> = flow {
        try {
            // 1. Intentar obtener de la red (collect el primer valor del flow de red)
            var networkBeacons: List<BeaconConfig>? = null
            networkRepository.getBeacons().collect { beacons ->
                networkBeacons = beacons
                return@collect // Solo necesitamos el primer emit
            }

            val beacons = networkBeacons ?: emptyList()

            if (beacons.isNotEmpty()) {
                // 2. Cachear resultado exitoso
                cacheBeacons(beacons)
                Log.d(TAG, "Red OK: ${beacons.size} beacons obtenidos y cacheados")
                emit(beacons)
            } else {
                // Red devolvió vacío → intentar caché
                val cached = getCachedBeacons()
                if (cached.isNotEmpty()) {
                    Log.d(TAG, "Red vacía, usando ${cached.size} beacons cacheados")
                }
                emit(cached)
            }
        } catch (e: Exception) {
            // 3. Red falló → fallback a caché
            Log.e(TAG, "Error de red, usando caché: ${e.message}", e)
            val cached = getCachedBeacons()
            if (cached.isNotEmpty()) {
                Log.d(TAG, "Fallback: ${cached.size} beacons desde caché")
            } else {
                Log.w(TAG, "Sin caché disponible, emitiendo lista vacía")
            }
            emit(cached)
        }
    }

    // =========================================================================
    // Cache (SharedPreferences + Gson)
    // =========================================================================

    private fun cacheBeacons(beacons: List<BeaconConfig>) {
        try {
            val json = gson.toJson(beacons)
            prefs.edit()
                .putString(KEY_BEACONS_JSON, json)
                .putLong(KEY_CACHED_AT, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cachear beacons", e)
        }
    }

    private fun getCachedBeacons(): List<BeaconConfig> {
        return try {
            val json = prefs.getString(KEY_BEACONS_JSON, null) ?: return emptyList()
            val type = object : TypeToken<List<BeaconConfig>>() {}.type
            gson.fromJson<List<BeaconConfig>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer caché de beacons", e)
            emptyList()
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
     * Limpia el caché local de beacons.
     */
    fun clearCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Caché de beacons limpiado")
    }
}
