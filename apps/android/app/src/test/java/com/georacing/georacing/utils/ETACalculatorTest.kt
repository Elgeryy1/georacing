package com.georacing.georacing.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests unitarios para ETACalculator (validación de fórmula proporcional).
 * 
 * Valida que el cálculo de ETA es estable y NO usa velocidad instantánea.
 */
class ETACalculatorTest {
    
    @Test
    fun `calculateRemainingTime returns proportional ETA`() {
        // Ruta total: 10 km, 600 segundos (10 minutos)
        // Quedan: 5 km (50%)
        // ETA esperado: 300 segundos (5 minutos) - 50% del tiempo total
        
        val remainingTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        assertEquals(300.0, remainingTime, 0.01)
    }
    
    @Test
    fun `calculateRemainingTime at 75 percent complete`() {
        // Ruta total: 10 km, 600 segundos
        // Quedan: 2.5 km (25%)
        // ETA esperado: 150 segundos (2.5 minutos)
        
        val remainingTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 2500.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        assertEquals(150.0, remainingTime, 0.01)
    }
    
    @Test
    fun `calculateRemainingTime near arrival`() {
        // Ruta total: 10 km, 600 segundos
        // Quedan: 100 m (1%)
        // ETA esperado: 6 segundos
        
        val remainingTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 100.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        assertEquals(6.0, remainingTime, 0.01)
    }
    
    @Test
    fun `calculateRemainingTime returns 0 when totalDistance is 0`() {
        val remainingTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 5000.0,
            totalDistance = 0.0,
            totalDuration = 600.0
        )
        
        assertEquals(0.0, remainingTime, 0.01)
    }
    
    @Test
    fun `calculateRemainingTime is stable regardless of speed fluctuations`() {
        // La fórmula proporcional NO depende de velocidad instantánea
        // Mismo resultado para misma distancia restante
        
        val result1 = ETACalculator.calculateRemainingTime(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        val result2 = ETACalculator.calculateRemainingTime(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        assertEquals(result1, result2, 0.0)
    }
    
    @Test
    fun `calculateRemainingTime handles very long routes`() {
        // Ruta larga: 500 km, 18000 segundos (5 horas)
        // Quedan: 100 km (20%)
        // ETA esperado: 3600 segundos (1 hora)
        
        val remainingTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 100000.0,
            totalDistance = 500000.0,
            totalDuration = 18000.0
        )
        
        assertEquals(3600.0, remainingTime, 0.01)
    }
    
    // ============================================================
    // FASE 3: Tests para calculateRemainingTimeWithTraffic
    // ============================================================
    
    @Test
    fun `calculateRemainingTimeWithTraffic with no traffic factor returns same as base`() {
        // Factor 1.0 = sin tráfico, debería dar el mismo resultado que calculateRemainingTime
        val baseTime = ETACalculator.calculateRemainingTime(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0
        )
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 1.0
        )
        
        assertEquals(baseTime, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic with moderate traffic`() {
        // Factor 1.2 = tráfico moderado (+20%)
        // ETA base: 300 segundos
        // ETA esperado: 360 segundos (300 * 1.2)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 1.2
        )
        
        assertEquals(360.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic with heavy traffic`() {
        // Factor 1.5 = tráfico intenso (+50%)
        // ETA base: 300 segundos
        // ETA esperado: 450 segundos (300 * 1.5)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 1.5
        )
        
        assertEquals(450.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic with flowing traffic`() {
        // Factor 0.8 = tráfico fluido (-20%)
        // ETA base: 300 segundos
        // ETA esperado: 240 segundos (300 * 0.8)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 0.8
        )
        
        assertEquals(240.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic clamps extreme low factor`() {
        // Factor 0.2 (muy bajo) debería limitarse a 0.5
        // ETA base: 300 segundos
        // ETA esperado: 150 segundos (300 * 0.5, no 60)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 0.2
        )
        
        assertEquals(150.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic clamps extreme high factor`() {
        // Factor 5.0 (muy alto) debería limitarse a 3.0
        // ETA base: 300 segundos
        // ETA esperado: 900 segundos (300 * 3.0, no 1500)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 5000.0,
            totalDistance = 10000.0,
            totalDuration = 600.0,
            trafficFactor = 5.0
        )
        
        assertEquals(900.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic handles zero distance with traffic`() {
        // Distancia total = 0, debería retornar 0 independientemente del factor de tráfico
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 0.0,
            totalDistance = 0.0,
            totalDuration = 600.0,
            trafficFactor = 1.5
        )
        
        assertEquals(0.0, timeWithTraffic, 0.01)
    }
    
    @Test
    fun `calculateRemainingTimeWithTraffic real-world scenario with congestion`() {
        // Escenario real: Ruta de 20 km que normalmente toma 15 minutos (900 seg)
        // Tráfico pesado: factor 1.8 (+80% más lento)
        // Quedan 10 km (50% de la ruta)
        // ETA base: 450 segundos
        // ETA con tráfico: 810 segundos (13.5 minutos)
        
        val timeWithTraffic = ETACalculator.calculateRemainingTimeWithTraffic(
            remainingDistance = 10000.0,
            totalDistance = 20000.0,
            totalDuration = 900.0,
            trafficFactor = 1.8
        )
        
        assertEquals(810.0, timeWithTraffic, 0.01)
    }
}

