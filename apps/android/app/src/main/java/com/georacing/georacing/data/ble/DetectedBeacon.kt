package com.georacing.georacing.data.ble

data class DetectedBeacon(
    val id: String,
    val name: String?,
    val rssi: Int,
    val isGeoRacing: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val circuitMode: com.georacing.georacing.domain.model.CircuitMode? = null // 🆕 Added State
)
