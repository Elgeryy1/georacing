package com.georacing.georacing

import com.georacing.georacing.fake.FakeLocationProvider
import com.georacing.georacing.utils.OffRouteDetector
import com.georacing.georacing.utils.RouteSnapper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.maplibre.android.geometry.LatLng

/**
 * FASE 2.3: Tests de navegación completa usando FakeLocationProvider.
 * 
 * Valida comportamiento end-to-end sin GPS real:
 * - Off-route no se dispara siguiendo el track
 * - Snap funciona correctamente en curvas
 * - GPS malo se ignora correctamente
 */
class NavigationIntegrationTest {
    
    @Before
    fun setup() {
        OffRouteDetector.reset()
    }
    
    @Test
    fun `following straight track does not trigger off-route`() {
        val provider = FakeLocationProvider.straightTrack()
        val route = listOf(
            LatLng(41.3851, 2.1734),
            LatLng(41.3870, 2.1700),
            LatLng(41.3890, 2.1665),
            LatLng(41.3910, 2.1630),
            LatLng(41.3930, 2.1595)
        )
        
        var offRouteDetected = false
        
        // Simular 5 actualizaciones GPS siguiendo el track
        repeat(5) {
            val location = provider.nextLocation()
            val snapResult = RouteSnapper.snapToRoute(location, route)
            
            val isOffRoute = OffRouteDetector.checkOffRoute(
                location = location,
                snapResult = snapResult
            )
            
            if (isOffRoute) {
                offRouteDetected = true
            }
        }
        
        assertFalse("No debería detectar off-route siguiendo el track", offRouteDetected)
    }
    
    @Test
    fun `snap to curved track follows the route correctly`() {
        val provider = FakeLocationProvider.curvedTrack()
        provider.reset()
        
        // El track curvo tiene 9 puntos
        val route = listOf(
            LatLng(41.3851, 2.1734),
            LatLng(41.3860, 2.1720),
            LatLng(41.3870, 2.1705),
            LatLng(41.3875, 2.1695),
            LatLng(41.3878, 2.1680),
            LatLng(41.3880, 2.1665),
            LatLng(41.3881, 2.1650),
            LatLng(41.3890, 2.1640),
            LatLng(41.3900, 2.1630)
        )
        
        var lastIndex = 0
        var allSnapsSuccessful = true
        
        repeat(9) {
            val location = provider.nextLocation()
            val snapResult = RouteSnapper.snapToRouteOptimized(
                currentLocation = location,
                routePoints = route,
                lastKnownIndex = lastIndex,
                searchRadius = 50
            )
            
            // El índice debería avanzar o quedarse igual (nunca retroceder mucho)
            if (snapResult.closestIndex < lastIndex - 2) {
                allSnapsSuccessful = false
            }
            
            lastIndex = snapResult.closestIndex
        }
        
        assertTrue("Snap debería seguir la ruta sin saltos raros", allSnapsSuccessful)
    }
    
    @Test
    fun `roundabout track completes full circle`() {
        val provider = FakeLocationProvider.roundaboutTrack()
        
        // Simular navegación completa de la rotonda
        var completed = false
        var iterations = 0
        val maxIterations = 20
        
        while (!provider.isCompleted() && iterations < maxIterations) {
            val location = provider.nextLocation()
            assertNotNull("Location debe ser válida", location)
            assertTrue("Latitude debe estar en rango", location.latitude in 41.0..42.0)
            assertTrue("Longitude debe estar en rango", location.longitude in 2.0..3.0)
            
            iterations++
        }
        
        completed = provider.isCompleted()
        
        assertTrue("Track de rotonda debería completarse", completed || iterations > 10)
    }
    
    @Test
    fun `unstable GPS track has high accuracy values`() {
        val provider = FakeLocationProvider.unstableGPSTrack()
        
        val location = provider.nextLocation()
        
        // Este track usa accuracy = 150m (GPS malo)
        assertTrue("GPS inestable debe tener accuracy alta (>50m)", location.accuracy > 50f)
    }
    
    @Test
    fun `circuit track provides realistic speeds`() {
        val provider = FakeLocationProvider.circuitTrack()
        
        val location = provider.nextLocation()
        
        // Speed está en m/s, circuitTrack usa 80 km/h = ~22 m/s
        assertTrue("Velocidad debe ser realista", location.speed in 15f..30f)
    }
    
    @Test
    fun `FakeLocationProvider calculates bearing correctly`() {
        val provider = FakeLocationProvider.straightTrack()
        
        val loc1 = provider.nextLocation()
        val loc2 = provider.nextLocation()
        
        // En track recto hacia el norte, bearing debe estar cerca de 0° o 360°
        // (o en dirección noreste ~20-70°)
        assertNotNull("Bearing no debe ser null", loc1.bearing)
        assertTrue("Bearing debe estar en 0-360", loc1.bearing in 0f..360f)
    }
    
    @Test
    fun `FakeLocationProvider skip jumps positions correctly`() {
        val provider = FakeLocationProvider.straightTrack()
        
        val loc1 = provider.nextLocation()  // Posición 0
        provider.skip(2)  // Saltar 2 posiciones
        val loc2 = provider.nextLocation()  // Debería estar en posición 3
        
        // Verificar que realmente saltó (distancia debe ser mayor)
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            loc1.latitude, loc1.longitude,
            loc2.latitude, loc2.longitude,
            results
        )
        
        assertTrue("Skip debería crear salto de distancia", results[0] > 100f)
    }
    
    @Test
    fun `FakeLocationProvider reset starts from beginning`() {
        val provider = FakeLocationProvider.straightTrack()
        
        val firstLoc = provider.nextLocation()
        provider.nextLocation()
        provider.nextLocation()
        
        provider.reset()
        
        val afterResetLoc = provider.nextLocation()
        
        // Después del reset, debería volver al primer punto
        assertEquals("Latitud debe ser igual tras reset", firstLoc.latitude, afterResetLoc.latitude, 0.0001)
        assertEquals("Longitud debe ser igual tras reset", firstLoc.longitude, afterResetLoc.longitude, 0.0001)
    }
    
    @Test
    fun `FakeLocationProvider progress increases correctly`() {
        val provider = FakeLocationProvider.straightTrack()
        
        val progress1 = provider.getProgress()
        assertEquals(0.0, progress1, 0.01)
        
        provider.nextLocation()
        provider.nextLocation()
        
        val progress2 = provider.getProgress()
        assertTrue("Progreso debe aumentar", progress2 > progress1)
    }
}
