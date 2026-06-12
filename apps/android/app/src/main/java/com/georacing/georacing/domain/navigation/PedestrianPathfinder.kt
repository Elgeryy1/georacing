package com.georacing.georacing.domain.navigation

import android.util.Log
import org.maplibre.android.geometry.LatLng
import java.util.PriorityQueue
import kotlin.math.*

/**
 * Motor de pathfinding A* local para rutas peatonales dentro del Circuit de Barcelona-Catalunya.
 *
 * Grafo de nodos peatonales internos del circuito con soporte para:
 * - Pesos dinámicos por congestión (anti-colas)
 * - Preferencia de rutas con sombra (12:00-16:00)
 * - Rutas accesibles (sin escaleras)
 * - Funcionamiento 100% offline
 */
object PedestrianPathfinder {

    private const val TAG = "PedestrianPathfinder"

    // ── Nodos del grafo peatonal ──

    data class PathNode(
        val id: String,
        val name: String,
        val position: LatLng,
        val hasStairs: Boolean = false,
        val hasShadow: Boolean = false, // Tiene sombra entre 12-16h
        val isIndoor: Boolean = false
    )

    data class PathEdge(
        val from: String,
        val to: String,
        val distanceMeters: Double,
        val hasStairs: Boolean = false,
        val hasShadow: Boolean = false,
        val congestionWeight: Double = 1.0 // 1.0 = normal, >1 = congestionado
    )

    data class PathResult(
        val nodes: List<PathNode>,
        val totalDistance: Double,
        val estimatedTimeSeconds: Double, // A 4.5 km/h peatón
        val steps: List<PedestrianStep>,
        val usedAccessibleRoute: Boolean = false,
        val usedShadowRoute: Boolean = false
    )

    data class PedestrianStep(
        val instruction: String,
        val distance: Double,
        val fromNode: PathNode,
        val toNode: PathNode
    )

    // ── Grafo del Circuit de Barcelona-Catalunya ──

    private val nodes = listOf(
        // Accesos principales
        PathNode("gate_main", "Acceso Principal", LatLng(41.5693, 2.2577)),
        PathNode("gate_1", "Porta 1", LatLng(41.5700, 2.2590)),
        PathNode("gate_3", "Porta 3", LatLng(41.5688, 2.2610)),
        PathNode("gate_7", "Porta 7", LatLng(41.5675, 2.2555)),
        
        // Tribunas
        PathNode("trib_main", "Tribuna Principal", LatLng(41.5695, 2.2585), hasShadow = true),
        PathNode("trib_a", "Tribuna A", LatLng(41.5698, 2.2600)),
        PathNode("trib_g", "Tribuna G", LatLng(41.5685, 2.2570)),
        PathNode("trib_h", "Tribuna H", LatLng(41.5680, 2.2562), hasStairs = true),
        
        // Zonas internas
        PathNode("fan_zone", "Fan Zone", LatLng(41.5690, 2.2595), hasShadow = true),
        PathNode("paddock", "Acceso Paddock", LatLng(41.5702, 2.2575)),
        PathNode("pit_lane", "Pit Lane Walkway", LatLng(41.5700, 2.2565)),
        PathNode("tower", "Torre de Control", LatLng(41.5705, 2.2570), isIndoor = true, hasShadow = true),
        
        // Servicios
        PathNode("food_main", "Zona Restauración Principal", LatLng(41.5692, 2.2590), hasShadow = true),
        PathNode("food_north", "Food Court Norte", LatLng(41.5698, 2.2578)),
        PathNode("wc_main", "Aseos Principales", LatLng(41.5691, 2.2583)),
        PathNode("wc_north", "Aseos Norte", LatLng(41.5701, 2.2582)),
        PathNode("merch", "Tienda Oficial", LatLng(41.5694, 2.2572), isIndoor = true, hasShadow = true),
        PathNode("medical", "Punto Médico", LatLng(41.5689, 2.2575)),
        
        // Curvas (zonas de espectadores)
        PathNode("curve_1", "Curva 1 (Elf)", LatLng(41.5710, 2.2600)),
        PathNode("curve_5", "Curva 5 (Seat)", LatLng(41.5665, 2.2620)),
        PathNode("curve_9", "Curva 9 (Campsa)", LatLng(41.5678, 2.2540)),
        
        // Cruces internos
        PathNode("cross_1", "Cruce Central", LatLng(41.5693, 2.2580)),
        PathNode("cross_2", "Cruce Norte", LatLng(41.5700, 2.2578)),
        PathNode("cross_3", "Cruce Sur", LatLng(41.5682, 2.2575)),
        PathNode("cross_4", "Cruce Este", LatLng(41.5690, 2.2605)),
        
        // Parkings
        PathNode("parking_n", "Parking Norte", LatLng(41.5715, 2.2555)),
        PathNode("parking_s", "Parking Sur", LatLng(41.5660, 2.2565)),
        
        // ── Última milla indoor (interior edificios / túneles / zonas cubiertas) ──
        PathNode("indoor_hospitality", "Hospitality Lounge", LatLng(41.5704, 2.2572), isIndoor = true, hasShadow = true),
        PathNode("indoor_media", "Centro de Medios", LatLng(41.5706, 2.2568), isIndoor = true, hasShadow = true),
        PathNode("indoor_paddock_hall", "Hall Paddock", LatLng(41.5703, 2.2573), isIndoor = true, hasShadow = true),
        PathNode("tunnel_south", "Túnel Sur (bajo pista)", LatLng(41.5685, 2.2580), isIndoor = true, hasShadow = true),
        PathNode("tunnel_north", "Túnel Norte (bajo pista)", LatLng(41.5705, 2.2580), isIndoor = true, hasShadow = true),
        PathNode("indoor_vip_box", "Palco VIP", LatLng(41.5696, 2.2586), isIndoor = true, hasShadow = true, hasStairs = true),
        PathNode("indoor_press_room", "Sala de Prensa", LatLng(41.5707, 2.2566), isIndoor = true, hasShadow = true),
        PathNode("indoor_museum", "Museo del Circuit", LatLng(41.5692, 2.2568), isIndoor = true, hasShadow = true),
        PathNode("parking_entrance_n", "Entrada Parking Norte", LatLng(41.5712, 2.2558)),
        PathNode("parking_entrance_s", "Entrada Parking Sur", LatLng(41.5663, 2.2567))
    ).associateBy { it.id }

