package com.georacing.georacing.domain.model

enum class NodeType {
    GATE, PARKING, FOOD, RESTROOM, MERCHANDISE
}

enum class Confidence {
    HIGH,    // Verified official data
    MEDIUM,  // Likely correct logic/approximation
    LOW,     // Needs check
    PENDING  // Do not use for navigation
}

data class CircuitNode(
    val id: String,
    val name: String,
    val type: NodeType,
    val lat: Double,
    val lon: Double,
    val confidence: Confidence,
    val source: String,
    val description: String = "" // Added context
)
