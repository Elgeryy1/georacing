package com.georacing.georacing.domain.model

/**
 * Estados de energía de la aplicación.
 * Determina qué funcionalidades están activas según el nivel de batería.
 */
enum class AppPowerState {
    /**
     * NORMAL: Todas las funcionalidades activas.
     * Batería > 30%
     */
    NORMAL,

    /**
     * POWER_SAVE: Modo ahorro de energía.
     * Batería entre 15% y 30%
     * - Desactiva gamificación
     * - Reduce frecuencia de sync
     * - Tema oscuro forzado
     */
    POWER_SAVE,

    /**
     * CRITICAL: Solo funciones de supervivencia.
     * Batería < 15%
     * - Solo EmergencyScreen
     * - Tema OLED negro puro
     * - Sin BLE scanning (solo advertising si es staff)
     */
    CRITICAL
}
