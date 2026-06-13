package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.local.dao.PoiDao
import com.georacing.georacing.data.local.entities.PoiEntity
import com.georacing.georacing.data.local.mappers.toDomain
import com.georacing.georacing.data.local.mappers.toEntities
import com.georacing.georacing.data.remote.GeoRacingApi
import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType
import com.georacing.georacing.domain.repository.PoiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Implementación Offline-First del PoiRepository.
 * 
 * # Patrón Single Source of Truth (SSOT):
 * - La UI observa SOLO la base de datos local (Room).
 * - Los datos de red se guardan en la DB, lo que dispara actualizaciones automáticas a la UI.
 * - Si la red falla, la UI sigue mostrando datos cacheados sin crashear.
 * 
 * # Flujo de Datos:
 * ```
 * [Network API] --> [Room Database] --> [Flow<List<Poi>>] --> [UI]
 *       |                   ^
 *       +-------------------+
 *        refreshPois() guarda aquí
 * ```
 */
class OfflineFirstPoiRepository(
    private val api: GeoRacingApi,
    private val dao: PoiDao
) : PoiRepository {

    companion object {
        private const val TAG = "OfflineFirstPoiRepo"
    }

    // =========================================================================
    // POIs Hardcodeados (Fallback inicial / datos esenciales)
    // =========================================================================
    
    private val hardcodedPois = listOf(
        PoiEntity("main_gate", "Acceso Principal", "GATE", "Entrada Principal Circuit", "Accesos", 0f, 0f, 41.56872, 2.25668),
        PoiEntity("gate_3", "Porta 3", "GATE", "Acceso Este", "Accesos", 0f, 0f, 41.56840, 2.25870),
        PoiEntity("parking_a", "Parking A", "PARKING", "Parking Zona Norte-Oeste", "Parking", 0f, 0f, 41.57063, 2.25796),
        PoiEntity("parking_b", "Parking B", "PARKING", "Parking Zona Norte", "Parking", 0f, 0f, 41.57133, 2.25574),
        PoiEntity("parking_c", "Parking C", "PARKING", "Parking Zona Este", "Parking", 0f, 0f, 41.56895, 2.26235),
        PoiEntity("parking_d", "Parking D", "PARKING", "Parking Zona Sur-Este", "Parking", 0f, 0f, 41.56552, 2.26330),
        PoiEntity("parking_e", "Parking E", "PARKING", "Parking Zona Sur", "Parking", 0f, 0f, 41.56480, 2.26053),
        PoiEntity("parking_f", "Parking F", "PARKING", "Parking Zona Sur-Oeste", "Parking", 0f, 0f, 41.56472, 2.25832)
    )

    // =========================================================================
    // EXPOSED DATA (UI observa esto)
    // =========================================================================

    /**
     * Observa los POIs desde la base de datos local.
     * 
     * - Convierte Entity -> Domain usando mappers.
     * - Emite automáticamente cuando la DB cambia.
     * - onStart: Si la DB está vacía, inserta los POIs hardcodeados.
     */
    override fun getPois(): Flow<List<Poi>> {
        return dao.getAllPois()
            .onStart { 
                // Seed inicial: si no hay datos, insertar hardcoded
                if (dao.count() == 0) {
                    Log.d(TAG, "DB vacía, insertando ${hardcodedPois.size} POIs hardcodeados")
                    dao.insertPois(hardcodedPois)
                }
            }
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    // =========================================================================
    // SYNC (Llamar desde ViewModel, WorkManager, Pull-to-Refresh)
    // =========================================================================

    /**
     * Sincroniza los POIs desde la API hacia la base de datos local.
     * 
     * - Si la red falla, loguea el error pero NO lanza excepción.
     * - Los datos viejos (caché) permanecen disponibles.
     * - Al insertar en la DB, el Flow de getPois() emite automáticamente.
     */
    override suspend fun refreshPois() {
        try {
            Log.d(TAG, "Iniciando sync de POIs desde la red...")
            
            // 1. Fetch desde la API
            val networkPois = api.getPois()
            Log.d(TAG, "Recibidos ${networkPois.size} POIs de la API")
            
            // 2. Convertir DTO -> Entity
            val entities = networkPois.toEntities()
            
            // 3. Merge con hardcoded (opcional: puedes decidir si limpiar primero)
            val allEntities = entities + hardcodedPois
            
            // 4. Insertar en la DB (REPLACE strategy actualiza si ID existe)
            dao.insertPois(allEntities)
            Log.d(TAG, "Sync completado: ${allEntities.size} POIs en caché")
            
        } catch (e: Exception) {
            // ⚠️ NO lanzar excepción - la UI sigue con datos cacheados
            Log.e(TAG, "Error al sincronizar POIs: ${e.message}", e)
            // Opcionalmente, podrías emitir un evento a un SharedFlow para mostrar un Snackbar
        }
    }

    /**
     * Fuerza una recarga completa (limpia caché + sync).
     */
    suspend fun forceRefresh() {
        dao.deleteAll()
        refreshPois()
    }
}
