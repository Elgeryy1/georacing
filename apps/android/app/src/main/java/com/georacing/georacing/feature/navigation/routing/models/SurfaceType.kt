package com.georacing.georacing.feature.navigation.routing.models

/**
 * Tipos de superficie por las que puede caminar un usuario.
 * Afecta directamente la retención térmica (y el cansancio).
 */
enum class SurfaceType(val thermalMultiplier: Float) {
    ASPHALT(1.5f),   // El asfalto irradia calor (más penalización)
    CONCRETE(1.2f),  // Hormigón, algo mejor que el asfalto oscuro
    GRAVEL(1.0f),    // Grava/Tierra compactada (neutro)
    GRASS(0.8f)      // Césped/Tierra natural (agradable térmicamente)
}
