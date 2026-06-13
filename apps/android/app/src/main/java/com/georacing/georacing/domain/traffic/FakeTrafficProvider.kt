package com.georacing.georacing.domain.traffic

import android.location.Location

/**
 * Proveedor de tráfico simulado para demo.
 * Devuelve factores de tráfico variables según hora y ubicación para dar realismo.
 */
class FakeTrafficProvider : TrafficProvider {
    
    /**
     * Devuelve un factor de tráfico simulado que varía según la hora del día
     * y la ubicación, dando realismo a las rutas anti-colas.
     */
    override fun getTrafficFactor(location: Location): Double {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        
        // Tráfico alto en horas punta del circuito
        val baseFactor = when (hour) {
            in 9..11 -> 1.3    // Llegada de público
            in 12..13 -> 1.1   // Media mañana
            in 14..16 -> 1.4   // Máxima afluencia
            in 17..19 -> 1.2   // Salida
            else -> 0.9        // Tráfico bajo
        }
        
        // Variación por zona (lat/lon) para que cada ruta sea distinta
        val zoneVariation = ((location.latitude * 100 + location.longitude * 100).toInt() % 4) * 0.05
        return (baseFactor + zoneVariation).coerceIn(0.7, 1.5)
    }
    
    override fun isAvailable(): Boolean = true
    
    override fun getTrafficDescription(location: Location): String {
        val factor = getTrafficFactor(location)
        return when {
            factor >= 1.3 -> "Tráfico denso — Recomendamos ruta alternativa"
            factor >= 1.1 -> "Tráfico moderado"
            else -> "Tráfico fluido"
        }
    }
    
    override fun getTrafficFactorForSegment(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double? {
        val midLat = (startLat + endLat) / 2
        val midLon = (startLon + endLon) / 2
        val midLocation = Location("segment").apply {
            latitude = midLat
            longitude = midLon
        }
        return getTrafficFactor(midLocation)
    }
}

