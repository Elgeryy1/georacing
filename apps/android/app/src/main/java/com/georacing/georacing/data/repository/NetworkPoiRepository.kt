package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.toDomain
import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType
import com.georacing.georacing.domain.repository.PoiRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @deprecated Use OfflineFirstPoiRepository instead for offline support.
 * This class is kept for backward compatibility during migration.
 */
@Deprecated("Use OfflineFirstPoiRepository for offline-first architecture")
class NetworkPoiRepository : PoiRepository {
    
    // Hardcoded POIs (previously in car/PoiRepository)
    private val hardcodedPois = listOf(
        Poi("main_gate", "Acceso Principal", PoiType.GATE, "Entrada Principal Circuit", "Accesos", 0f, 0f, 41.56872, 2.25668),
        Poi("gate_3", "Porta 3", PoiType.GATE, "Acceso Este", "Accesos", 0f, 0f, 41.56840, 2.25870),
        Poi("parking_a", "Parking A", PoiType.PARKING, "Parking Zona Norte-Oeste", "Parking", 0f, 0f, 41.57063, 2.25796),
        Poi("parking_b", "Parking B", PoiType.PARKING, "Parking Zona Norte", "Parking", 0f, 0f, 41.57133, 2.25574),
        Poi("parking_c", "Parking C", PoiType.PARKING, "Parking Zona Este", "Parking", 0f, 0f, 41.56895, 2.26235),
        Poi("parking_d", "Parking D", PoiType.PARKING, "Parking Zona Sur-Este", "Parking", 0f, 0f, 41.56552, 2.26330),
        Poi("parking_e", "Parking E", PoiType.PARKING, "Parking Zona Sur", "Parking", 0f, 0f, 41.56480, 2.26053),
        Poi("parking_f", "Parking F", PoiType.PARKING, "Parking Zona Sur-Oeste", "Parking", 0f, 0f, 41.56472, 2.25832)
    )

    override fun getPois(): Flow<List<Poi>> = flow {
        // Emit hardcoded first (fast)
        emit(hardcodedPois)
        
        try {
            val networkPois = RetrofitClient.api.getPois().map { it.toDomain() }
            // Merge: Prefer network if IDs collide, or just append
            // For simplicitly, just emitting network ones + hardcoded
            emit(hardcodedPois + networkPois)
        } catch (e: Exception) {
            Log.e("NetworkPoiRepo", "Error fetching POIs: ${e.message}")
            // Keep emitting hardcoded
            emit(hardcodedPois)
        }
    }

    /**
     * No-op: Esta implementación legacy no cachea datos.
     * Migrar a OfflineFirstPoiRepository para soporte offline.
     */
    override suspend fun refreshPois() {
        // No-op - Esta implementación no usa Room
        Log.w("NetworkPoiRepo", "refreshPois() no tiene efecto en NetworkPoiRepository. Usa OfflineFirstPoiRepository.")
    }
}
