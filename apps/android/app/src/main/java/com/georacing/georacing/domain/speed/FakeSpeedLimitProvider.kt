package com.georacing.georacing.domain.speed

import android.location.Location
import android.util.Log

/**
 * FASE 3.2: Implementación PLACEHOLDER de SpeedLimitProvider.
 * 
 * IMPORTANTE: Esta es una implementación temporal que estima el límite de velocidad
 * según la velocidad actual del vehículo. NO usa datos reales de límites de vía.
 * 
 * Estrategia de estimación:
 * - Velocidad < 40 km/h → Límite 30 km/h (zona urbana, parking)
 * - Velocidad 40-60 km/h → Límite 50 km/h (ciudad)
 * - Velocidad 60-90 km/h → Límite 90 km/h (carretera secundaria)
 * - Velocidad 90-110 km/h → Límite 100 km/h (autovía)
 * - Velocidad > 110 km/h → Límite 120 km/h (autopista)
 * 
 * TODO: Reemplazar con implementación real que use:
 * - Tags de OpenStreetMap (maxspeed)
 * - API de tráfico (TomTom Traffic API, HERE Maps, etc.)
 * - Base de datos local de límites por segmento
 */
class FakeSpeedLimitProvider : SpeedLimitProvider {
    
    companion object {
        private const val TAG = "FakeSpeedLimitProvider"
    }
    
    override fun getSpeedLimitForLocation(location: Location): Int? {
        if (!location.hasSpeed()) {
            Log.v(TAG, "Location has no speed, returning null")
            return null
        }
        
        val speedKmh = location.speed * 3.6f  // m/s a km/h
        
        // Estimar límite según velocidad actual
        val estimatedLimit = when {
            speedKmh < 40f -> 30   // Zona urbana/parking
            speedKmh < 60f -> 50   // Ciudad
            speedKmh < 90f -> 90   // Carretera secundaria
            speedKmh < 110f -> 100 // Autovía
            else -> 120            // Autopista
        }
        
        Log.v(TAG, "PLACEHOLDER: Speed=${speedKmh.toInt()}km/h → Estimated limit=${estimatedLimit}km/h")
        
        return estimatedLimit
    }
}
