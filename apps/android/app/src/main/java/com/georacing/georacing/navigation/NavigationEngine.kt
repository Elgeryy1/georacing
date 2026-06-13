package com.georacing.georacing.navigation

import android.location.Location
import android.util.Log
import com.georacing.georacing.car.RouteRepository
import com.georacing.georacing.car.RouteResult
import com.georacing.georacing.car.Step as OsrmStep
import com.georacing.georacing.domain.usecases.CheckArrivalUseCase
import com.georacing.georacing.utils.*
import org.maplibre.android.geometry.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Motor de navegación compartido entre Android Auto y la app móvil.
 * 
 * Este componente encapsula toda la lógica de navegación:
 * - Gestión de rutas (cálculo, recálculo)
 * - Snap a ruta
 * - Cálculo de distancia y ETA
 * - Detección de off-route
 * - Detección de llegada
 * - Detección de pasos/maniobras
 * 
 * Es usado tanto por GeoRacingNavigationScreen (Android Auto) como por
 * CircuitNavigationScreen (app móvil) para garantizar comportamiento consistente.
 */
class NavigationEngine {
    
    private val TAG = "NavigationEngine"
    
    private val routeRepository = RouteRepository()
    
    // Estado de navegación
    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Idle)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()
    
    // Última posición conocida para snap adaptativo
    private var lastSnapResult: SnapResult? = null
    
    // Control de recálculo de ruta
    private var lastRouteCalculationTime = 0L
    private val MIN_RECALCULATION_INTERVAL_MS = 30_000L // 30 segundos
    
    /**
     * Inicia la navegación hacia un destino.
     * 
     * @param destination Coordenadas del destino
     * @param destinationName Nombre del destino para mostrar al usuario
     * @param currentLocation Ubicación actual del usuario (opcional, si ya está disponible)
     */
    suspend fun startNavigation(
        destination: LatLng,
        destinationName: String,
        currentLocation: Location? = null
    ): Boolean {
        Log.i(TAG, "🚀 Iniciando navegación a $destinationName")
        
        _navigationState.value = NavigationState.Loading(destinationName)
        
        // Si no tenemos ubicación actual, esperar a que llegue la primera actualización
        if (currentLocation == null) {
            Log.d(TAG, "Esperando primera ubicación GPS para calcular ruta...")
            _navigationState.value = NavigationState.WaitingForLocation(destination, destinationName)
            return true
        }
        
        // Calcular ruta inicial
        val route = routeRepository.getRoute(
            origin = LatLng(currentLocation.latitude, currentLocation.longitude),
            dest = destination
        )
        
        if (route == null) {
            Log.e(TAG, "❌ Error al calcular ruta inicial")
            _navigationState.value = NavigationState.Error("No se pudo calcular la ruta. Verifica tu conexión.")
            return false
        }
        
        Log.i(TAG, "✅ Ruta calculada: ${route.distance}m, ${route.duration}s, ${route.steps.size} pasos")
        
        // Inicializar estado activo
        _navigationState.value = NavigationState.Active(
            route = route,
            destinationName = destinationName,
            destination = destination,
            currentStepIndex = 0,
            currentStep = route.steps.firstOrNull(),
            distanceToNextManeuver = route.steps.firstOrNull()?.distance ?: 0.0,
            remainingDistance = route.distance,
            estimatedTimeRemaining = route.duration,
            isOffRoute = false,
            closestPointIndex = 0,
            distanceToRoute = 0.0
        )
        
        lastRouteCalculationTime = System.currentTimeMillis()
        
        // Resetear detectores
        OffRouteDetector.reset()
        TTSManager.reset()
        
        return true
    }
    
    /**
     * Actualiza el estado de navegación con una nueva ubicación GPS.
     * 
     * Esta es la función principal que se llama cada vez que llega una nueva señal GPS.
     * Realiza todo el procesamiento necesario:
     * - Snap a ruta
     * - Actualización de distancia y ETA
     * - Detección de off-route y recálculo automático
     * - Detección de llegada
     * - Actualización de paso/maniobra actual
     */
    suspend fun updateLocation(location: Location) {
        val currentState = _navigationState.value
        
        // Si estamos esperando ubicación para calcular ruta inicial
        if (currentState is NavigationState.WaitingForLocation) {
            startNavigation(currentState.destination, currentState.destinationName, location)
            return
        }
        
        // Solo procesar si estamos en navegación activa
        if (currentState !is NavigationState.Active) {
            return
        }
        
        val route = currentState.route
        if (route.points.isEmpty()) {
            Log.w(TAG, "Ruta sin puntos, ignorando actualización")
            return
        }
        
        // 1. SNAP TO ROUTE (adaptativo para mejor precisión)
        val snapResult = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = route.points,
            lastIndex = lastSnapResult?.closestIndex ?: 0,
            firstRadius = 30,
            secondRadius = 100,
            distanceThresholdMeters = 80.0
        )
        
        lastSnapResult = snapResult
        
        Log.d(TAG, "📍 Snap: index=${snapResult.closestIndex}, distToRoute=${snapResult.distanceToRoute.toInt()}m")
        
        // 2. CALCULAR DISTANCIA RESTANTE
        val remainingDistance = DistanceCalculator.calculateRemainingDistance(
            snapResult = snapResult,
            routePoints = route.points
        )
        
        // 3. CALCULAR ETA ESTABLE (proporcional a ruta original)
        val estimatedTimeRemaining = ETACalculator.calculateRemainingTime(
            remainingDistance = remainingDistance,
            totalDistance = route.distance,
            totalDuration = route.duration
        )
        
        // 4. DETECTAR PASO/MANIOBRA ACTUAL
        val stepInfo = StepDetector.findCurrentStep(
            snapResult = snapResult,
            route = route
        )
        
        Log.d(TAG, "📊 Dist: ${(remainingDistance / 1000.0).format(1)} km, ETA: ${formatETA(estimatedTimeRemaining)}, Paso: ${stepInfo.index + 1}/${route.steps.size}")
        
        // 5. DETECCIÓN OFF-ROUTE
        val isOffRoute = OffRouteDetector.checkOffRoute(
            location = location,
            snapResult = snapResult
        )
        
        if (isOffRoute) {
            Log.w(TAG, "⚠️ Usuario fuera de ruta, recalculando...")
            handleOffRoute(location, currentState)
            return
        }
        
        // 6. DETECCIÓN DE LLEGADA
        val arrived = CheckArrivalUseCase.executeSimple(
            currentLocation = location,
            destination = currentState.destination
        )
        
        if (arrived) {
            Log.i(TAG, "🎯 LLEGADA a ${currentState.destinationName}")
            _navigationState.value = NavigationState.Arrived(currentState.destinationName)
            return
        }
        
        // 7. EXTRAER LÍMITE DE VELOCIDAD REAL DEL SEGMENTO ACTUAL (OSRM maxspeed)
        val currentSpeedLimit = getSpeedLimitForSegment(
            segmentIndex = snapResult.closestIndex,
            maxSpeedAnnotations = route.maxSpeedAnnotations
        )
        
        // 8. ACTUALIZAR ESTADO
        _navigationState.value = currentState.copy(
            currentStepIndex = stepInfo.index,
            currentStep = stepInfo.step,
            distanceToNextManeuver = stepInfo.distanceToManeuver,
            remainingDistance = remainingDistance,
            estimatedTimeRemaining = estimatedTimeRemaining,
            isOffRoute = false,
            closestPointIndex = snapResult.closestIndex,
            distanceToRoute = snapResult.distanceToRoute,
            currentSpeedLimit = currentSpeedLimit
        )
    }
    
    /**
     * Maneja la situación cuando el usuario se sale de la ruta.
     * Recalcula automáticamente desde la posición actual.
     */
    private suspend fun handleOffRoute(location: Location, currentState: NavigationState.Active) {
        Log.i(TAG, "🔄 Recalculando ruta desde posición actual...")
        
        val newRoute = routeRepository.getRoute(
            origin = LatLng(location.latitude, location.longitude),
            dest = currentState.destination
        )
        
        if (newRoute == null) {
            Log.e(TAG, "❌ Error al recalcular ruta")
            // Mantener ruta anterior pero marcar como off-route
            _navigationState.value = currentState.copy(isOffRoute = true)
            return
        }
        
        Log.i(TAG, "✅ Nueva ruta calculada: ${newRoute.distance}m, ${newRoute.duration}s")
        
        // Actualizar estado con nueva ruta
        _navigationState.value = NavigationState.Active(
            route = newRoute,
            destinationName = currentState.destinationName,
            destination = currentState.destination,
            currentStepIndex = 0,
            currentStep = newRoute.steps.firstOrNull(),
            distanceToNextManeuver = newRoute.steps.firstOrNull()?.distance ?: 0.0,
            remainingDistance = newRoute.distance,
            estimatedTimeRemaining = newRoute.duration,
            isOffRoute = false,
            closestPointIndex = 0,
            distanceToRoute = 0.0
        )
        
        lastRouteCalculationTime = System.currentTimeMillis()
        
        // Resetear detectores
        OffRouteDetector.reset()
        TTSManager.reset()
    }
    
    /**
     * Recalcula la ruta periódicamente para actualizar datos de tráfico.
     * Solo se ejecuta si ha pasado el intervalo mínimo desde el último cálculo.
     */
    suspend fun recalculateRouteIfNeeded(location: Location): Boolean {
        val currentState = _navigationState.value
        if (currentState !is NavigationState.Active) {
            return false
        }
        
        val now = System.currentTimeMillis()
        if (now - lastRouteCalculationTime < MIN_RECALCULATION_INTERVAL_MS) {
            return false // Muy pronto para recalcular
        }
        
        Log.d(TAG, "🔄 Recalculando ruta por actualización de tráfico...")
        
        val newRoute = routeRepository.getRoute(
            origin = LatLng(location.latitude, location.longitude),
            dest = currentState.destination,
            avoidTraffic = true
        )
        
        if (newRoute != null) {
            _navigationState.value = currentState.copy(
                route = newRoute,
                remainingDistance = newRoute.distance,
                estimatedTimeRemaining = newRoute.duration
            )
            lastRouteCalculationTime = now
            Log.i(TAG, "✅ Ruta actualizada con datos de tráfico")
            return true
        }
        
        return false
    }
    
    /**
     * Detiene la navegación y resetea el estado.
     */
    fun stopNavigation() {
        Log.i(TAG, "🛑 Navegación detenida")
        _navigationState.value = NavigationState.Idle
        lastSnapResult = null
        OffRouteDetector.reset()
        TTSManager.reset()
    }
    
    /**
     * Formatea el tiempo en segundos a un string legible.
     */
    private fun formatETA(seconds: Double): String {
        val mins = (seconds / 60).toInt()
        return if (mins < 60) {
            "$mins min"
        } else {
            val hours = mins / 60
            val remainingMins = mins % 60
            "${hours}h ${remainingMins}min"
        }
    }
    
    /**
     * Extrae el límite de velocidad real del segmento actual usando datos OSRM/OSM.
     * Los annotations de OSRM devuelven un maxspeed por cada segmento entre puntos.
     */
    private fun getSpeedLimitForSegment(
        segmentIndex: Int,
        maxSpeedAnnotations: List<com.georacing.georacing.car.MaxSpeedEntry>
    ): Int? {
        if (maxSpeedAnnotations.isEmpty()) return null
        
        // El índice del segmento es el min de segmentIndex y el tamaño de annotations
        // (annotations tiene N-1 entries para N points)
        val idx = segmentIndex.coerceIn(0, maxSpeedAnnotations.size - 1)
        val entry = maxSpeedAnnotations[idx]
        
        // Si el dato es desconocido o no hay límite, devolver null
        if (entry.unknown == true || entry.none == true) return null
        
        return entry.speed
    }
    
    /**
     * Extensión para formatear Double con decimales.
     */
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}

/**
 * Estados posibles de la navegación.
 */
sealed class NavigationState {
    /** No hay navegación activa */
    object Idle : NavigationState()
    
    /** Cargando ruta inicial */
    data class Loading(val destinationName: String) : NavigationState()
    
    /** Esperando primera ubicación GPS para calcular ruta */
    data class WaitingForLocation(
        val destination: LatLng,
        val destinationName: String
    ) : NavigationState()
    
    /** Navegación activa */
    data class Active(
        val route: RouteResult,
        val destinationName: String,
        val destination: LatLng,
        val currentStepIndex: Int,
        val currentStep: OsrmStep?,
        val distanceToNextManeuver: Double,
        val remainingDistance: Double,
        val estimatedTimeRemaining: Double,
        val isOffRoute: Boolean,
        val closestPointIndex: Int,
        val distanceToRoute: Double,
        val currentSpeedLimit: Int? = null  // Real speed limit from OSM/OSRM in km/h
    ) : NavigationState()
    
    /** Llegada al destino */
    data class Arrived(val destinationName: String) : NavigationState()
    
    /** Error en navegación */
    data class Error(val message: String) : NavigationState()
}