    private val edges = listOf(
        // Acceso Principal → Cruce Central
        PathEdge("gate_main", "cross_1", 30.0, hasShadow = true),
        PathEdge("cross_1", "gate_main", 30.0, hasShadow = true),
        
        // Cruce Central → Servicios
        PathEdge("cross_1", "food_main", 25.0, hasShadow = true),
        PathEdge("food_main", "cross_1", 25.0, hasShadow = true),
        PathEdge("cross_1", "wc_main", 20.0),
        PathEdge("wc_main", "cross_1", 20.0),
        PathEdge("cross_1", "merch", 35.0, hasShadow = true),
        PathEdge("merch", "cross_1", 35.0, hasShadow = true),
        PathEdge("cross_1", "medical", 40.0),
        PathEdge("medical", "cross_1", 40.0),
        
        // Cruce Central → Tribunas
        PathEdge("cross_1", "trib_main", 45.0, hasShadow = true),
        PathEdge("trib_main", "cross_1", 45.0, hasShadow = true),
        PathEdge("cross_1", "trib_g", 60.0),
        PathEdge("trib_g", "cross_1", 60.0),
        PathEdge("cross_1", "fan_zone", 50.0, hasShadow = true),
        PathEdge("fan_zone", "cross_1", 50.0, hasShadow = true),
        
        // Cruce Norte
        PathEdge("cross_1", "cross_2", 40.0),
        PathEdge("cross_2", "cross_1", 40.0),
        PathEdge("cross_2", "paddock", 35.0),
        PathEdge("paddock", "cross_2", 35.0),
        PathEdge("cross_2", "pit_lane", 30.0),
        PathEdge("pit_lane", "cross_2", 30.0),
        PathEdge("cross_2", "tower", 40.0, hasShadow = true),
        PathEdge("tower", "cross_2", 40.0, hasShadow = true),
        PathEdge("cross_2", "food_north", 25.0),
        PathEdge("food_north", "cross_2", 25.0),
        PathEdge("cross_2", "wc_north", 20.0),
        PathEdge("wc_north", "cross_2", 20.0),
        PathEdge("cross_2", "gate_1", 55.0),
        PathEdge("gate_1", "cross_2", 55.0),
        PathEdge("cross_2", "curve_1", 80.0),
        PathEdge("curve_1", "cross_2", 80.0),
        
        // Cruce Sur
        PathEdge("cross_1", "cross_3", 50.0),
        PathEdge("cross_3", "cross_1", 50.0),
        PathEdge("cross_3", "trib_h", 45.0, hasStairs = true),
        PathEdge("trib_h", "cross_3", 45.0, hasStairs = true),
        PathEdge("cross_3", "gate_7", 60.0),
        PathEdge("gate_7", "cross_3", 60.0),
        PathEdge("cross_3", "curve_9", 90.0),
        PathEdge("curve_9", "cross_3", 90.0),
        PathEdge("cross_3", "parking_s", 120.0),
        PathEdge("parking_s", "cross_3", 120.0),
        
        // Cruce Este
        PathEdge("cross_1", "cross_4", 55.0),
        PathEdge("cross_4", "cross_1", 55.0),
        PathEdge("cross_4", "trib_a", 40.0),
        PathEdge("trib_a", "cross_4", 40.0),
        PathEdge("cross_4", "gate_3", 50.0),
        PathEdge("gate_3", "cross_4", 50.0),
        PathEdge("cross_4", "curve_5", 100.0),
        PathEdge("curve_5", "cross_4", 100.0),
        
        // Tribuna Principal → Fan Zone (directo)
        PathEdge("trib_main", "fan_zone", 35.0, hasShadow = true),
        PathEdge("fan_zone", "trib_main", 35.0, hasShadow = true),
        
        // Parking Norte
        PathEdge("cross_2", "parking_n", 100.0),
        PathEdge("parking_n", "cross_2", 100.0),
        
        // Alternativa sin escaleras a Tribuna H
        PathEdge("cross_3", "trib_g", 55.0),
        PathEdge("trib_g", "cross_3", 55.0),
        PathEdge("trib_g", "trib_h", 40.0), // Rodeo sin escaleras
        PathEdge("trib_h", "trib_g", 40.0),
        
        // ── Última milla indoor: túneles, edificios, conexiones interiores ──
        
        // Túnel Sur (conecta zona grada con zona interior sin cruzar pista)
        PathEdge("cross_3", "tunnel_south", 30.0, hasShadow = true),
        PathEdge("tunnel_south", "cross_3", 30.0, hasShadow = true),
        PathEdge("tunnel_south", "fan_zone", 40.0, hasShadow = true),
        PathEdge("fan_zone", "tunnel_south", 40.0, hasShadow = true),
        
        // Túnel Norte (conexión paddock ↔ zona espectadores)
        PathEdge("cross_2", "tunnel_north", 25.0, hasShadow = true),
        PathEdge("tunnel_north", "cross_2", 25.0, hasShadow = true),
        PathEdge("tunnel_north", "paddock", 20.0, hasShadow = true),
        PathEdge("paddock", "tunnel_north", 20.0, hasShadow = true),
        
        // Hospitality / Hall Paddock / Media
        PathEdge("paddock", "indoor_paddock_hall", 15.0, hasShadow = true),
        PathEdge("indoor_paddock_hall", "paddock", 15.0, hasShadow = true),
        PathEdge("indoor_paddock_hall", "indoor_hospitality", 20.0, hasShadow = true),
        PathEdge("indoor_hospitality", "indoor_paddock_hall", 20.0, hasShadow = true),
        PathEdge("indoor_paddock_hall", "indoor_media", 25.0, hasShadow = true),
        PathEdge("indoor_media", "indoor_paddock_hall", 25.0, hasShadow = true),
        PathEdge("indoor_media", "indoor_press_room", 15.0, hasShadow = true),
        PathEdge("indoor_press_room", "indoor_media", 15.0, hasShadow = true),
        PathEdge("tower", "indoor_press_room", 20.0, hasShadow = true),
        PathEdge("indoor_press_room", "tower", 20.0, hasShadow = true),
        
        // Palco VIP (desde tribuna principal, con escaleras)
        PathEdge("trib_main", "indoor_vip_box", 25.0, hasStairs = true, hasShadow = true),
        PathEdge("indoor_vip_box", "trib_main", 25.0, hasStairs = true, hasShadow = true),
        
        // Museo del Circuit
        PathEdge("merch", "indoor_museum", 30.0, hasShadow = true),
        PathEdge("indoor_museum", "merch", 30.0, hasShadow = true),
        PathEdge("cross_1", "indoor_museum", 45.0),
        PathEdge("indoor_museum", "cross_1", 45.0),
        
        // Entradas de parking → nodos de parking
        PathEdge("parking_n", "parking_entrance_n", 25.0),
        PathEdge("parking_entrance_n", "parking_n", 25.0),
        PathEdge("parking_entrance_n", "cross_2", 80.0),
        PathEdge("cross_2", "parking_entrance_n", 80.0),
        PathEdge("parking_s", "parking_entrance_s", 25.0),
        PathEdge("parking_entrance_s", "parking_s", 25.0),
        PathEdge("parking_entrance_s", "cross_3", 95.0),
        PathEdge("cross_3", "parking_entrance_s", 95.0)
    )

