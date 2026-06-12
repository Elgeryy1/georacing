package com.georacing.georacing

import android.location.Location
import com.georacing.georacing.utils.RouteSnapper
import org.junit.Assert.*
import org.junit.Test
import org.maplibre.android.geometry.LatLng

/**
 * FASE 2.1: Tests para snap adaptativo en dos pasadas.
 * 
 * Valida que:
 * - Primera pasada rápida funciona cuando estás cerca de la ruta
 * - Segunda pasada se activa cuando la distancia es grande
 * - No hay errores con rutas vacías o índices inválidos
 */
class RouteSnapperAdaptiveTest {
    
    @Test
    fun `snapToRouteAdaptive uses first pass when close to route`() {
        // Track simple: línea recta de 5 puntos
        val routePoints = listOf(
            LatLng(41.3850, 2.1730),
            LatLng(41.3860, 2.1735),
            LatLng(41.3870, 2.1740),  // <-- Punto más cercano esperado
            LatLng(41.3880, 2.1745),
            LatLng(41.3890, 2.1750)
        )
        
        // Ubicación GPS muy cerca del punto 2 (índice 2)
        val location = Location("test").apply {
            latitude = 41.3870
            longitude = 2.1741  // 10m al este del punto
            accuracy = 10f
        }
        
        val result = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = routePoints,
            lastIndex = 2,  // Empieza buscando desde punto 2
            firstRadius = 30,
            secondRadius = 100,
            distanceThresholdMeters = 80.0
        )
        
        // Debería encontrar el punto 2 en primera pasada
        assertEquals(2, result.closestIndex)
        assertTrue("Primera pasada debería estar cerca (<80m)", result.distanceToRoute < 80.0)
    }
    
    @Test
    fun `snapToRouteAdaptive uses second pass when far from route`() {
        // Track simple
        val routePoints = listOf(
            LatLng(41.3850, 2.1730),
            LatLng(41.3860, 2.1735),
            LatLng(41.3870, 2.1740),
            LatLng(41.3880, 2.1745),
            LatLng(41.3890, 2.1750)  // <-- Punto más cercano esperado
        )
        
        // GPS saltó muy lejos (simula túnel GPS)
        val location = Location("test").apply {
            latitude = 41.3891  // ~110m del punto 4, pero fuera del firstRadius desde punto 1
            longitude = 2.1751
            accuracy = 15f
        }
        
        val result = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = routePoints,
            lastIndex = 1,  // Última posición conocida: punto 1
            firstRadius = 30,
            secondRadius = 100,
            distanceThresholdMeters = 80.0
        )
        
        // Debería necesitar segunda pasada para encontrar punto 4
        // (primera pasada desde índice 1 con radius 30 no alcanza el punto 4)
        assertTrue("Segunda pasada debería encontrar el punto correcto", result.closestIndex >= 3)
    }
    
    @Test
    fun `snapToRouteAdaptive handles edge of route correctly`() {
        val routePoints = listOf(
            LatLng(41.3850, 2.1730),
            LatLng(41.3860, 2.1735),
            LatLng(41.3870, 2.1740)
        )
        
        // GPS cerca del último punto
        val location = Location("test").apply {
            latitude = 41.3871
            longitude = 2.1741
            accuracy = 10f
        }
        
        val result = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = routePoints,
            lastIndex = 2,  // Último punto
            firstRadius = 30,
            secondRadius = 100,
            distanceThresholdMeters = 80.0
        )
        
        assertEquals(2, result.closestIndex)
        assertNotNull(result.closestPoint)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `snapToRouteAdaptive throws on empty route`() {
        val emptyRoute = emptyList<LatLng>()
        val location = Location("test").apply {
            latitude = 41.3850
            longitude = 2.1730
        }
        
        RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = emptyRoute,
            lastIndex = 0
        )
    }
    
    @Test
    fun `snapToRouteAdaptive works with single point route`() {
        val singlePointRoute = listOf(LatLng(41.3850, 2.1730))
        
        val location = Location("test").apply {
            latitude = 41.3851
            longitude = 2.1731
        }
        
        val result = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = singlePointRoute,
            lastIndex = 0
        )
        
        assertEquals(0, result.closestIndex)
        assertEquals(singlePointRoute[0], result.closestPoint)
    }
    
    @Test
    fun `snapToRouteAdaptive handles negative lastIndex gracefully`() {
        val routePoints = listOf(
            LatLng(41.3850, 2.1730),
            LatLng(41.3860, 2.1735),
            LatLng(41.3870, 2.1740)
        )
        
        val location = Location("test").apply {
            latitude = 41.3851
            longitude = 2.1731
        }
        
        // lastIndex negativo (debería coercionarse a 0)
        val result = RouteSnapper.snapToRouteAdaptive(
            currentLocation = location,
            routePoints = routePoints,
            lastIndex = -5
        )
        
        assertNotNull(result)
        assertTrue("Index debe estar en rango válido", result.closestIndex in 0..2)
    }
}
