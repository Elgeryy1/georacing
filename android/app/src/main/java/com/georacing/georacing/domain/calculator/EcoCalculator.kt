package com.georacing.georacing.domain.calculator

object EcoCalculator {
    /**
     * Calcula el CO2 ahorrado si hubieras hecho este trayecto en coche.
     * 1 km caminando ≈ 120g de CO2 ahorrado.
     */
    fun calculateCo2Saved(distanceMeters: Double): Double {
        val km = distanceMeters / 1000.0
        return km * 120.0
    }
}