    // Adyacencia
    private val adjacency: Map<String, List<PathEdge>> by lazy {
        edges.groupBy { it.from }
    }

    // ── Congestion dinámica ──

    private val congestionMap = mutableMapOf<String, Double>() // nodeId → factor (1.0 = normal)

    fun updateCongestion(nodeId: String, factor: Double) {
        congestionMap[nodeId] = factor.coerceIn(0.5, 3.0)
        Log.d(TAG, "📊 Congestion updated: $nodeId → $factor")
    }

    fun clearCongestion() {
        congestionMap.clear()
    }

    // ── Algoritmo A* ──

    /**
     * Calcula la ruta peatonal óptima entre dos puntos del circuito.
     *
     * @param fromId ID del nodo origen
     * @param toId ID del nodo destino
     * @param avoidStairs Evitar tramos con escaleras (accesibilidad)
     * @param preferShadow Preferir rutas con sombra (activo automáticamente 12-16h)
     * @param useDynamicWeights Aplicar pesos de congestión dinámicos
     */
    fun findRoute(
        fromId: String,
        toId: String,
        avoidStairs: Boolean = false,
        preferShadow: Boolean = false,
        useDynamicWeights: Boolean = true
    ): PathResult? {
        val start = nodes[fromId] ?: return null
        val goal = nodes[toId] ?: return null

        Log.d(TAG, "🔍 A* pathfinding: ${start.name} → ${goal.name} (stairs=${!avoidStairs}, shadow=$preferShadow)")

        // A* con PriorityQueue
        data class AStarNode(
            val nodeId: String,
            val gCost: Double, // Coste real acumulado
            val fCost: Double, // gCost + heurística
            val parent: String?
        ) : Comparable<AStarNode> {
            override fun compareTo(other: AStarNode) = fCost.compareTo(other.fCost)
        }

        val openSet = PriorityQueue<AStarNode>()
        val closedSet = mutableSetOf<String>()
        val cameFrom = mutableMapOf<String, String>()
        val gScore = mutableMapOf<String, Double>()

        gScore[fromId] = 0.0
        openSet.add(AStarNode(fromId, 0.0, heuristic(start.position, goal.position), null))

        while (openSet.isNotEmpty()) {
            val current = openSet.poll() ?: break

            if (current.nodeId == toId) {
                // Reconstruir ruta
                return reconstructPath(current.nodeId, cameFrom, gScore[toId] ?: 0.0, avoidStairs, preferShadow)
            }

            if (current.nodeId in closedSet) continue
            closedSet.add(current.nodeId)

            val neighbors = adjacency[current.nodeId] ?: continue

            for (edge in neighbors) {
                if (edge.to in closedSet) continue

                // Filtrar escaleras si es accesible
                if (avoidStairs && edge.hasStairs) continue

                // Calcular coste del edge
                var edgeCost = edge.distanceMeters

                // Aplicar peso de congestión
                if (useDynamicWeights) {
                    val congestion = congestionMap[edge.to] ?: edge.congestionWeight
                    edgeCost *= congestion
                }

                // Penalizar rutas sin sombra si se prefiere sombra
                if (preferShadow && !edge.hasShadow) {
                    edgeCost *= 1.4 // 40% penalización a cielo abierto
                }

                val tentativeG = (gScore[current.nodeId] ?: Double.MAX_VALUE) + edgeCost

                if (tentativeG < (gScore[edge.to] ?: Double.MAX_VALUE)) {
                    cameFrom[edge.to] = current.nodeId
                    gScore[edge.to] = tentativeG

                    val goalNode = nodes[toId]!!
                    val neighborNode = nodes[edge.to]!!
                    val fScore = tentativeG + heuristic(neighborNode.position, goalNode.position)

                    openSet.add(AStarNode(edge.to, tentativeG, fScore, current.nodeId))
                }
            }
        }

        Log.w(TAG, "❌ No se encontró ruta de $fromId a $toId")
        return null
    }

