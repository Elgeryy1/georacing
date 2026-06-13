package com.georacing.georacing.domain.repository

import com.georacing.georacing.domain.model.Poi
import kotlinx.coroutines.flow.Flow

interface PoiRepository {
    /**
     * Observa los POIs desde la fuente de datos.
     * En implementación Offline-First, esto observa la DB local.
     */
    fun getPois(): Flow<List<Poi>>

    /**
     * Sincroniza los POIs desde la red hacia la cache local.
     * Diseñado para ser llamado en background (WorkManager, pull-to-refresh, etc.)
     */
    suspend fun refreshPois()
}
