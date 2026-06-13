package com.georacing.georacing.domain.model

/**
 * Modelo de dominio de Beacon/Baliza.
 * Compatible con Panel Metropolis que gestiona: mode, message, color, brightness, battery, etc.
 */
data class BeaconConfig(
    val id: String,
    val beaconUid: String = "",
    val name: String,
    val zone: String,
    val mapX: Float = 0f,
    val mapY: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val messageNormal: String = "",
    val messageEmergency: String = "",
    val message: String = "",
    val arrowDirection: ArrowDirection = ArrowDirection.NONE,
    val mode: BeaconMode = BeaconMode.NORMAL,
    val color: String = "#00FF00",
    val brightness: Int = 100,
    val batteryLevel: Int = 100,
    val isOnline: Boolean = true,
    val hasScreen: Boolean = false
)

enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT,
    NONE
}

enum class BeaconMode {
    UNCONFIGURED, NORMAL, CONGESTION, EMERGENCY, EVACUATION, MAINTENANCE
}
