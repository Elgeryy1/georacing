package com.georacing.georacing.core.battery.domain

import kotlinx.coroutines.flow.StateFlow

/**
 * Interfaz de dominio para monitorizar la batería, independiente de Android.
 */
interface BatteryMonitor {
    /**
     * Un [StateFlow] que emite el estado actual de la batería en tiempo real.
     */
    val batteryState: StateFlow<BatteryState>
}
