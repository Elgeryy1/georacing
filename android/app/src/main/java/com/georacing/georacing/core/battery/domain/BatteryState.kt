package com.georacing.georacing.core.battery.domain

/**
 * Representa el estado actual de la batería.
 */
data class BatteryState(
    val percentage: Int,
    val isCharging: Boolean
) {
    companion object {
        val UNKNOWN = BatteryState(percentage = -1, isCharging = false)
    }
}
