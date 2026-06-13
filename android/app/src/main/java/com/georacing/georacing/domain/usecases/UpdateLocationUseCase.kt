package com.georacing.georacing.domain.usecases

import android.location.Location
import com.georacing.georacing.car.RouteResult
import com.georacing.georacing.domain.models.NavigationState
import com.georacing.georacing.utils.*

/**
 * Use Case: Actualizar estado de navegación con nueva ubicación GPS.
 * 
 * Este es el use case más importante, se ejecuta cada vez que
 * llega un update del GPS (típicamente cada 1 segundo).
 * 
 * Responsabilidades:
 * 1. Hacer snap de la posición a la ruta
 * 2. Calcular distancia restante
 * 3. Calcular ETA proporcional
 * 4. Detectar paso actual
 * 5. Detectar si está fuera de ruta
 * 6. Actualizar NavigationState.Active
 */
object UpdateLocationUseCase {
    
    /**
     * FASE 3: Factor de tráfico para ajustar ETA.
     * 
     * Valor por defecto: 1.0 (sin ajuste de tráfico)
     * - 1.0 = sin tráfico
     * - 1.2 = tráfico moderado (+20%)
     * - 1.5 = tráfico intenso (+50%)
     * 
     * TODO FASE 3: Reemplazar con TrafficProvider real
     */
    private var trafficFactor: Double = 1.0
    
    /**
     * Establece el factor de tráfico global.
     * FASE 3: Este método será llamado por NavigationManager cuando haya datos de tráfico.
     * 
     * @param factor Factor de tráfico (1.0 = sin cambios, >1.0 = más lento)
     */
    fun setTrafficFactor(factor: Double) {
        trafficFactor = factor.coerceIn(0.5, 3.0)
    }
    
    /**
     * Ejecuta el update de ubicación.
     * 
     * @param newLocation Nueva posición GPS
     * @param currentState Estado actual de navegación
     * @return Nuevo estado actualizado (o el mismo si no hay cambios)
     */
    fun execute(
        newLocation: Location,
        currentState: NavigationState
    ): NavigationState {
        // Solo procesar si estamos en navegación activa
        if (currentState !is NavigationState.Active) {
            return currentState
        }
        
        val route = currentState.route
        
        // 1. SNAP TO ROUTE
        val snapResult = if (currentState.closestPointIndex > 0) {
            // Optimización: buscar solo cerca del último punto conocido
            RouteSnapper.snapToRouteOptimized(
                currentLocation = newLocation,
                routePoints = route.points,
                lastKnownIndex = currentState.closestPointIndex,
                searchRadius = 50
            )
        } else {
            // Primera vez: buscar en toda la ruta
            RouteSnapper.snapToRoute(
                currentLocation = newLocation,
                routePoints = route.points
            )
        }
        
        // 2. CALCULAR DISTANCIA RESTANTE
        val remainingDistance = DistanceCalculator.calculateRemainingDistance(
            snapResult = snapResult,
            routePoints = route.points
        )
        
        // 3. CALCULAR ETA (PROPORCIONAL CON FACTOR DE TRÁFICO - FASE 3)
        val estimatedTimeRemaining = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = remainingDistance,
            totalDistance = route.distance,
            totalDuration = route.duration,
            trafficFactor = trafficFactor  // FASE 3: Actualmente 1.0 (placeholder)
        )
        
        // 4. DETECTAR PASO ACTUAL
        val stepInfo = StepDetector.findCurrentStep(
            snapResult = snapResult,
            route = route
        )
        
        // 5. CHECK OFF-ROUTE
        val isOffRoute = OffRouteDetector.checkOffRoute(
            location = newLocation,
            snapResult = snapResult
        )
        
        // 6. CONSTRUIR NUEVO ESTADO
        return currentState.copy(
            currentStepIndex = stepInfo.index,
            currentStep = stepInfo.step,
            distanceToNextManeuver = stepInfo.distanceToManeuver,
            remainingDistance = remainingDistance,
            estimatedTimeRemaining = estimatedTimeRemaining,
            isOffRoute = isOffRoute,
            closestPointIndex = snapResult.closestIndex,
            distanceToRoute = snapResult.distanceToRoute
        )
    }
    
    /**
     * Helper para determinar si debemos avanzar al siguiente paso.
     * Se llama después de execute() para detectar cambios de paso.
     */
    fun shouldAdvanceToNextStep(state: NavigationState.Active): Boolean {
        // Si estamos a menos de 20m de la maniobra, consideramos que ya la ejecutamos
        return state.distanceToNextManeuver < 20.0 &&
                state.currentStepIndex < state.route.steps.size - 1
    }
}
