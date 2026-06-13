package com.georacing.georacing.car

data class DestinationModel(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val type: DestinationType
)

enum class DestinationType {
    GATE,
    PARKING,
    PADDOCK,
    FAN_ZONE,
    SERVICE
}
