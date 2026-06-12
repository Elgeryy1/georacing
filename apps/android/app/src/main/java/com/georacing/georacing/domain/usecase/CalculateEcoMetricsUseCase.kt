package com.georacing.georacing.domain.usecase

import com.georacing.georacing.domain.model.EcoMetrics
import com.georacing.georacing.infrastructure.health.HealthConnectManager

/**
 * Caso de uso que aplica las reglas de negocio de Sostenibilidad.
 * Transforma datos nativos pasivos (Health Connect) en el valor Gamificado ("CO2 Ahorrado")
 * sin usar sensores GPS en background.
 */
class CalculateEcoMetricsUseCase(
    private val healthConnectManager: HealthConnectManager
) {

    // Asumimos un coche promedio emite 120 gramos de CO2 por kilómetro en tráfico denso
    private val gramsOfCo2PerKm = 120.0

    /**
     * Genera las métricas de eco consultando silenciosamente a HealthConnect.
     */
    suspend operator fun invoke(): EcoMetrics {
        val steps = healthConnectManager.getTodaySteps()
        val distanceMeters = healthConnectManager.getTodayDistanceMeters()
        
        // Si no hay permisos o no hay cuenta de distancia, podemos hacer una estimación 
        // fallback (ej: 0.70m por paso) para que la UI nunca esté vacía.
        val effectiveDistanceMeters = if (distanceMeters == 0.0 && steps > 0) {
            steps * 0.76 // zancada promedio de un adulto en metros
        } else {
            distanceMeters
        }

        val distanceKm = effectiveDistanceMeters / 1000.0
        val co2Saved = distanceKm * gramsOfCo2PerKm

        return EcoMetrics(
            stepsWalkedToday = steps,
            distanceWalkedMeters = effectiveDistanceMeters,
            co2SavedGrams = co2Saved
        )
    }
}
