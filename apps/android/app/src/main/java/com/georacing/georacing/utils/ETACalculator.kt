package com.georacing.georacing.utils

/**
 * Calculador de ETA (Estimated Time of Arrival) estilo Google Maps.
 * 
 * IMPORTANTE: Este calculador NO usa la velocidad actual del vehículo,
 * ya que esto causaría que el ETA fluctúe constantemente.
 * 
 * En su lugar, usa el tiempo estimado de la ruta original y calcula
 * proporcionalmente cuánto tiempo queda según la distancia restante.
 */
object ETACalculator {
    
    /**
     * Calcula el tiempo restante proporcionalmente a la ruta original.
     * 
     * Ejemplo:
     * - Ruta total: 15 km, 20 minutos
     * - Distancia restante: 7.5 km (50% de la ruta)
     * - Tiempo restante: 10 minutos (50% del tiempo)
     * 
     * Este método hace que el ETA sea ESTABLE y solo cambie cuando:
     * 1. Se recalcula la ruta (cada 30s o si hay off-route)
     * 2. Hay cambios significativos de tráfico en el servidor
     * 
     * @param remainingDistance Distancia restante en metros
     * @param totalDistance Distancia total de la ruta en metros
     * @param totalDuration Duración total estimada de la ruta en segundos
     * @return Tiempo restante en segundos
     */
    fun calculateRemainingTime(
        remainingDistance: Double,
        totalDistance: Double,
        totalDuration: Double
    ): Double {
        if (totalDistance <= 0) return 0.0
        
        // Cálculo proporcional
        val proportion = remainingDistance / totalDistance
        return proportion * totalDuration
    }
    
    /**
     * Calcula el tiempo restante aplicando un factor de tráfico.
     * 
     * FASE 3: Extensión para soportar datos de tráfico en tiempo real.
     * 
     * El factor de tráfico permite ajustar el ETA según las condiciones actuales:
     * - trafficFactor = 1.0 -> sin tráfico (tiempo normal)
     * - trafficFactor = 1.2 -> tráfico moderado (+20% tiempo)
     * - trafficFactor = 1.5 -> tráfico intenso (+50% tiempo)
     * - trafficFactor = 0.8 -> tráfico fluido (-20% tiempo, poco común)
     * 
     * Ejemplo:
     * - ETA base: 600 segundos (10 min)
     * - Factor tráfico: 1.2 (tráfico moderado)
     * - ETA ajustado: 720 segundos (12 min)
     * 
     * Rango recomendado: 0.8 - 2.0
     * - Valores < 0.5 o > 2.5 probablemente indican datos incorrectos
     * - El factor se limita internamente a [0.5, 3.0] por seguridad
     * 
     * TODO FASE 3: Integrar con TrafficProvider real cuando esté disponible
     * TODO FASE 3: Considerar factores de tráfico por segmento de ruta (no solo global)
     * 
     * @param remainingDistance Distancia restante en metros
     * @param totalDistance Distancia total de la ruta en metros
     * @param totalDuration Duración total estimada de la ruta en segundos
     * @param trafficFactor Multiplicador de tráfico (1.0 = sin cambios, >1.0 = más lento, <1.0 = más rápido)
     * @return Tiempo restante en segundos ajustado por tráfico
     */
    fun calculateRemainingTimeWithTraffic(
        remainingDistance: Double,
        totalDistance: Double,
        totalDuration: Double,
        trafficFactor: Double
    ): Double {
        // Calcular ETA base
        val baseETA = calculateRemainingTime(remainingDistance, totalDistance, totalDuration)
        
        // Limitar factor de tráfico a rangos razonables (evitar datos corruptos)
        val clampedFactor = trafficFactor.coerceIn(0.5, 3.0)
        
        // Aplicar factor de tráfico
        return baseETA * clampedFactor
    }
    
    /**
     * Calcula el timestamp de llegada estimado.
     * 
     * @param remainingTimeSeconds Segundos restantes
     * @return Timestamp Unix en milisegundos
     */
    fun calculateArrivalTimestamp(remainingTimeSeconds: Double): Long {
        val now = System.currentTimeMillis()
        return now + (remainingTimeSeconds * 1000).toLong()
    }
    
    /**
     * Formatea el tiempo restante en formato legible.
     * 
     * Ejemplos:
     * - 45 segundos -> "1 min"
     * - 90 segundos -> "2 min"
     * - 3600 segundos -> "1 h"
     * - 5400 segundos -> "1 h 30 min"
     * - 86400 segundos -> "1 día"
     * 
     * @param seconds Segundos totales
     * @return String formateado
     */
    fun formatDuration(seconds: Double): String {
        val totalSeconds = seconds.toLong()
        
        return when {
            totalSeconds < 60 -> "1 min"
            totalSeconds < 3600 -> {
                val minutes = (totalSeconds / 60).toInt()
                "$minutes min"
            }
            totalSeconds < 86400 -> {
                val hours = (totalSeconds / 3600).toInt()
                val minutes = ((totalSeconds % 3600) / 60).toInt()
                if (minutes > 0) {
                    "$hours h $minutes min"
                } else {
                    "$hours h"
                }
            }
            else -> {
                val days = (totalSeconds / 86400).toInt()
                val hours = ((totalSeconds % 86400) / 3600).toInt()
                if (hours > 0) {
                    "$days d $hours h"
                } else {
                    "$days días"
                }
            }
        }
    }
    
    /**
     * Formatea la hora de llegada en formato HH:mm
     * 
     * @param arrivalTimestamp Timestamp Unix de llegada
     * @return String en formato "14:30"
     */
    fun formatArrivalTime(arrivalTimestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = arrivalTimestamp
        
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        return String.format("%02d:%02d", hour, minute)
    }
}
