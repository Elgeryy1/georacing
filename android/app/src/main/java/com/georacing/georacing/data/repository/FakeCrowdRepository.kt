package com.georacing.georacing.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class HeatPoint(
    val lat: Double,
    val lon: Double,
    val intensity: Float, // 0.0 to 1.0
    val radius: Float // Visual radius
)

object FakeCrowdRepository {

    // Hardcoded Strategic Points for Demo
    private val POINTS = listOf(
        // Gate 3 (Main Access)
        HeatPoint(41.57, 2.26, 0.0f, 100f),
        // Food Zone (ComponentesFan Zone)
        HeatPoint(41.568, 2.262, 0.0f, 120f),
        // Paddock
        HeatPoint(41.571, 2.258, 0.0f, 80f)
    )

    fun getHeatPoints(baseIntensity: Float): Flow<List<HeatPoint>> = flow {
        // Emit points modified by the base intensity from Simulator
        val currentPoints = POINTS.map { point ->
            // Random variation for realism, but anchored to baseIntensity
            val localVariation = (Math.random() * 0.1).toFloat()
            val currentIntensity = (baseIntensity + localVariation).coerceIn(0f, 1f)
            point.copy(intensity = currentIntensity)
        }
        emit(currentPoints)
    }
}
