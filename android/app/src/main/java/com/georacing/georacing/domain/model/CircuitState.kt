package com.georacing.georacing.domain.model

data class CircuitState(
    val mode: CircuitMode,
    val message: String?,
    val temperature: String?,
    val updatedAt: String,
    // Campos meteorológicos adicionales (Panel Metropolis)
    val humidity: String? = null,
    val wind: String? = null,
    val forecast: String? = null,
    // Race Control Data (Simulated or Real)
    val sessionInfo: RaceSessionInfo? = null 
)

data class RaceSessionInfo(
    val sessionTime: String = "00:00:00",
    val currentLap: Int = 0,
    val totalLaps: Int = 66,
    val topDrivers: List<DriverInfo> = emptyList()
)

data class DriverInfo(
    val position: Int,
    val name: String,
    val team: String,
    val gap: String,
    val tireCompound: String = "S"
)

enum class CircuitMode {
    NORMAL,      // Green Flag implicitly
    GREEN_FLAG,  // Explicit Green
    YELLOW_FLAG, // Local or Full Course Yellow
    SAFETY_CAR,
    VSC,         // Virtual Safety Car
    RED_FLAG,
    EVACUATION,
    UNKNOWN
}
