package com.georacing.georacing.domain.usecases

import android.location.Location
import com.georacing.georacing.domain.models.NavigationState
import com.georacing.georacing.utils.DistanceCalculator
import org.maplibre.android.geometry.LatLng

/**
 * Use Case: Detectar cuando el usuario ha llegado al destino.
 * 
 * Criterios de llegada:
 * 1. Distancia directa al destino < 30 metros
 * 2. Velocidad < 5 km/h (prácticamente detenido)
 * 3. Distancia restante de ruta < 50 metros
 */
object CheckArrivalUseCase {
    
    private const val ARRIVAL_DISTANCE_THRESHOLD = 30.0  // metros
    private const val ARRIVAL_SPEED_THRESHOLD = 1.4f      // m/s (~5 km/h)
    private const val ARRIVAL_ROUTE_DISTANCE_THRESHOLD = 50.0  // metros
    
    /**
     * Verifica si el usuario ha llegado al destino.
     * 
     * @param currentLocation Ubicación GPS actual
     * @param currentState Estado de navegación actual
     * @return NavigationState.Arrived si llegó, null si aún no
     */
    fun execute(
        currentLocation: Location,
        currentState: NavigationState.Active
    ): NavigationState.Arrived? {
        
        // Obtener destino
        val destination = currentState.route.points.lastOrNull() ?: return null
        
        // 1. Distancia directa al destino
        val directDistance = DistanceCalculator.calculateDirectDistance(
            from = currentLocation,
            to = destination
        )
        
        // 2. Velocidad actual
        val speedMps = currentLocation.speed  // m/s
        
        // 3. Distancia restante de ruta
        val routeDistance = currentState.remainingDistance
        
        android.util.Log.d(
            "CheckArrival",
            "Distance: ${directDistance}m, Speed: ${speedMps * 3.6f}km/h, Route: ${routeDistance}m"
        )
        
        // Criterio principal: distancia directa
        val arrivedByDistance = directDistance < ARRIVAL_DISTANCE_THRESHOLD
        
        // Criterio secundario: distancia de ruta (por si la directa no es precisa)
        val arrivedByRoute = routeDistance < ARRIVAL_ROUTE_DISTANCE_THRESHOLD
        
        if (arrivedByDistance || arrivedByRoute) {
            android.util.Log.i(
                "CheckArrival",
                "🎯 ARRIVED at ${currentState.destinationName}"
            )
            
            return NavigationState.Arrived(
                destinationName = currentState.destinationName
            )
        }
        
        return null  // Aún no ha llegado
    }
    
    /**
     * Variante que solo considera distancia directa (más simple).
     * Útil para destinos en plazas/parkings amplios.
     */
    fun executeSimple(
        currentLocation: Location,
        destination: LatLng
    ): Boolean {
        val distance = DistanceCalculator.calculateDirectDistance(
            from = currentLocation,
            to = destination
        )
        
        return distance < ARRIVAL_DISTANCE_THRESHOLD
    }
}
