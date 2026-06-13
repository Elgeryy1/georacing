package com.georacing.georacing.car

import android.location.Location
import android.util.Log
import androidx.car.app.navigation.model.Maneuver
import com.georacing.georacing.car.config.OsrmConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.max
import kotlin.math.roundToLong

data class RouteStep(
    val instruction: String,
    val road: String,
    val maneuverType: Int,
    val distanceMeters: Double,
    val target: Location,
    val estimatedTimeSeconds: Long
)

data class SimulatedRoute(
    val destination: DestinationModel,
    val startLocation: Location,
    val steps: List<RouteStep>,
    val totalDistanceMeters: Double,
    val totalTimeSeconds: Long
)

/**
 * Route planner using REAL OSRM routing API.
 * Falls back to straight-line estimation if OSRM is unreachable.
 */
class RoutePlanner {

    companion object {
        private const val TAG = "RoutePlanner"
    }

    /**
     * Plans a route using OSRM from the user's current location to the destination.
     * If OSRM is unavailable, creates a single direct step with estimated distance/time.
     */
    suspend fun planRoute(destination: DestinationModel, startLat: Double? = null, startLon: Double? = null): SimulatedRoute {
        val originLat = startLat ?: 41.5489 // Default: near Montmeló station
        val originLon = startLon ?: 2.2538
        val start = locationOf(originLat, originLon)
        val destLocation = locationOf(destination.latitude, destination.longitude)

        return try {
            planRouteOsrm(destination, start, destLocation)
        } catch (e: Exception) {
            Log.w(TAG, "OSRM no disponible, usando ruta directa: ${e.message}")
            planRouteDirect(destination, start, destLocation)
        }
    }

    /**
     * Non-suspend version for Android Auto compatibility
     */
    fun planRoute(destination: DestinationModel): SimulatedRoute {
        val start = locationOf(41.5489, 2.2538)
        val destLocation = locationOf(destination.latitude, destination.longitude)
        return planRouteDirect(destination, start, destLocation)
    }

    private suspend fun planRouteOsrm(
        destination: DestinationModel,
        start: Location,
        destLocation: Location
    ): SimulatedRoute {
        val baseUrl = OsrmConfig.getBaseUrl()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(OsrmService::class.java)

        val coordinates = "${start.longitude},${start.latitude};${destLocation.longitude},${destLocation.latitude}"
        val response = service.getRoute(coordinates = coordinates, steps = true)

        if (response.code != "Ok" || response.routes.isEmpty()) {
            throw Exception("OSRM returned code=${response.code}")
        }

        val route = response.routes.first()
        val osrmSteps = route.legs.flatMap { it.steps }

        val steps = osrmSteps.map { step ->
            val maneuverLocation = locationOf(
                step.maneuver.location[1], // lat
                step.maneuver.location[0]  // lon
            )
            RouteStep(
                instruction = buildInstruction(step),
                road = step.name.ifBlank { "la carretera" },
                maneuverType = osrmToCarManeuver(step.maneuver.type, step.maneuver.modifier),
                distanceMeters = step.distance,
                target = maneuverLocation,
                estimatedTimeSeconds = step.duration.toLong()
            )
        }

        Log.i(TAG, "✅ OSRM route: ${steps.size} steps, ${route.distance.toInt()}m, ${route.duration.toInt()}s")

        return SimulatedRoute(
            destination = destination,
            startLocation = start,
            steps = steps,
            totalDistanceMeters = route.distance,
            totalTimeSeconds = max(route.duration.toLong(), 180L)
        )
    }

    private fun planRouteDirect(
        destination: DestinationModel,
        start: Location,
        destLocation: Location
    ): SimulatedRoute {
        val distance = distanceBetween(start, destLocation)
        val timeSeconds = max(45L, (distance / 13.8).roundToLong()) // ~50 km/h

        val steps = listOf(
            RouteStep(
                instruction = "Dirígete hacia ${destination.name}",
                road = "Ruta directa",
                maneuverType = Maneuver.TYPE_DEPART,
                distanceMeters = distance,
                target = destLocation,
                estimatedTimeSeconds = timeSeconds
            )
        )

        return SimulatedRoute(
            destination = destination,
            startLocation = start,
            steps = steps,
            totalDistanceMeters = distance,
            totalTimeSeconds = max(timeSeconds, 180L)
        )
    }

    private fun buildInstruction(step: com.georacing.georacing.car.Step): String {
        val maneuver = step.maneuver
        val road = step.name.ifBlank { "la carretera" }
        return when (maneuver.type) {
            "depart" -> "Sal por $road"
            "arrive" -> "Has llegado a ${road.ifBlank { "tu destino" }}"
            "turn" -> "${maneuver.modifier?.replaceFirstChar { it.uppercase() } ?: "Gira"} hacia $road"
            "new name" -> "Continúa por $road"
            "merge" -> "Incorpórate a $road"
            "on ramp" -> "Toma la incorporación a $road"
            "off ramp" -> "Toma la salida hacia $road"
            "fork" -> "Mantente ${maneuver.modifier ?: "recto"} en la bifurcación"
            "roundabout", "rotary" -> "En la rotonda, toma la salida ${maneuver.exit ?: ""}"
            else -> "Continúa por $road"
        }
    }

    private fun osrmToCarManeuver(type: String, modifier: String?): Int = when (type) {
        "depart" -> Maneuver.TYPE_DEPART
        "arrive" -> Maneuver.TYPE_DESTINATION
        "turn" -> when (modifier) {
            "left" -> Maneuver.TYPE_TURN_NORMAL_LEFT
            "right" -> Maneuver.TYPE_TURN_NORMAL_RIGHT
            "slight left" -> Maneuver.TYPE_TURN_SLIGHT_LEFT
            "slight right" -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
            "sharp left" -> Maneuver.TYPE_TURN_SHARP_LEFT
            "sharp right" -> Maneuver.TYPE_TURN_SHARP_RIGHT
            else -> Maneuver.TYPE_STRAIGHT
        }
        "merge" -> Maneuver.TYPE_MERGE_SIDE_UNSPECIFIED
        "on ramp" -> Maneuver.TYPE_ON_RAMP_NORMAL_RIGHT
        "off ramp" -> when (modifier) {
            "left" -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
            else -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
        }
        "fork" -> when (modifier) {
            "left" -> Maneuver.TYPE_FORK_LEFT
            else -> Maneuver.TYPE_FORK_RIGHT
        }
        "roundabout", "rotary" -> Maneuver.TYPE_ROUNDABOUT_ENTER_CW
        else -> Maneuver.TYPE_STRAIGHT
    }

    private fun distanceBetween(from: Location, to: Location): Double {
        val result = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, result)
        return result.first().toDouble()
    }

    private fun locationOf(lat: Double, lon: Double): Location =
        Location("osrm").apply {
            latitude = lat
            longitude = lon
        }
}
