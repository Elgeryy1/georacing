package com.georacing.georacing.utils

import com.georacing.georacing.car.RouteResult
import com.georacing.georacing.car.Step as OsrmStep
import org.maplibre.android.geometry.LatLng

/**
 * Información sobre el paso/maniobra actual
 */
data class StepInfo(
    val index: Int,                    // Índice del paso en route.steps
    val step: OsrmStep,                // Objeto del paso
    val distanceToManeuver: Double     // Metros hasta la maniobra
)

/**
 * Detector del paso/instrucción actual en la navegación.
 * Determina qué maniobra viene a continuación y a qué distancia está.
 */
object StepDetector {
    
    /**
     * Encuentra el paso actual basándose en la posición en la ruta.
     * 
     * Algoritmo:
     * 1. Recorrer todos los pasos de la ruta
     * 2. Acumular la distancia de cada paso
     * 3. Comparar con el progreso actual en la ruta
     * 4. Cuando el progreso del paso > progreso actual -> ese es el próximo paso
     * 
     * @param snapResult Posición actual en la ruta
     * @param route Ruta completa con steps
     * @return StepInfo con el paso actual y distancia a la maniobra
     */
    fun findCurrentStep(
        snapResult: SnapResult,
        route: RouteResult
    ): StepInfo {
        if (route.steps.isEmpty()) {
            throw IllegalStateException("Route has no steps")
        }
        
        var accumulatedDistance = 0.0
        val progressThroughRoute = snapResult.closestIndex.toDouble() / route.points.size
        
        for (stepIndex in route.steps.indices) {
            val step = route.steps[stepIndex]
            accumulatedDistance += step.distance
            
            // Punto de progreso donde debería estar este paso
            val stepProgressPoint = accumulatedDistance / route.distance
            
            // Si este paso está adelante en la ruta
            if (stepProgressPoint > progressThroughRoute) {
                // Calcular distancia exacta al paso
                val distanceToStep = calculateDistanceToStep(
                    snapResult.closestIndex,
                    stepIndex,
                    route
                )
                
                return StepInfo(
                    index = stepIndex,
                    step = step,
                    distanceToManeuver = distanceToStep.coerceAtLeast(0.0)
                )
            }
        }
        
        // Si llegamos aquí, estamos en el último paso
        val lastStep = route.steps.last()
        val remainingDistance = DistanceCalculator.calculateRemainingDistance(
            snapResult,
            route.points
        )
        
        return StepInfo(
            index = route.steps.size - 1,
            step = lastStep,
            distanceToManeuver = remainingDistance
        )
    }
    
    /**
     * Calcula la distancia exacta desde el índice actual hasta el inicio de un paso.
     * 
     * @param currentIndex Índice actual en route.points
     * @param stepIndex Índice del paso objetivo
     * @param route Ruta completa
     */
    private fun calculateDistanceToStep(
        currentIndex: Int,
        stepIndex: Int,
        route: RouteResult
    ): Double {
        // Calcular en qué índice de points empieza este step
        var accumulatedDistance = 0.0
        for (i in 0 until stepIndex) {
            accumulatedDistance += route.steps[i].distance
        }
        
        // Convertir distancia acumulada a índice aproximado
        val progressToStep = accumulatedDistance / route.distance
        val stepStartIndex = (progressToStep * route.points.size).toInt()
            .coerceIn(0, route.points.size - 1)
        
        // Sumar distancias desde currentIndex hasta stepStartIndex
        var distToStep = 0.0
        for (i in currentIndex until stepStartIndex.coerceAtMost(route.points.size - 1)) {
            val p1 = route.points[i].toLocation()
            val p2 = route.points[i + 1].toLocation()
            distToStep += p1.distanceTo(p2).toDouble()
        }
        
        return distToStep
    }
    
    /**
     * Helper para detectar si hemos pasado un paso y debemos avanzar al siguiente.
     * 
     * @param currentStepInfo Información del paso actual
     * @return true si debemos avanzar al siguiente paso
     */
    fun shouldAdvanceToNextStep(currentStepInfo: StepInfo): Boolean {
        // Si estamos a menos de 20m de la maniobra, consideramos que ya la pasamos
        // (threshold pequeño para que las instrucciones de voz sean precisas)
        return currentStepInfo.distanceToManeuver < 20.0
    }
}
