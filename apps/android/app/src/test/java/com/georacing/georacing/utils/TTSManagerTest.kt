package com.georacing.georacing.utils

import android.speech.tts.TextToSpeech
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

/**
 * Tests unitarios para TTSManager con flags anti-saltos.
 * 
 * FASE 1.3 + FASE 2.4: Validar que:
 * 1. No se pierden mensajes si el usuario salta de 600m a 150m
 * 2. No se repiten mensajes en el mismo umbral
 * 3. Se resetean flags al cambiar instrucción
 * 4. Solo se dispara el umbral MÁS CERCANO aplicable
 */
class TTSManagerTest {
    
    private lateinit var mockTTS: TextToSpeech
    
    @Before
    fun setUp() {
        mockTTS = mock(TextToSpeech::class.java)
        TTSManager.reset()
    }
    
    @Test
    fun `handleProgressiveTTS speaks at 500m threshold`() {
        TTSManager.handleProgressiveTTS("gira a la derecha", 500.0, mockTTS)
        
        verify(mockTTS).speak(
            eq("En 500 metros, gira a la derecha"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
    }
    
    @Test
    fun `handleProgressiveTTS does not repeat at same threshold`() {
        TTSManager.handleProgressiveTTS("gira a la derecha", 500.0, mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 490.0, mockTTS)  // Aún en 500m
        
        // Solo debe hablar una vez
        verify(mockTTS, times(1)).speak(anyString(), anyInt(), any(), any())
    }
    
    @Test
    fun `handleProgressiveTTS speaks at each threshold when distance decreases gradually`() {
        // Simular progresión normal: 600m → 400m → 150m → 40m
        TTSManager.handleProgressiveTTS("gira a la derecha", 600.0, mockTTS)  // Nada
        TTSManager.handleProgressiveTTS("gira a la derecha", 400.0, mockTTS)  // 500m
        TTSManager.handleProgressiveTTS("gira a la derecha", 150.0, mockTTS)  // 200m
        TTSManager.handleProgressiveTTS("gira a la derecha", 40.0, mockTTS)   // 50m
        
        // Debe hablar 3 veces: 500m, 200m, 50m
        verify(mockTTS, times(3)).speak(anyString(), anyInt(), any(), any())
    }
    
    @Test
    fun `handleProgressiveTTS handles threshold jumps correctly`() {
        // CASO CRÍTICO: Salto de 600m a 150m (saltándose 500m y 200m)
        // Esto puede pasar a alta velocidad o en túnel GPS
        
        TTSManager.handleProgressiveTTS("gira a la derecha", 600.0, mockTTS)
        reset(mockTTS)  // Limpiar llamadas previas
        
        // Salto brusco
        TTSManager.handleProgressiveTTS("gira a la derecha", 150.0, mockTTS)
        
        // Debe hablar el umbral más cercano (200m), no perderse el mensaje
        verify(mockTTS).speak(
            eq("En 200 metros, gira a la derecha"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
    }
    
    @Test
    fun `handleProgressiveTTS resets flags on instruction change`() {
        // Primera instrucción
        TTSManager.handleProgressiveTTS("gira a la derecha", 400.0, mockTTS)  // 500m
        verify(mockTTS, times(1)).speak(anyString(), anyInt(), any(), any())
        
        reset(mockTTS)
        
        // Nueva instrucción (cambió el paso)
        TTSManager.handleProgressiveTTS("gira a la izquierda", 450.0, mockTTS)  // 500m de nuevo
        
        // Debe hablar porque cambió la instrucción, aunque estemos en 500m
        verify(mockTTS).speak(
            eq("En 500 metros, gira a la izquierda"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
    }
    
    @Test
    fun `handleProgressiveTTS says now at very close distance`() {
        TTSManager.handleProgressiveTTS("gira a la derecha", 5.0, mockTTS)
        
        verify(mockTTS).speak(
            eq("gira a la derecha ahora"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
    }
    
    @Test
    fun `reset clears all flags`() {
        // Establecer estado
        TTSManager.handleProgressiveTTS("gira a la derecha", 400.0, mockTTS)  // 500m hablado
        reset(mockTTS)
        
        // Reset
        TTSManager.reset()
        
        // Misma instrucción y distancia debe hablar de nuevo
        TTSManager.handleProgressiveTTS("gira a la derecha", 400.0, mockTTS)
        
        verify(mockTTS).speak(anyString(), anyInt(), any(), any())
    }
    
    @Test
    fun `FASE 2_4 - normal progression 600 to 450 to 190 to 40 to 5m`() {
        // Progresión normal sin saltos
        TTSManager.handleProgressiveTTS("gira a la derecha", 600.0, mockTTS)  // Nada
        verify(mockTTS, never()).speak(anyString(), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 450.0, mockTTS)  // 500m
        verify(mockTTS).speak(contains("500 metros"), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 190.0, mockTTS)  // 200m
        verify(mockTTS).speak(contains("200 metros"), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 40.0, mockTTS)   // 50m
        verify(mockTTS).speak(contains("50 metros"), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 5.0, mockTTS)    // ahora
        verify(mockTTS).speak(contains("ahora"), anyInt(), any(), any())
    }
    
    @Test
    fun `FASE 2_4 - jump from 600m to 150m speaks closest threshold only`() {
        TTSManager.reset()
        
        // Salto brusco de 600m a 150m (se saltea 500m)
        TTSManager.handleProgressiveTTS("gira a la derecha", 600.0, mockTTS)
        verify(mockTTS, never()).speak(anyString(), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 150.0, mockTTS)
        
        // Debe decir SOLO "200 metros" (el más cercano aplicable)
        verify(mockTTS).speak(
            eq("En 200 metros, gira a la derecha"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
        verify(mockTTS, times(1)).speak(anyString(), anyInt(), any(), any())  // Solo una vez
    }
    
    @Test
    fun `FASE 2_4 - does not speak absurd thresholds when already past them`() {
        TTSManager.reset()
        
        // Empieza en 30m (ya cerca)
        TTSManager.handleProgressiveTTS("gira a la derecha", 30.0, mockTTS)
        
        // Debe decir "50 metros", no "500 metros" ni "200 metros"
        verify(mockTTS).speak(
            eq("En 50 metros, gira a la derecha"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
        verify(mockTTS, never()).speak(contains("500 metros"), anyInt(), any(), any())
        verify(mockTTS, never()).speak(contains("200 metros"), anyInt(), any(), any())
    }
    
    @Test
    fun `FASE 2_4 - instruction change between maneuvers resets properly`() {
        TTSManager.reset()
        
        // Primera maniobra
        TTSManager.handleProgressiveTTS("gira a la derecha", 400.0, mockTTS)  // Dice "500 metros"
        verify(mockTTS, times(1)).speak(anyString(), anyInt(), any(), any())
        
        reset(mockTTS)
        TTSManager.handleProgressiveTTS("gira a la derecha", 100.0, mockTTS)  // Dice "200 metros"
        verify(mockTTS, times(1)).speak(anyString(), anyInt(), any(), any())
        
        reset(mockTTS)
        
        // CAMBIO de maniobra (nueva instrucción)
        TTSManager.handleProgressiveTTS("continúa recto", 450.0, mockTTS)
        
        // Debe resetear flags y decir "500 metros" para la NUEVA instrucción
        verify(mockTTS).speak(
            eq("En 500 metros, continúa recto"),
            eq(TextToSpeech.QUEUE_FLUSH),
            any(),
            any()
        )
    }
}
