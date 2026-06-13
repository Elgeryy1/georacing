package com.georacing.georacing.domain.model

/**
 * Modelo de dominio para una ruta del circuito con datos de tráfico en tiempo real.
 * Sincronizado con tabla "routes" del backend y Panel Metropolis.
 */
data class RouteTraffic(
    val id: String,
    val name: String,
    val origin: String,
    val destination: String,
    val status: RouteTrafficStatus = RouteTrafficStatus.OPERATIVA,
    val activeUsers: Int = 0,
    val capacity: Int = 0,
    val capacityPercentage: Int = 0,
    val averageSpeed: Double = 0.0,
    val distance: Int = 0,
    val signalQuality: Int = 0,
    val estimatedTime: Int = 0, // minutos
    val velocity: Double = 0.0  // m/s
)

enum class RouteTrafficStatus {
    OPERATIVA,
    SATURADA,
    CERRADA,
    MANTENIMIENTO;

    companion object {
        fun fromString(s: String?): RouteTrafficStatus =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: OPERATIVA
    }
}

/**
 * Modelo de dominio para ocupación de una zona del circuito.
 * Sincronizado con tabla "zone_traffic" del backend y Panel Metropolis.
 */
data class ZoneOccupancy(
    val id: String,
    val name: String,
    val type: String = "GRADA", // GRADA, PADDOCK, FANZONE, VIAL, PARKING
    val status: ZoneOccupancyStatus = ZoneOccupancyStatus.ABIERTA,
    val capacity: Int = 0,
    val currentOccupancy: Int = 0,
    val temperature: Double = 0.0,
    val waitTime: Int = 0,    // minutos
    val entryRate: Int = 0,   // personas/min
    val exitRate: Int = 0     // personas/min
) {
    val occupancyPercentage: Int
        get() = if (capacity > 0) ((currentOccupancy.toDouble() / capacity) * 100).toInt() else 0
    
    val congestionFactor: Double
        get() = when {
            occupancyPercentage >= 90 -> 2.5
            occupancyPercentage >= 75 -> 1.8
            occupancyPercentage >= 60 -> 1.4
            occupancyPercentage >= 40 -> 1.1
            else -> 1.0
        }
}

enum class ZoneOccupancyStatus {
    ABIERTA,
    SATURADA,
    CERRADA,
    MANTENIMIENTO,
    OPERATIVA;

    companion object {
        fun fromString(s: String?): ZoneOccupancyStatus =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: ABIERTA
    }
}