    /**
     * Encuentra la ruta óptima desde una coordenada GPS arbitraria.
     * Primero busca el nodo más cercano al usuario.
     */
    fun findRouteFromGps(
        userPosition: LatLng,
        toId: String,
        avoidStairs: Boolean = false,
        preferShadow: Boolean = false
    ): PathResult? {
        val nearestNode = findNearestNode(userPosition) ?: return null
        return findRoute(nearestNode.id, toId, avoidStairs, preferShadow)
    }

    /**
     * Encuentra el nodo del grafo más cercano a una coordenada GPS.
     */
    fun findNearestNode(position: LatLng): PathNode? {
        return nodes.values.minByOrNull { node ->
            position.distanceTo(node.position)
        }
    }

    /**
     * Devuelve todos los nodos del grafo (para mostrar en mapa).
     */
    fun getAllNodes(): List<PathNode> = nodes.values.toList()

    /**
     * Devuelve todos los edges (para dibujar en mapa).
     */
    fun getAllEdges(): List<PathEdge> = edges

    /**
     * Devuelve los nodos indoor (última milla, interiores, túneles).
     */
    fun getIndoorNodes(): List<PathNode> = nodes.values.filter { it.isIndoor }

    /**
     * Guía "Última Milla" — Ruta desde el parking hasta el destino final,
     * priorizando caminos cubiertos (indoor) y sombreados.
     *
     * @param parkingId ID del parking de origen (ej: "parking_n", "parking_s")
     * @param destinationId ID del destino final (ej: "trib_main", "fan_zone", "indoor_vip_box")
     * @param avoidStairs Evitar escaleras
     */
    fun findLastMileRoute(
        parkingId: String,
        destinationId: String,
        avoidStairs: Boolean = false
    ): PathResult? {
        Log.d(TAG, "🚶 Última milla: parking=$parkingId → destino=$destinationId")
        // La última milla siempre prioriza sombra/indoor
        return findRoute(
            fromId = parkingId,
            toId = destinationId,
            avoidStairs = avoidStairs,
            preferShadow = true, // Siempre priorizar rutas cubiertas
            useDynamicWeights = true
        )
    }

