package com.georacing.georacing.data.ble

import com.georacing.georacing.domain.model.CircuitMode

data class BleCircuitSignal(
    val version: Int,
    val zoneId: Int,
    val mode: CircuitMode,
    val flags: Int,
    val sequence: Int,
    val ttlSeconds: Int,
    val temperature: Int? = null,
    val sourceId: Int? = null, // 🆕 Added Source ID
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        // Basic validation: Version 1 (Circuit) and 2 (Staff Danger) supported
        return version == 1 || version == 2
    }

    fun isExpired(): Boolean {
        val ageSeconds = (System.currentTimeMillis() - timestamp) / 1000
        return ageSeconds > ttlSeconds
    }
}
