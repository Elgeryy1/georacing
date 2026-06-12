package com.georacing.georacing.utils

import android.location.Location
import org.maplibre.android.geometry.LatLng

/**
 * Utilidades para cálculo de distancias en navegación.
 */
object DistanceCalculator {
    
    /**
     * Calcula la distancia restante desde un punto en la ruta hasta el final.
     * 
     * @param snapResult Resultado del snap (punto actual en la ruta)
     * @param routePoints Geometría completa de la ruta
     * @return Distancia en metros
     */
    fun calculateRemainingDistance(
        snapResult: SnapResult,
        routePoints: List<LatLng>
    ): Double {
        var remaining = 0.0
        
        // Sumar distancias de todos los segmentos desde el punto actual hasta el final
        for (i in snapResult.closestIndex until routePoints.size - 1) {
            val p1 = routePoints[i].toLocation()
            val p2 = routePoints[i + 1].toLocation()
            remaining += p1.distanceTo(p2).toDouble()
        }
        
        return remaining
    }
    
    /**
     * Calcula la distancia a un punto específico de la ruta.
     * Útil para saber cuántos metros faltan para un paso/maniobra.
     * 
     * @param currentIndex Índice actual en la ruta
     * @param targetIndex Índice del punto objetivo
     * @param routePoints Geometría de la ruta
     */
    fun calculateDistanceBetweenIndices(
        currentIndex: Int,
        targetIndex: Int,
        routePoints: List<LatLng>
    ): Double {
        if (currentIndex >= targetIndex) return 0.0
        
        var distance = 0.0
        for (i in currentIndex until targetIndex.coerceAtMost(routePoints.size - 1)) {
            val p1 = routePoints[i].toLocation()
            val p2 = routePoints[i + 1].toLocation()
            distance += p1.distanceTo(p2).toDouble()
        }
        
        return distance
    }
    
    /**
     * Calcula distancia directa (línea recta) entre dos puntos.
     * Útil para check rápido de cercanía al destino.
     */
    fun calculateDirectDistance(from: Location, to: LatLng): Double {
        val toLocation = to.toLocation()
        return from.distanceTo(toLocation).toDouble()
    }
}