    /**
     * Guía "Última Milla" desde coordenadas GPS (ej: GPS del coche en el parking).
     */
    fun findLastMileRouteFromGps(
        carPosition: LatLng,
        destinationId: String,
        avoidStairs: Boolean = false
    ): PathResult? {
        val nearestParking = nodes.values
            .filter { it.id.startsWith("parking") }
            .minByOrNull { it.position.distanceTo(carPosition) }
            ?: return findRouteFromGps(carPosition, destinationId, avoidStairs, preferShadow = true)
        
        return findLastMileRoute(nearestParking.id, destinationId, avoidStairs)
    }

    // ── Helpers ──

    private fun heuristic(a: LatLng, b: LatLng): Double {
        return a.distanceTo(b) // Distancia en metros (Haversine)
    }

    private fun reconstructPath(
        goalId: String,
        cameFrom: Map<String, String>,
        totalCost: Double,
        usedAccessible: Boolean,
        usedShadow: Boolean
    ): PathResult {
        val path = mutableListOf(goalId)
        var current = goalId
        while (cameFrom.containsKey(current)) {
            current = cameFrom[current]!!
            path.add(0, current)
        }

        val pathNodes = path.mapNotNull { nodes[it] }
        val steps = mutableListOf<PedestrianStep>()

        for (i in 0 until pathNodes.size - 1) {
            val from = pathNodes[i]
            val to = pathNodes[i + 1]
            val dist = from.position.distanceTo(to.position)
            val bearing = calculateBearing(from.position, to.position)
            val direction = bearingToDirection(bearing)

            steps.add(
                PedestrianStep(
                    instruction = "Dirígete $direction hacia ${to.name} (${dist.toInt()}m)",
                    distance = dist,
                    fromNode = from,
                    toNode = to
                )
            )
        }

        val totalDistance = pathNodes.zipWithNext().sumOf { (a, b) -> a.position.distanceTo(b.position) }
        val walkingSpeedMps = 4.5 / 3.6 // 4.5 km/h → m/s
        val estimatedTime = totalCost / walkingSpeedMps

        Log.i(TAG, "✅ Ruta encontrada: ${pathNodes.size} nodos, ${totalDistance.toInt()}m, ${(estimatedTime / 60).toInt()} min")

        return PathResult(
            nodes = pathNodes,
            totalDistance = totalDistance,
            estimatedTimeSeconds = estimatedTime,
            steps = steps,
            usedAccessibleRoute = usedAccessible,
            usedShadowRoute = usedShadow
        )
    }

    private fun calculateBearing(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    private fun bearingToDirection(bearing: Double): String = when {
        bearing < 22.5 || bearing >= 337.5 -> "al norte"
        bearing < 67.5 -> "al noreste"
        bearing < 112.5 -> "al este"
        bearing < 157.5 -> "al sureste"
        bearing < 202.5 -> "al sur"
        bearing < 247.5 -> "al suroeste"
        bearing < 292.5 -> "al oeste"
        else -> "al noroeste"
    }
}
