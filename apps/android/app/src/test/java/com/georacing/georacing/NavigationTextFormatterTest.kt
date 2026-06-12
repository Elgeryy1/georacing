package com.georacing.georacing

import com.georacing.georacing.car.Maneuver
import com.georacing.georacing.car.Step as OsrmStep
import org.junit.Assert.*
import org.junit.Test

/**
 * FASE 2.2: Tests para instrucciones de rotonda con ordinales en español.
 * 
 * Valida que:
 * - Los ordinales se generan correctamente (primera, segunda, tercera...)
 * - Las rotondas con exit=null usan mensaje genérico
 * - Se incluye el nombre de calle cuando existe
 */
class NavigationTextFormatterTest {
    
    /**
     * Helper para simular getInstructionText como está en GeoRacingNavigationScreen.
     * En producción esto estaría en la clase real, aquí lo replicamos para tests.
     */
    private fun getInstructionText(step: OsrmStep): String {
        val modifier = step.maneuver.modifier
        val type = step.maneuver.type
        val exit = step.maneuver.exit
        val streetName = if (step.name.isNotEmpty() && step.name != "unknown") " hacia ${step.name}" else ""
        
        return when (type) {
            "turn" -> {
                when (modifier) {
                    "left" -> "Gire a la izquierda$streetName"
                    "right" -> "Gire a la derecha$streetName"
                    "slight left" -> "Continúe ligeramente a la izquierda$streetName"
                    "slight right" -> "Continúe ligeramente a la derecha$streetName"
                    "sharp left" -> "Gire completamente a la izquierda$streetName"
                    "sharp right" -> "Gire completamente a la derecha$streetName"
                    else -> "Continúe$streetName"
                }
            }
            "depart" -> "Inicie el recorrido$streetName"
            "arrive" -> "Ha llegado a su destino"
            "roundabout", "rotary" -> {
                if (exit != null && exit > 0) {
                    val ordinal = exitNumberToSpanishOrdinal(exit)
                    if (streetName.isNotEmpty()) {
                        "En la rotonda, toma la $ordinal salida$streetName"
                    } else {
                        "En la rotonda, toma la $ordinal salida"
                    }
                } else {
                    if (streetName.isNotEmpty()) {
                        "En la rotonda, toma la salida$streetName"
                    } else {
                        "En la rotonda, continúa recto"
                    }
                }
            }
            "continue" -> "Continúe recto$streetName"
            else -> {
                if (step.name.isNotEmpty() && step.name != "unknown") {
                    "Continúe por ${step.name}"
                } else {
                    "Continúe por la ruta"
                }
            }
        }
    }
    
    private fun exitNumberToSpanishOrdinal(exit: Int): String {
        return when (exit) {
            1 -> "primera"
            2 -> "segunda"
            3 -> "tercera"
            4 -> "cuarta"
            5 -> "quinta"
            else -> "${exit}ª"
        }
    }
    
    @Test
    fun `roundabout with exit 1 uses primera salida`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 1
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'primera salida'", instruction.contains("primera salida"))
    }
    
    @Test
    fun `roundabout with exit 2 uses segunda salida`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 2
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'segunda salida'", instruction.contains("segunda salida"))
    }
    
    @Test
    fun `roundabout with exit 3 uses tercera salida`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 3
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'tercera salida'", instruction.contains("tercera salida"))
    }
    
    @Test
    fun `roundabout with exit 4 uses cuarta salida`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 4
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'cuarta salida'", instruction.contains("cuarta salida"))
    }
    
    @Test
    fun `roundabout with exit 5 uses quinta salida`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 5
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'quinta salida'", instruction.contains("quinta salida"))
    }
    
    @Test
    fun `roundabout with exit 6 uses numeric ordinal`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 6
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener '6ª salida'", instruction.contains("6ª salida"))
    }
    
    @Test
    fun `roundabout with exit 7 uses numeric ordinal`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 7
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener '7ª salida'", instruction.contains("7ª salida"))
    }
    
    @Test
    fun `roundabout without exit uses generic message`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = null
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener mensaje genérico", 
            instruction.contains("continúa recto") || instruction.contains("toma la salida"))
    }
    
    @Test
    fun `roundabout with exit and street name includes both`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "roundabout",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 2
            ),
            name = "Avenida Diagonal",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Debe contener 'segunda salida'", instruction.contains("segunda salida"))
        assertTrue("Debe incluir nombre de calle", instruction.contains("Avenida Diagonal"))
    }
    
    @Test
    fun `rotary type is treated same as roundabout`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "rotary",  // Sinónimo de roundabout en OSRM
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = 3
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertTrue("Rotary debe usar 'tercera salida'", instruction.contains("tercera salida"))
    }
    
    @Test
    fun `turn left instruction is in Spanish`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "turn",
                modifier = "left",
                location = listOf(2.1734, 41.3851),
                exit = null
            ),
            name = "",
            distance = 100.0,
            duration = 20.0
        )
        
        val instruction = getInstructionText(step)
        
        assertEquals("Gire a la izquierda", instruction)
    }
    
    @Test
    fun `arrive instruction is in Spanish`() {
        val step = OsrmStep(
            geometry = "",
            maneuver = com.georacing.georacing.car.Maneuver(
                type = "arrive",
                modifier = null,
                location = listOf(2.1734, 41.3851),
                exit = null
            ),
            name = "",
            distance = 0.0,
            duration = 0.0
        )
        
        val instruction = getInstructionText(step)
        
        assertEquals("Ha llegado a su destino", instruction)
    }
}
