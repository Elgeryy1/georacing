package com.georacing.georacing.core.battery.domain

/**
 * Niveles de modo de supervivencia de la aplicación.
 */
enum class SurvivalMode {
    /** Batería por encima del umbral seguro (> 50%). Todo habilitado. */
    NORMAL,
    
    /** Batería en nivel medio (30% - 50%). Posibles advertencias ligeras. */
    WARNING,
    
    /** Batería crítica (<= 30%). Se activan las contramedidas extremas. */
    SURVIVAL
}
