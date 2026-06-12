package com.georacing.georacing.utils

import android.location.Location
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para OffRouteDetector con threshold dinámico por velocidad.
 * 
 * FASE 1.2: Validar que los thresholds se ajustan según velocidad:
 * - < 40 km/h → 30m (parking/ciudad)
 * - 40-80 km/h → 50m (carretera normal)
 * - > 80 km/h → 80m (autopista)
 */
class OffRouteDetectorTest {
    
    @Before
    fun setUp() {
        OffRouteDetector.reset()
    }
    
    @Test
    fun `dynamicOffRouteThreshold returns 30m for city speed`() {
        assertEquals(30.0, OffRouteDetector.dynamicOffRouteThreshold(20f), 0.01)
        assertEquals(30.0, OffRouteDetector.dynamicOffRouteThreshold(39f), 0.01)
    }
    
    @Test
    fun `dynamicOffRouteThreshold returns 50m for normal road speed`() {
        assertEquals(50.0, OffRouteDetector.dynamicOffRouteThreshold(40f), 0.01)
        assertEquals(50.0, OffRouteDetector.dynamicOffRouteThreshold(60f), 0.01)
        assertEquals(50.0, OffRouteDetector.dynamicOffRouteThreshold(79f), 0.01)
    }
    
    @Test
    fun `dynamicOffRouteThreshold returns 80m for highway speed`() {
        assertEquals(80.0, OffRouteDetector.dynamicOffRouteThreshold(80f), 0.01)
        assertEquals(80.0, OffRouteDetector.dynamicOffRouteThreshold(120f), 0.01)
    }
    
    @Test
    fun `checkOffRoute returns false immediately when distance exceeds threshold`() {
        val location = createLocation(0.0, 0.0, speedKmh = 50f)
        val snapResult = createSnapResult(distanceToRoute = 60.0)  // > 50m threshold
        
        // Primera llamada: debe dar período de gracia
        val result = OffRouteDetector.checkOffRoute(location, snapResult)
        
        assertFalse("No debe confirmar off-route inmediatamente", result)
    }
    
    @Test
    fun `checkOffRoute confirms after 3 seconds timeout`() {
        val location = createLocation(0.0, 0.0, speedKmh = 50f)
        val snapResult = createSnapResult(distanceToRoute = 60.0)  // > 50m threshold
        
        // Primera detección: guardar timestamp
        OffRouteDetector.checkOffRoute(location, snapResult)
        
        // Simular paso de 3+ segundos ajustando el timestamp interno (hack de test)
        Thread.sleep(3100)
        
        // Segunda llamada: debe confirmar
        val result = OffRouteDetector.checkOffRoute(location, snapResult)
        
        assertTrue("Debe confirmar off-route tras 3s", result)
    }
    
    @Test
    fun `checkOffRoute resets when back on route`() {
        val location = createLocation(0.0, 0.0, speedKmh = 50f)
        
        // Off-route inicial
        val offRouteSnap = createSnapResult(distanceToRoute = 60.0)
        OffRouteDetector.checkOffRoute(location, offRouteSnap)
        
        // Volver a ruta
        val onRouteSnap = createSnapResult(distanceToRoute = 20.0)  // < 50m threshold
        val result = OffRouteDetector.checkOffRoute(location, onRouteSnap)
        
        assertFalse("Debe resetear estado cuando vuelve a ruta", result)
    }
    
    // Helper methods
    
    private fun createLocation(lat: Double, lon: Double, speedKmh: Float): Location {
        return Location("test").apply {
            latitude = lat
            longitude = lon
            speed = speedKmh / 3.6f  // Convertir km/h a m/s
            accuracy = 10f
        }
    }
    
    private fun createSnapResult(distanceToRoute: Double): com.georacing.georacing.utils.SnapResult {
        return com.georacing.georacing.utils.SnapResult(
            closestIndex = 0,
            closestPoint = org.maplibre.android.geometry.LatLng(0.0, 0.0),
            distanceToRoute = distanceToRoute
        )
    }
}
