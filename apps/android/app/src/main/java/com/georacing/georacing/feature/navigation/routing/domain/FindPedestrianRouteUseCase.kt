package com.georacing.georacing.feature.navigation.routing.domain

import com.georacing.georacing.feature.navigation.routing.algorithm.PedestrianPathfinder
import com.georacing.georacing.feature.navigation.routing.models.CircuitEdge
import com.georacing.georacing.feature.navigation.routing.models.RoutePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caso de Uso que encapsula y expone la solicitud de ruta desde el ViewModel.
 * Recopila la temperatura, la preferencia e inyecta esto al Pathfinder.
 */
class FindPedestrianRouteUseCase(
    private val pathfinder: PedestrianPathfinder
    // Injectaríamos aquí un WeatherRepository offline (BLE o caché)
) {
    /**
     * Calcula de forma asíncrona la mejor ruta según las variables térmicas.
     * En un entorno real `currentTemperature` vendría del repo del clima.
     */
    suspend operator fun invoke(
        startNodeId: String,
        targetNodeId: String,
        preference: RoutePreference,
        currentTemperature: Float // Simulamos 32.0C en horas puntas de Julio
    ): List<CircuitEdge> = withContext(Dispatchers.Default) {
        
        pathfinder.findRoute(
            startId = startNodeId,
            targetId = targetNodeId,
            preference = preference,
            currentTemperature = currentTemperature
        )
    }
}
