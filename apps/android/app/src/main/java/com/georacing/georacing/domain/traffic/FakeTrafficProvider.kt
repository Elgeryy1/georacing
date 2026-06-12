package com.georacing.georacing.domain.traffic

import android.location.Location

/**
 * FASE 3: PLACEHOLDER - Proveedor de tráfico falso para desarrollo.
 * 
 * Este proveedor SIEMPRE devuelve factor 1.0 (tráfico normal, sin ajustes).
 * 
 * TODO FASE 3: Reemplazar con implementación real usando:
 * - Google Maps Traffic API
 * - TomTom Traffic API
 * - HERE Traffic API
 * - OpenStreetMap live traffic data
 * - Datos propios recopilados de usuarios
 * 
 * COMPORTAMIENTO ACTUAL:
 * - getTrafficFactor() → 1.0 (sin cambios al ETA)
 * - isAvailable() → true (siempre disponible)
 * - getTrafficDescription() → "Tráfico normal (simulado)"
 * 
 * TESTING: Para probar diferentes escenarios de tráfico, puedes modificar
 * temporalmente el valor de retorno de getTrafficFactor():
 * 
 * Ejemplos de testing:
 * - return 1.2  // Simular tráfico moderado
 * - return 1.5  // Simular tráfico intenso
 * - return 0.8  // Simular tráfico fluido
 */
class FakeTrafficProvider : TrafficProvider {
    
    /**
     * PLACEHOLDER: Siempre devuelve 1.0 (sin tráfico).
     * 
     * TODO FASE 3: Implementar lógica real conectando con API de tráfico.
     * 
     * Ejemplo de implementación real:
     * ```kotlin
     * override fun getTrafficFactor(location: Location): Double? {
     *     // Llamar a Google Maps Traffic API
     *     val response = trafficApi.getTraffic(location.latitude, location.longitude)
     *     
     *     // Convertir nivel de tráfico a factor
     *     return when (response.trafficLevel) {
     *         "TRAFFIC_FREE" -> 0.9
     *         "TRAFFIC_LIGHT" -> 1.1
     *         "TRAFFIC_MODERATE" -> 1.3
     *         "TRAFFIC_HEAVY" -> 1.6
     *         "TRAFFIC_JAM" -> 2.0
     *         else -> 1.0
     *     }
     * }
     * ```
     */
    override fun getTrafficFactor(location: Location): Double {
        // PLACEHOLDER: Sin tráfico
        return 1.0
        
        // TODO FASE 3: Descomentar para testing de diferentes escenarios
        // return 1.2  // Tráfico moderado
        // return 1.5  // Tráfico intenso
        // return 0.8  // Tráfico fluido
    }
    
    /**
     * PLACEHOLDER: Siempre disponible.
     * 
     * TODO FASE 3: Verificar conectividad con API real.
     */
    override fun isAvailable(): Boolean {
        // PLACEHOLDER: Siempre disponible
        return true
        
        // TODO FASE 3: Verificar API real
        // return networkManager.isConnected() && trafficApi.isHealthy()
    }
    
    /**
     * PLACEHOLDER: Descripción fija.
     * 
     * TODO FASE 3: Delegar en implementación de interfaz base.
     */
    override fun getTrafficDescription(location: Location): String {
        // PLACEHOLDER: Descripción fija
        return "Tráfico normal (simulado)"
        
        // TODO FASE 3: Usar implementación real de la interfaz base
        // return super.getTrafficDescription(location) ?: "Sin datos"
    }
    
    /**
     * FASE 3: No soportado en placeholder.
     * 
     * TODO FASE 3: Implementar consulta por segmentos si la API lo soporta.
     */
    override fun getTrafficFactorForSegment(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double? {
        // PLACEHOLDER: No soportado
        return null
        
        // TODO FASE 3: Consultar tráfico para segmento específico
        // return trafficApi.getSegmentTraffic(startLat, startLon, endLat, endLon)?.toFactor()
    }
}
