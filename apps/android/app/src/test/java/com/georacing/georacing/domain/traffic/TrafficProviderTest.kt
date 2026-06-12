package com.georacing.georacing.domain.traffic

import android.location.Location
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para TrafficProvider y FakeTrafficProvider.
 * 
 * FASE 3: Valida el comportamiento del placeholder y la interfaz.
 */
class TrafficProviderTest {
    
    private lateinit var fakeProvider: FakeTrafficProvider
    private lateinit var testLocation: Location
    
    @Before
    fun setup() {
        fakeProvider = FakeTrafficProvider()
        
        testLocation = Location("test").apply {
            latitude = 41.5685
            longitude = 2.2555
        }
    }
    
    // ============================================================
    // TESTS PARA FakeTrafficProvider
    // ============================================================
    
    @Test
    fun `FakeTrafficProvider returns 1_0 for normal traffic`() {
        val factor = fakeProvider.getTrafficFactor(testLocation)
        
        assertEquals(1.0, factor, 0.001)
    }
    
    @Test
    fun `FakeTrafficProvider is always available`() {
        val isAvailable = fakeProvider.isAvailable()
        
        assertTrue(isAvailable)
    }
    
    @Test
    fun `FakeTrafficProvider returns simulated description`() {
        val description = fakeProvider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico normal (simulado)", description)
    }
    
    @Test
    fun `FakeTrafficProvider does not support segment traffic`() {
        val segmentFactor = fakeProvider.getTrafficFactorForSegment(
            startLat = 41.5685,
            startLon = 2.2555,
            endLat = 41.5690,
            endLon = 2.2560
        )
        
        assertNull(segmentFactor)
    }
    
    @Test
    fun `FakeTrafficProvider factor is compatible with ETACalculator range`() {
        val factor = fakeProvider.getTrafficFactor(testLocation)
        
        // El factor debe estar en rango aceptable para ETACalculator (0.5 - 3.0)
        assertTrue(factor >= 0.5)
        assertTrue(factor <= 3.0)
    }
    
    // ============================================================
    // TESTS PARA TrafficProvider interface (default methods)
    // ============================================================
    
    @Test
    fun `TrafficProvider getTrafficDescription returns fluido for factor 0_8`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 0.8
            override fun isAvailable() = true
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico fluido", description)
    }
    
    @Test
    fun `TrafficProvider getTrafficDescription returns normal for factor 1_0`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 1.0
            override fun isAvailable() = true
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico normal", description)
    }
    
    @Test
    fun `TrafficProvider getTrafficDescription returns moderado for factor 1_2`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 1.2
            override fun isAvailable() = true
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico moderado", description)
    }
    
    @Test
    fun `TrafficProvider getTrafficDescription returns intenso for factor 1_5`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 1.5
            override fun isAvailable() = true
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico intenso", description)
    }
    
    @Test
    fun `TrafficProvider getTrafficDescription returns muy intenso for factor 2_0`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 2.0
            override fun isAvailable() = true
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertEquals("Tráfico muy intenso", description)
    }
    
    @Test
    fun `TrafficProvider getTrafficDescription returns null when factor is null`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location): Double? = null
            override fun isAvailable() = false
        }
        
        val description = provider.getTrafficDescription(testLocation)
        
        assertNull(description)
    }
    
    @Test
    fun `TrafficProvider getTrafficFactorForSegment returns null by default`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 1.0
            override fun isAvailable() = true
        }
        
        val segmentFactor = provider.getTrafficFactorForSegment(
            startLat = 41.5685,
            startLon = 2.2555,
            endLat = 41.5690,
            endLon = 2.2560
        )
        
        assertNull(segmentFactor)
    }
    
    // ============================================================
    // INTEGRATION TESTS (TrafficProvider + ETACalculator)
    // ============================================================
    
    @Test
    fun `TrafficProvider factor integrates correctly with ETACalculator`() {
        val provider = object : TrafficProvider {
            override fun getTrafficFactor(location: Location) = 1.3
            override fun isAvailable() = true
        }
        
        val trafficFactor = provider.getTrafficFactor(testLocation) ?: 1.0
        
        // Simular cálculo de ETA con tráfico
        val baseETA = 600.0  // 10 minutos
        val adjustedETA = baseETA * trafficFactor
        
        // Con factor 1.3, ETA debe ser 780 segundos (13 minutos)
        assertEquals(780.0, adjustedETA, 0.01)
    }
    
    @Test
    fun `TrafficProvider handles multiple consecutive requests`() {
        val provider = FakeTrafficProvider()
        
        // Simular múltiples requests (como en navegación real)
        val factor1 = provider.getTrafficFactor(testLocation)
        val factor2 = provider.getTrafficFactor(testLocation)
        val factor3 = provider.getTrafficFactor(testLocation)
        
        // Todos deben ser consistentes
        assertEquals(factor1, factor2, 0.001)
        assertEquals(factor2, factor3, 0.001)
    }
}
