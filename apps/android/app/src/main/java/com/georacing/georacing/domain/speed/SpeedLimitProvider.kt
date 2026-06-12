package com.georacing.georacing.domain.speed

import android.location.Location

/**
 * FASE 3.2: Proveedor de límites de velocidad para un tramo.
 * 
 * Esta interfaz permite obtener el límite de velocidad del tramo actual.
 * 
 * Implementaciones:
 * - FakeSpeedLimitProvider: Versión de placeholder que estima según velocidad actual
 * - OSMSpeedLimitProvider (futuro): Obtiene límites desde tags de OpenStreetMap
 * - APISpeedLimitProvider (futuro): Consulta API de tráfico (TomTom, HERE, etc.)
 */
interface SpeedLimitProvider {
    
    /**
     * Obtiene el límite de velocidad en km/h para la ubicación actual.
     * 
     * @param location Ubicación GPS actual
     * @return Límite de velocidad en km/h, o null si no está disponible
     */
    fun getSpeedLimitForLocation(location: Location): Int?
}
