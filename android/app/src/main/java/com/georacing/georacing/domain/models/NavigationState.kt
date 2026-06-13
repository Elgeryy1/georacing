package com.georacing.georacing.domain.models

import com.georacing.georacing.car.RouteResult
import com.georacing.georacing.car.Step as OsrmStep

/**
 * Estados de la máquina de navegación.
 * Representa todos los posibles estados en el flujo de navegación.
 */
sealed class NavigationState {
    
    /**
     * Estado inicial: sin navegación activa
     */
    object Idle : NavigationState()
    
    /**
     * Vista previa de ruta: usuario ve la ruta pero no ha iniciado navegación
     */
    data class Preview(
        val route: RouteResult,
        val destinationName: String
    ) : NavigationState()
    
    /**
     * Navegación activa: turn-by-turn en curso
     */
    data class Active(
        val route: RouteResult,
        val destinationName: String,
        val currentStepIndex: Int,
        val currentStep: OsrmStep,
        val distanceToNextManeuver: Double,      // Metros hasta próxima maniobra
        val remainingDistance: Double,            // Metros totales restantes
        val estimatedTimeRemaining: Double,       // Segundos restantes (calculado proporcionalmente)
        val isOffRoute: Boolean = false,
        val closestPointIndex: Int = 0,           // Índice del punto más cercano en la ruta
        val distanceToRoute: Double = 0.0         // Distancia al punto más cercano
    ) : NavigationState() {
        
        /**
         * Helper para obtener el siguiente paso (si existe)
         */
        fun getNextStep(): OsrmStep? {
            return if (currentStepIndex + 1 < route.steps.size) {
                route.steps[currentStepIndex + 1]
            } else null
        }
        
        /**
         * Helper para calcular progreso de la ruta (0.0 - 1.0)
         */
        fun getProgress(): Float {
            if (route.distance <= 0) return 1f
            return ((route.distance - remainingDistance) / route.distance).toFloat()
        }
    }
    
    /**
     * Destino alcanzado
     */
    data class Arrived(
        val destinationName: String
    ) : NavigationState()
}
