package com.georacing.georacing.domain.model

data class Poi(
    val id: String,
    val name: String,
    val type: PoiType,
    val description: String,
    val zone: String = "",
    val mapX: Float = 0f, // Deprecated, keep for backward compat if needed
    val mapY: Float = 0f, // Deprecated
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class PoiType {
    WC, FOOD, MERCH, ACCESS, PARKING, EXIT, OTHER, GATE, FANZONE, SERVICE
}
