package com.georacing.georacing.car

/**
 * Formateador de instrucciones de navegación a texto natural en español.
 *
 * Centraliza la conversión de una maniobra OSRM ([Step]) a la frase que se muestra
 * en el HUD del coche y que se envía al motor TTS. Es código puro (sin dependencias
 * de Android), por lo que puede ejercitarse directamente desde tests unitarios JVM.
 *
 * FASE 2.2: incluye soporte de rotondas con ordinales en español natural
 * (estilo Google Maps: "En la rotonda, toma la segunda salida").
 */
object NavigationTextFormatter {

    private const val UNKNOWN_STREET = "unknown"

    /**
     * Genera la instrucción de navegación legible para una maniobra OSRM.
     *
     * @param step Paso OSRM con la maniobra, el nombre de la calle y distancias.
     * @return Frase en español lista para mostrar o leer en voz alta.
     */
    fun getInstructionText(step: Step): String {
        val modifier = step.maneuver.modifier
        val type = step.maneuver.type
        val exit = step.maneuver.exit
        val streetName = streetSuffix(step.name)

        return when (type) {
            "turn" -> when (modifier) {
                "left" -> "Gire a la izquierda$streetName"
                "right" -> "Gire a la derecha$streetName"
                "slight left" -> "Continúe ligeramente a la izquierda$streetName"
                "slight right" -> "Continúe ligeramente a la derecha$streetName"
                "sharp left" -> "Gire completamente a la izquierda$streetName"
                "sharp right" -> "Gire completamente a la derecha$streetName"
                else -> "Continúe$streetName"
            }
            "depart" -> "Inicie el recorrido$streetName"
            "arrive" -> "Ha llegado a su destino"
            "roundabout", "rotary" -> formatRoundabout(exit, streetName)
            "continue" -> "Continúe recto$streetName"
            else -> {
                if (step.name.isNotEmpty() && step.name != UNKNOWN_STREET) {
                    "Continúe por ${step.name}"
                } else {
                    "Continúe por la ruta"
                }
            }
        }
    }

    /**
     * Convierte el número de salida de una rotonda a su ordinal en español.
     * Usado para instrucciones naturales tipo Google Maps.
     *
     * @param exit Número de salida (1-based).
     * @return Ordinal en español ("primera", "segunda"...) o forma numérica ("6ª") a partir de 6.
     */
    fun exitNumberToSpanishOrdinal(exit: Int): String = when (exit) {
        1 -> "primera"
        2 -> "segunda"
        3 -> "tercera"
        4 -> "cuarta"
        5 -> "quinta"
        else -> "${exit}ª" // A partir de 6: "6ª", "7ª", etc.
    }

    private fun formatRoundabout(exit: Int?, streetName: String): String {
        return if (exit != null && exit > 0) {
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

    private fun streetSuffix(name: String): String =
        if (name.isNotEmpty() && name != UNKNOWN_STREET) " hacia $name" else ""
}
