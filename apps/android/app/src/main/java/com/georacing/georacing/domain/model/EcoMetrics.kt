package com.georacing.georacing.domain.model

/**
 * Modelo de negocio de Sostenibilidad / Eficiencia nativa.
 */
data class EcoMetrics(
    val stepsWalkedToday: Long,
    val distanceWalkedMeters: Double,
    val co2SavedGrams: Double
) {
    /**
     * Devuelve la distancia redondeada en formato Kilómetros (ej: 3.5 km)
     */
    val distanceFormatted: String
        get() = String.format("%.2f km", distanceWalkedMeters / 1000.0)

    /**
     * Devuelve el CO2 en kilogramos si supera los 1000g, o en gramos.
     */
    val co2Formatted: String
        get() = if (co2SavedGrams >= 1000.0) {
            String.format("%.2f kg", co2SavedGrams / 1000.0)
        } else {
            String.format("%.0f g", co2SavedGrams)
        }
}
