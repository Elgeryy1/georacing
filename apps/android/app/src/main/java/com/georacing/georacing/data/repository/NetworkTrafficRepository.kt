package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.model.RouteTraffic
import com.georacing.georacing.domain.model.RouteTrafficStatus
import com.georacing.georacing.domain.model.ZoneOccupancy
import com.georacing.georacing.domain.model.ZoneOccupancyStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repositorio real que lee rutas y zonas de tráfico desde el backend.
 * Tablas: "routes" y "zone_traffic" en alpo.myqnapcloud.com:4010/api
 * 
 * Sustituye a FakeCrowdRepository — datos reales gestionados desde Panel Metropolis.
 */
class NetworkTrafficRepository {

    private val TAG = "NetworkTrafficRepo"
    private val api = FirestoreLikeClient.api

    /**
     * Lee todas las rutas del circuito con su estado de tráfico.
     */
    suspend fun getRoutes(): List<RouteTraffic> {
        return try {
            val data = api.read("routes")
            data.map { row ->
                RouteTraffic(
                    id = (row["id"] as? String) ?: (row["route_id"] as? String) ?: "",
                    name = (row["name"] as? String) ?: "",
                    origin = (row["origin"] as? String) ?: "",
                    destination = (row["destination"] as? String) ?: "",
                    status = RouteTrafficStatus.fromString(row["status"] as? String),
                    activeUsers = ((row["active_users"] ?: row["activeUsers"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    capacity = (row["capacity"]?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    capacityPercentage = ((row["capacity_percentage"] ?: row["capacityPercentage"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    averageSpeed = ((row["average_speed"] ?: row["averageSpeed"])?.toString()?.toDoubleOrNull()) ?: 0.0,
                    distance = (row["distance"]?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    signalQuality = ((row["signal_quality"] ?: row["signalQuality"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    estimatedTime = ((row["estimated_time"] ?: row["estimatedTime"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    velocity = (row["velocity"]?.toString()?.toDoubleOrNull()) ?: 0.0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching routes", e)
            emptyList()
        }
    }

    /**
     * Lee todas las zonas del circuito con ocupación en tiempo real.
     */
    suspend fun getZoneTraffic(): List<ZoneOccupancy> {
        return try {
            val data = api.read("zone_traffic")
            data.map { row ->
                ZoneOccupancy(
                    id = (row["id"] as? String) ?: (row["zone_id"] as? String) ?: "",
                    name = (row["name"] as? String) ?: "",
                    type = (row["type"] as? String) ?: "GRADA",
                    status = ZoneOccupancyStatus.fromString(row["status"] as? String),
                    capacity = (row["capacity"]?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    currentOccupancy = ((row["current_occupancy"] ?: row["currentOccupancy"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    temperature = (row["temperature"]?.toString()?.toDoubleOrNull()) ?: 0.0,
                    waitTime = ((row["wait_time"] ?: row["waitTime"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    entryRate = ((row["entry_rate"] ?: row["entryRate"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0,
                    exitRate = ((row["exit_rate"] ?: row["exitRate"])?.toString()?.toDoubleOrNull()?.toInt()) ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching zone_traffic", e)
            emptyList()
        }
    }

    /**
     * Flow que emite rutas periódicamente (para UI observables).
     */
    fun observeRoutes(intervalMs: Long = 15000): Flow<List<RouteTraffic>> = flow {
        while (true) {
            emit(getRoutes())
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Flow que emite zonas de tráfico periódicamente.
     */
    fun observeZoneTraffic(intervalMs: Long = 15000): Flow<List<ZoneOccupancy>> = flow {
        while (true) {
            emit(getZoneTraffic())
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Convierte datos de zona a HeatPoints para el mapa de calor.
     * Sustituye a FakeCrowdRepository.getHeatPoints().
     */
    fun getHeatPointsFromZones(zones: List<ZoneOccupancy>): List<HeatPoint> {
        // Coordenadas aproximadas de cada zona del Circuit de Barcelona-Catalunya
        val zoneCoordinates = mapOf(
            "grada-t1-recta-principal" to Pair(41.5695, 2.2585),
            "grada-t2-curva-ascari" to Pair(41.5678, 2.2610),
            "fan-zone-principal" to Pair(41.5690, 2.2595),
            "paddock-vip-boxes" to Pair(41.5702, 2.2575),
            "vial-acceso-a-norte" to Pair(41.5710, 2.2560),
            "grada-t3-chicane" to Pair(41.5685, 2.2555),
            "fan-zone-tecnologica" to Pair(41.5688, 2.2605)
        )

        return zones.mapNotNull { zone ->
            val coords = zoneCoordinates[zone.id] ?: return@mapNotNull null
            val intensity = (zone.occupancyPercentage / 100.0f).coerceIn(0f, 1f)
            val radius = when (zone.type) {
                "GRADA" -> 120f
                "FANZONE" -> 100f
                "PADDOCK" -> 80f
                "VIAL" -> 60f
                else -> 100f
            }
            HeatPoint(
                lat = coords.first,
                lon = coords.second,
                intensity = intensity,
                radius = radius
            )
        }
    }
}
