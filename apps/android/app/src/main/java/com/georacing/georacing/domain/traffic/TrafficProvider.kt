package com.georacing.georacing.domain.traffic

import android.location.Location

/**
 * FASE 3: Interfaz para proveedores de datos de tráfico en tiempo real.
 * 
 * Esta interfaz define el contrato para obtener información de tráfico
 * que se usará para ajustar el ETA durante la navegación.
 * 
 * IMPLEMENTACIONES POSIBLES:
 * - Google Maps Traffic API (requiere API key de pago)
 * - TomTom Traffic API
 * - HERE Traffic API
 * - OpenStreetMap Traffic (limitado)
 * - Datos propios de sensores/GPS
 * 
 * FASE 3: Actualmente solo existe FakeTrafficProvider (placeholder).
 */
interface TrafficProvider {
    
    /**
     * Obtiene el factor de tráfico global para la ubicación actual.
     * 
     * El factor indica qué tan lento/rápido está el tráfico:
     * - 1.0 = tráfico normal (sin cambios al ETA)
     * - 1.2 = tráfico moderado (+20% más lento)
     * - 1.5 = tráfico intenso (+50% más lento)
     * - 2.0 = tráfico muy intenso (el doble de tiempo)
     * - 0.8 = tráfico fluido (-20% más rápido, poco común)
     * 
     * IMPORTANTE: Los valores extremos (<0.5 o >3.0) serán limitados
     * automáticamente por ETACalculator para evitar errores.
     * 
     * @param location Ubicación actual del vehículo
     * @return Factor de tráfico (1.0 = normal). Null si no hay datos disponibles.
     */
    fun getTrafficFactor(location: Location): Double?
    
    /**
     * Obtiene el factor de tráfico para un segmento específico de ruta.
     * 
     * FASE 3: Este método es AVANZADO y permite factores diferentes
     * por segmento de ruta (más preciso que factor global).
     * 
     * Ejemplo de uso:
     * - Segmento 1 (autopista): factor 1.0 (fluido)
     * - Segmento 2 (ciudad): factor 1.8 (atasco)
     * - Segmento 3 (salida): factor 1.2 (moderado)
     * 
     * IMPLEMENTACIÓN FUTURA: Por ahora, FakeTrafficProvider devuelve null.
     * 
     * @param startLat Latitud inicio del segmento
     * @param startLon Longitud inicio del segmento
     * @param endLat Latitud fin del segmento
     * @param endLon Longitud fin del segmento
     * @return Factor de tráfico para ese segmento. Null si no soportado.
     */
    fun getTrafficFactorForSegment(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double? = null  // Default: no soportado
    
    /**
     * Indica si el proveedor tiene datos de tráfico disponibles.
     * 
     * Útil para mostrar advertencias al usuario si no hay datos.
     * 
     * @return true si hay datos disponibles, false si no
     */
    fun isAvailable(): Boolean
    
    /**
     * Obtiene descripción legible del estado del tráfico.
     * 
     * Ejemplos:
     * - "Tráfico fluido"
     * - "Tráfico moderado"
     * - "Tráfico intenso"
     * - "Datos no disponibles"
     * 
     * @param location Ubicación para consultar
     * @return Descripción del tráfico o null si no disponible
     */
    fun getTrafficDescription(location: Location): String? {
        val factor = getTrafficFactor(location) ?: return null
        
        return when {
            factor < 0.9 -> "Tráfico fluido"
            factor < 1.1 -> "Tráfico normal"
            factor < 1.3 -> "Tráfico moderado"
            factor < 1.6 -> "Tráfico intenso"
            else -> "Tráfico muy intenso"
        }
    }
}
