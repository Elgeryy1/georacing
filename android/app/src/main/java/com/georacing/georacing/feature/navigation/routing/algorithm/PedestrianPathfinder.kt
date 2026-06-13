package com.georacing.georacing.feature.navigation.routing.algorithm

import com.georacing.georacing.feature.navigation.routing.models.CircuitEdge
import com.georacing.georacing.feature.navigation.routing.models.CircuitNode
import com.georacing.georacing.feature.navigation.routing.models.RoutePreference
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Motor de enrutamiento A* (A-Star) modificado para "Seguridad Industrial".
 * Permite calcular la ruta más corta (clásica) o la ruta más fresca (térmica)
 * basándose en variables ambientales offline.
 */
class PedestrianPathfinder(
    private val nodes: Map<String, CircuitNode>,
    private val edges: List<CircuitEdge>
) {
    // Mapa de adyacencia: NodeId -> Lista de Aristas que salen de él
    private val graph: Map<String, List<CircuitEdge>> = edges.groupBy { it.source }

    /**
     * Calcula el "Peso Térmico" de un tramo.
     * Si la temperatura es extrema y el usuario pide sombra, penalizamos 
     * exponencialmente los tramos de asfalto y sin protección solar.
     */
    fun calculateEdgeCost(
        edge: CircuitEdge,
        preference: RoutePreference,
        currentTemperature: Float
    ): Float {
        val baseDistance = edge.distanceMeters

        if (preference == RoutePreference.FASTEST) {
            return baseDistance // Comportamiento Dijkstra clásico puro
        }

        // Si es COOLEST, aplicamos la Regla de Negocio
        if (currentTemperature > 28.0f) {
            // Factor base: si hay 0% sombra (shadeFactor=0), la penalización térmica es máxima.
            // Si shadeFactor=1 (100% sombra), la penalización térmica es cero.
            val sunExposurePenalty = 1.0f - edge.shadeFactor
            
            // Incrementamos x3 o x5 según la exposición y tipo de suelo
            // (10 metros al sol pueden "sentirse" como 50 metros para el algoritmo)
            val thermalMultiplier = 1.0f + (sunExposurePenalty * 4.0f) * edge.surfaceType.thermalMultiplier
            
            return baseDistance * thermalMultiplier
        }

        // Si la temperatura es agradable, la preferencia térmica es casi igual a la de distancia
        return baseDistance
    }

    /**
     * Heurística para A* (Distancia en línea recta - Haversine).
     */
    private fun heuristic(nodeId1: String, nodeId2: String): Float {
        val n1 = nodes[nodeId1] ?: return Float.MAX_VALUE
        val n2 = nodes[nodeId2] ?: return Float.MAX_VALUE
        return calculateHaversineDistance(n1.latitude, n1.longitude, n2.latitude, n2.longitude)
    }

    /**
     * Ejecuta el Algoritmo A*.
     * @return Una lista en orden de los ID de las aristas que forman la ruta óptima.
     * Retorna lista vacía si no hay ruta.
     */
    fun findRoute(
        startId: String,
        targetId: String,
        preference: RoutePreference,
        currentTemperature: Float
    ): List<CircuitEdge> {
        if (!nodes.containsKey(startId) || !nodes.containsKey(targetId)) return emptyList()

        // Costes G: Coste real acumulado desde el inicio hasta un nodo
        val gScores = mutableMapOf<String, Float>().withDefault { Float.MAX_VALUE }
        gScores[startId] = 0f

        // Registro del camino (quién visitó a quién a través de qué arista)
        val cameFrom = mutableMapOf<String, CircuitEdge>()

        // Cola de prioridad basada en el F-Score (G-Score + Heurística)
        val openSet = PriorityQueue<Pair<String, Float>>(compareBy { it.second })
        openSet.add(Pair(startId, heuristic(startId, targetId)))

        // Para evitar revisitar nodos de forma ineficiente
        val closedSet = mutableSetOf<String>()

        while (openSet.isNotEmpty()) {
            val (currentId, _) = openSet.poll()

            // ¡Meta encontrada! Reconstruir el camino desde atrás
            if (currentId == targetId) {
                return reconstructPath(cameFrom, currentId)
            }

            if (!closedSet.add(currentId)) continue

            val neighbors = graph[currentId] ?: emptyList()
            for (edge in neighbors) {
                val neighborId = edge.destination
                if (closedSet.contains(neighborId)) continue

                val tentativeGScore = gScores.getValue(currentId) + 
                                      calculateEdgeCost(edge, preference, currentTemperature)

                if (tentativeGScore < gScores.getValue(neighborId)) {
                    cameFrom[neighborId] = edge
                    gScores[neighborId] = tentativeGScore
                    
                    val fScore = tentativeGScore + heuristic(neighborId, targetId)
                    openSet.add(Pair(neighborId, fScore))
                }
            }
        }
        return emptyList() // No se encontró camino
    }

    private fun reconstructPath(cameFrom: Map<String, CircuitEdge>, currentId: String): List<CircuitEdge> {
        val path = mutableListOf<CircuitEdge>()
        var curr = currentId
        while (cameFrom.containsKey(curr)) {
            val edge = cameFrom[curr]!!
            path.add(edge)
            curr = edge.source
        }
        path.reverse()
        return path
    }

    // Fórmula Haversine para distancia en metros
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val R = 6371e3 // Radio de la tierra en metros
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (R * c).toFloat()
    }
}
