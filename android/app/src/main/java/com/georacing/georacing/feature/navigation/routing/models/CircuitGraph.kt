package com.georacing.georacing.feature.navigation.routing.models

/**
 * Nodo del grafo que representa un punto en el Circuito (intersecciones).
 */
data class CircuitNode(
    val id: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Arista del grafo (un camino entre dos Nodos).
 */
data class CircuitEdge(
    val id: String,
    val source: String,       // ID del nodo origen
    val destination: String,  // ID del nodo destino
    val distanceMeters: Float, // Coste base (Kruskal/Dijkstra clásico)
    // --- Variables Térmicas/Industriales ---
    val surfaceType: SurfaceType = SurfaceType.ASPHALT,
    /**
     * Nivel de protección solar (0.0 = pleno sol ardiente, 1.0 = toldo/interior refrigerado).
     */
    val shadeFactor: Float = 0.0f 
)
