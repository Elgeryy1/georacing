package com.georacing.georacing.utils

import android.location.Location
import org.maplibre.android.geometry.LatLng

/**
 * Resultado del snap-to-route: punto más cercano en la ruta
 */
data class SnapResult(
    val closestIndex: Int,           // Índice del punto más cercano
    val closestPoint: LatLng,        // Coordenadas del punto más cercano
    val distanceToRoute: Double      // Distancia en metros al punto más cercano
)

/**
 * Utilidad para hacer snap de la posición GPS a la ruta calculada.
 * Esto evita que el coche "salte" fuera de la línea de la ruta en el mapa.
 */
object RouteSnapper {
    
    /**
     * Encuentra el punto más cercano de la ruta a la posición actual.
     * 
     * @param currentLocation Posición GPS actual
     * @param routePoints Lista de puntos que forman la geometría de la ruta
     * @return SnapResult con el punto más cercano e información relacionada
     */
    fun snapToRoute(
        currentLocation: Location,
        routePoints: List<LatLng>
    ): SnapResult {
        if (routePoints.isEmpty()) {
            throw IllegalArgumentException("Route points cannot be empty")
        }
        
        var minDistance = Double.MAX_VALUE
        var closestIndex = 0
        var closestPoint = routePoints[0]
        
        routePoints.forEachIndexed { index, point ->
            val dist = currentLocation.distanceTo(point.toLocation()).toDouble()
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = index
                closestPoint = point
            }
        }
        
        return SnapResult(
            closestIndex = closestIndex,
            closestPoint = closestPoint,
            distanceToRoute = minDistance
        )
    }
    
    /**
     * Variante optimizada que solo busca en un rango alrededor de la última posición conocida.
     * Útil para rutas muy largas donde no tiene sentido buscar en toda la geometría.
     * 
     * @param currentLocation Posición GPS actual
     * @param routePoints Lista de puntos de la ruta
     * @param lastKnownIndex Último índice conocido (de update anterior)
     * @param searchRadius Cuántos puntos buscar antes/después del lastKnownIndex
     */
    fun snapToRouteOptimized(
        currentLocation: Location,
        routePoints: List<LatLng>,
        lastKnownIndex: Int,
        searchRadius: Int = 50
    ): SnapResult {
        return snapInWindow(
            currentLocation = currentLocation,
            routePoints = routePoints,
            lastKnownIndex = lastKnownIndex,
            searchRadius = searchRadius
        )
    }
    
    /**
     * FASE 2.1: Snap adaptativo en dos pasadas.
     * 
     * Primera pasada: búsqueda rápida con ventana reducida (firstRadius).
     * Segunda pasada: si la distancia es muy grande, amplía la búsqueda (secondRadius).
     * 
     * Ideal para rutas con densidad variable de puntos o cuando el GPS puede haber saltado.
     * 
     * @param currentLocation Posición GPS actual
     * @param routePoints Lista completa de puntos de la ruta
     * @param lastIndex Último índice conocido (de update anterior)
     * @param firstRadius Radio inicial para búsqueda rápida (puntos antes/después)
     * @param secondRadius Radio ampliado si primera pasada está muy lejos
     * @param distanceThresholdMeters Si distanceToRoute > threshold, hace segunda pasada
     * @return SnapResult con el mejor punto encontrado
     */
    fun snapToRouteAdaptive(
        currentLocation: Location,
        routePoints: List<LatLng>,
        lastIndex: Int,
        firstRadius: Int = 30,
        secondRadius: Int = 100,
        distanceThresholdMeters: Double = 80.0
    ): SnapResult {
        if (routePoints.isEmpty()) {
            throw IllegalArgumentException("Route points cannot be empty")
        }
        
        // PRIMERA PASADA: búsqueda rápida con ventana reducida
        val firstPassResult = snapInWindow(
            currentLocation = currentLocation,
            routePoints = routePoints,
            lastKnownIndex = lastIndex,
            searchRadius = firstRadius
        )
        
        android.util.Log.d(
            "RouteSnapper",
            "FASE 2.1 - Primera pasada (radius=$firstRadius): " +
            "index=${firstPassResult.closestIndex}, distToRoute=${firstPassResult.distanceToRoute}m"
        )
        
        // Si está cerca, usar resultado de primera pasada
        if (firstPassResult.distanceToRoute <= distanceThresholdMeters) {
            return firstPassResult
        }
        
        // SEGUNDA PASADA: GPS puede haber saltado, ampliar búsqueda
        android.util.Log.w(
            "RouteSnapper",
            "FASE 2.1 - Primera pasada muy lejos (${firstPassResult.distanceToRoute}m > ${distanceThresholdMeters}m), " +
            "ampliando búsqueda con radius=$secondRadius"
        )
        
        val secondPassResult = snapInWindow(
            currentLocation = currentLocation,
            routePoints = routePoints,
            lastKnownIndex = lastIndex,
            searchRadius = secondRadius
        )
        
        android.util.Log.d(
            "RouteSnapper",
            "FASE 2.1 - Segunda pasada: index=${secondPassResult.closestIndex}, " +
            "distToRoute=${secondPassResult.distanceToRoute}m"
        )
        
        // Si segunda pasada sigue muy lejos, advertir (posible off-route)
        if (secondPassResult.distanceToRoute > distanceThresholdMeters) {
            android.util.Log.w(
                "RouteSnapper",
                "FASE 2.1 - Segunda pasada también lejos (${secondPassResult.distanceToRoute}m). " +
                "Posible off-route o GPS inestable."
            )
        }
        
        return secondPassResult
    }
    
    /**
     * Función interna: busca el punto más cercano en una ventana alrededor de lastKnownIndex.
     * Reutilizada por snapToRouteOptimized y snapToRouteAdaptive.
     */
    private fun snapInWindow(
        currentLocation: Location,
        routePoints: List<LatLng>,
        lastKnownIndex: Int,
        searchRadius: Int
    ): SnapResult {
        val startIndex = (lastKnownIndex - searchRadius).coerceAtLeast(0)
        val endIndex = (lastKnownIndex + searchRadius).coerceAtMost(routePoints.size - 1)
        
        var minDistance = Double.MAX_VALUE
        var closestIndex = lastKnownIndex.coerceIn(0, routePoints.size - 1)
        var closestPoint = routePoints[closestIndex]
        
        for (i in startIndex..endIndex) {
            val point = routePoints[i]
            val dist = currentLocation.distanceTo(point.toLocation()).toDouble()
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
                closestPoint = point
            }
        }
        
        return SnapResult(
            closestIndex = closestIndex,
            closestPoint = closestPoint,
            distanceToRoute = minDistance
        )
    }
}

/**
 * Extension function para convertir LatLng a Location
 */
fun LatLng.toLocation(): Location {
    return Location("").apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
    }
}
