package com.georacing.georacing

import com.georacing.georacing.car.Maneuver
import com.georacing.georacing.car.NavigationTextFormatter
import com.georacing.georacing.car.Step as OsrmStep
import org.junit.Assert.*
import org.junit.Test

/**
 * FASE 2.2: Tests para instrucciones de rotonda con ordinales en español.
 *
 * Ejercitan la lógica REAL de producción ([NavigationTextFormatter]), la misma que
 * usa GeoRacingNavigationScreen para el HUD y el TTS. No hay copias del código bajo
 * prueba: si la lógica de formateo cambia o se rompe, estos tests lo detectan.
 *
 * Valida que:
 * - Los ordinales se generan correctamente (primera, segunda, tercera...)
 * - Las rotondas con exit=null usan mensaje genérico
 * - Se incluye el nombre de calle cuando existe
 */
class NavigationTextFormatterTest {

    private fun stepOf(
        type: String,
        modifier: String? = null,
        exit: Int? = null,
        name: String = "",
        distance: Double = 100.0,
        duration: Double = 20.0
    ): OsrmStep = OsrmStep(
        geometry = "",
        maneuver = Maneuver(
            type = type,
            modifier = modifier,
            location = listOf(2.1734, 41.3851),
            exit = exit
        ),
        name = name,
        distance = distance,
        duration = duration
    )

    @Test
    fun `roundabout with exit 1 uses primera salida`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 1))
        assertTrue("Debe contener 'primera salida'", instruction.contains("primera salida"))
    }

    @Test
    fun `roundabout with exit 2 uses segunda salida`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 2))
        assertTrue("Debe contener 'segunda salida'", instruction.contains("segunda salida"))
    }

    @Test
    fun `roundabout with exit 3 uses tercera salida`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 3))
        assertTrue("Debe contener 'tercera salida'", instruction.contains("tercera salida"))
    }

    @Test
    fun `roundabout with exit 4 uses cuarta salida`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 4))
        assertTrue("Debe contener 'cuarta salida'", instruction.contains("cuarta salida"))
    }

    @Test
    fun `roundabout with exit 5 uses quinta salida`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 5))
        assertTrue("Debe contener 'quinta salida'", instruction.contains("quinta salida"))
    }

    @Test
    fun `roundabout with exit 6 uses numeric ordinal`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 6))
        assertTrue("Debe contener '6ª salida'", instruction.contains("6ª salida"))
    }

    @Test
    fun `roundabout with exit 7 uses numeric ordinal`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = 7))
        assertTrue("Debe contener '7ª salida'", instruction.contains("7ª salida"))
    }

    @Test
    fun `roundabout without exit uses generic message`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("roundabout", exit = null))
        assertTrue(
            "Debe contener mensaje genérico",
            instruction.contains("continúa recto") || instruction.contains("toma la salida")
        )
    }

    @Test
    fun `roundabout with exit and street name includes both`() {
        val instruction = NavigationTextFormatter.getInstructionText(
            stepOf("roundabout", exit = 2, name = "Avenida Diagonal")
        )
        assertTrue("Debe contener 'segunda salida'", instruction.contains("segunda salida"))
        assertTrue("Debe incluir nombre de calle", instruction.contains("Avenida Diagonal"))
    }

    @Test
    fun `rotary type is treated same as roundabout`() {
        // "rotary" es sinónimo de "roundabout" en OSRM.
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("rotary", exit = 3))
        assertTrue("Rotary debe usar 'tercera salida'", instruction.contains("tercera salida"))
    }

    @Test
    fun `turn left instruction is in Spanish`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("turn", modifier = "left"))
        assertEquals("Gire a la izquierda", instruction)
    }

    @Test
    fun `turn right with street name appends hacia`() {
        val instruction = NavigationTextFormatter.getInstructionText(
            stepOf("turn", modifier = "right", name = "Calle Mayor")
        )
        assertEquals("Gire a la derecha hacia Calle Mayor", instruction)
    }

    @Test
    fun `arrive instruction is in Spanish`() {
        val instruction = NavigationTextFormatter.getInstructionText(
            stepOf("arrive", distance = 0.0, duration = 0.0)
        )
        assertEquals("Ha llegado a su destino", instruction)
    }

    @Test
    fun `unknown street name is ignored in suffix`() {
        // OSRM devuelve "unknown" para tramos sin nombre; no debe filtrarse al usuario.
        val instruction = NavigationTextFormatter.getInstructionText(
            stepOf("turn", modifier = "left", name = "unknown")
        )
        assertEquals("Gire a la izquierda", instruction)
    }

    @Test
    fun `unmapped maneuver type falls back to street name`() {
        val instruction = NavigationTextFormatter.getInstructionText(
            stepOf("merge", name = "Ronda Litoral")
        )
        assertEquals("Continúe por Ronda Litoral", instruction)
    }

    @Test
    fun `unmapped maneuver type without street falls back to generic`() {
        val instruction = NavigationTextFormatter.getInstructionText(stepOf("fork"))
        assertEquals("Continúe por la ruta", instruction)
    }

    @Test
    fun `exitNumberToSpanishOrdinal maps low numbers to words`() {
        assertEquals("primera", NavigationTextFormatter.exitNumberToSpanishOrdinal(1))
        assertEquals("quinta", NavigationTextFormatter.exitNumberToSpanishOrdinal(5))
    }

    @Test
    fun `exitNumberToSpanishOrdinal uses numeric form from six`() {
        assertEquals("6ª", NavigationTextFormatter.exitNumberToSpanishOrdinal(6))
        assertEquals("10ª", NavigationTextFormatter.exitNumberToSpanishOrdinal(10))
    }
}
