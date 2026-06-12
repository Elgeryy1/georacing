package com.georacing.georacing.domain.usecases

import android.location.Location
import com.georacing.georacing.car.RouteRepository
import com.georacing.georacing.domain.models.NavigationState
import org.maplibre.android.geometry.LatLng

/**
 * Use Case: Recalcular ruta cuando el usuario se desvía.
 * 
 * Se ejecuta cuando:
 * - OffRouteDetector confirma que estamos fuera de ruta
 * - Han pasado 30+ segundos desde el último cálculo (actualización por tráfico)
 */
object RecalculateRouteUseCase {
    
    private val routeRepository = RouteRepository()
    
    /**
     * Recalcula la ruta desde la posición actual al destino original.
     * 
     * @param currentLocation Posición GPS actual
     * @param currentState Estado de navegación actual
     * @return Nuevo estado con ruta actualizada, o null si falla el recalculo
     */
    suspend fun execute(
        currentLocation: Location,
        currentState: NavigationState.Active
    ): NavigationState.Active? {
        // Obtener destino de la ruta actual
        val destination = currentState.route.points.lastOrNull() ?: return null
        
        android.util.Log.i(
            "RecalculateRoute",
            "Recalculando ruta desde (${currentLocation.latitude}, ${currentLocation.longitude}) a destino"
        )
        
        // Llamar al repositorio para obtener nueva ruta
        val newRoute = routeRepository.getRoute(
            origin = LatLng(currentLocation.latitude, currentLocation.longitude),
            dest = destination,
            avoidTraffic = true
        )
        
        if (newRoute == null) {
            android.util.Log.e("RecalculateRoute", "Failed to get new route")
            return null
        }
        
        android.util.Log.i(
            "RecalculateRoute",
            "Nueva ruta: ${newRoute.distance}m, ${newRoute.duration}s, ${newRoute.steps.size} steps"
        )
        
        // Resetear OffRouteDetector (ya no estamos fuera de ruta con la nueva)
        com.georacing.georacing.utils.OffRouteDetector.reset()
        
        // Resetear TTSManager para que anuncie las nuevas instrucciones
        com.georacing.georacing.utils.TTSManager.reset()
        
        // Crear nuevo estado con la ruta recalculada
        return NavigationState.Active(
            route = newRoute,
            destinationName = currentState.destinationName,
            currentStepIndex = 0,  // Volver al primer paso
            currentStep = newRoute.steps.firstOrNull() 
                ?: currentState.currentStep,  // Fallback si no hay steps
            distanceToNextManeuver = newRoute.steps.firstOrNull()?.distance ?: 0.0,
            remainingDistance = newRoute.distance,
            estimatedTimeRemaining = newRoute.duration,
            isOffRoute = false,  // Ya no estamos fuera de ruta
            closestPointIndex = 0,  // Reset
            distanceToRoute = 0.0
        )
    }
    
    /**
     * Versión simplificada que solo recalcula si han pasado X segundos.
     * Útil para actualizaciones periódicas por tráfico.
     */
    suspend fun executeIfNeeded(
        currentLocation: Location,
        currentState: NavigationState.Active,
        lastRecalculationTime: Long,
        minIntervalMs: Long = 30_000L  // 30 segundos por defecto
    ): NavigationState.Active? {
        val now = System.currentTimeMillis()
        val timeSinceLastRecalc = now - lastRecalculationTime
        
        if (timeSinceLastRecalc < minIntervalMs) {
            return null  // Aún no toca recalcular
        }
        
        return execute(currentLocation, currentState)
    }
}
