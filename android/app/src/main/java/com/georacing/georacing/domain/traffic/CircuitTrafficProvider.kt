package com.georacing.georacing.domain.traffic

import android.location.Location
import android.util.Log
import com.georacing.georacing.data.repository.NetworkTrafficRepository
import com.georacing.georacing.domain.model.ZoneOccupancy
import com.georacing.georacing.domain.navigation.PedestrianPathfinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Proveedor de tráfico real basado en datos del backend (tabla "zone_traffic").
 * 
 * Lee datos de ocupación gestionados desde Panel Metropolis y convierte
 * la ocupación de zonas en factores de congestión para el pathfinding A*.
 * 
 * Sustituye la versión anterior que usaba ScenarioSimulator (datos simulados).
 */
class CircuitTrafficProvider : TrafficProvider {

    private val TAG = "CircuitTrafficProvider"
    private val repository = NetworkTrafficRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Zonas del circuito con coordenadas y nodos del grafo A*
    private data class ZoneMapping(
        val zoneId: String,
        val centerLat: Double,
        val centerLon: Double,
        val radiusMeters: Double,
        val graphNodeIds: List<String>,
        var congestionFactor: Double = 1.0
    )

    private val zoneMappings = listOf(
        ZoneMapping("grada-t1-recta-principal", 41.5695, 2.2585, 100.0,
            listOf("trib_main", "trib_a", "cross_1")),
        ZoneMapping("fan-zone-principal", 41.5690, 2.2595, 80.0,
            listOf("fan_zone", "cross_4", "food_main")),
        ZoneMapping("paddock-vip-boxes", 41.5702, 2.2575, 70.0,
            listOf("paddock", "pit_lane", "tower", "cross_2")),
        ZoneMapping("grada-t2-curva-ascari", 41.5678, 2.2610, 100.0,
            listOf("curve_1", "curve_5")),
        ZoneMapping("vial-acceso-a-norte", 41.5710, 2.2560, 50.0,
            listOf("gate_main", "gate_1", "gate_3", "gate_7")),
        ZoneMapping("grada-t3-chicane", 41.5685, 2.2555, 150.0,
            listOf("parking_n", "parking_s", "cross_3")),
        ZoneMapping("fan-zone-tecnologica", 41.5688, 2.2605, 80.0,
            listOf("curve_9", "food_north", "wc_main"))
    )

    // Último estado conocido de zonas
    private val _zones = MutableStateFlow<List<ZoneOccupancy>>(emptyList())
    val zones = _zones.asStateFlow()

    init {
        // Iniciar polling de zonas del backend
        scope.launch {
            repository.observeZoneTraffic(15000).collect { zoneList ->
                _zones.value = zoneList
                updateCongestionFromZones(zoneList)
            }
        }
    }

    /**
     * Actualiza factores de congestión basándose en datos reales del backend.
     */
    private fun updateCongestionFromZones(zoneList: List<ZoneOccupancy>) {
        zoneMappings.forEach { mapping ->
            val zone = zoneList.find { it.id == mapping.zoneId }
            if (zone != null) {
                mapping.congestionFactor = zone.congestionFactor

                // Propagar al grafo A*
                mapping.graphNodeIds.forEach { nodeId ->
                    PedestrianPathfinder.updateCongestion(nodeId, mapping.congestionFactor)
                }
            }
        }

        Log.d(TAG, "📊 Congestion from backend: " +
                zoneMappings.joinToString { "${it.zoneId}=${String.format("%.1f", it.congestionFactor)}" })
    }

    /**
     * Fuerza una recarga inmediata de datos del backend.
     */
    suspend fun refreshNow() {
        val zoneList = repository.getZoneTraffic()
        _zones.value = zoneList
        updateCongestionFromZones(zoneList)
    }

    override fun getTrafficFactor(location: Location): Double {
        // Encontrar la zona más cercana
        val nearestZone = zoneMappings.minByOrNull { mapping ->
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                mapping.centerLat, mapping.centerLon,
                results
            )
            results[0].toDouble()
        }

        return nearestZone?.congestionFactor ?: 1.0
    }

    override fun isAvailable(): Boolean = true

    override fun getTrafficDescription(location: Location): String {
        val factor = getTrafficFactor(location)
        return when {
            factor < 0.9 -> "🟢 Tráfico fluido"
            factor < 1.2 -> "🟢 Tráfico normal"
            factor < 1.5 -> "🟡 Zona concurrida"
            factor < 2.0 -> "🟠 Zona congestionada"
            else -> "🔴 Zona muy congestionada"
        }
    }

    override fun getTrafficFactorForSegment(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double
    ): Double {
        val startLocation = Location("").apply {
            latitude = startLat
            longitude = startLon
        }
        val endLocation = Location("").apply {
            latitude = endLat
            longitude = endLon
        }

        val startFactor = getTrafficFactor(startLocation)
        val endFactor = getTrafficFactor(endLocation)

        return (startFactor + endFactor) / 2.0
    }
}
