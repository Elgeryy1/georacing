package com.georacing.georacing.car

import com.google.gson.annotations.SerializedName

// OSRM Models (fallback)
data class OsrmResponse(
    @SerializedName("routes") val routes: List<Route>,
    @SerializedName("code") val code: String
)

data class Route(
    @SerializedName("geometry") val geometry: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("distance") val distance: Double,
    @SerializedName("legs") val legs: List<Leg>
)

data class Leg(
    @SerializedName("steps") val steps: List<Step>,
    @SerializedName("annotation") val annotation: LegAnnotation? = null
)

/**
 * Anotaciones por segmento de la ruta OSRM.
 * Se obtienen con ?annotations=speed,maxspeed,duration,distance
 */
data class LegAnnotation(
    @SerializedName("speed") val speed: List<Double>? = null,           // m/s por segmento
    @SerializedName("duration") val duration: List<Double>? = null,     // segundos por segmento
    @SerializedName("distance") val distance: List<Double>? = null,     // metros por segmento
    @SerializedName("maxspeed") val maxspeed: List<MaxSpeedEntry>? = null // límite de velocidad OSM
)

data class MaxSpeedEntry(
    @SerializedName("speed") val speed: Int? = null,        // km/h (null si desconocido)
    @SerializedName("unit") val unit: String? = null,       // "km/h" o "mph"
    @SerializedName("unknown") val unknown: Boolean? = null, // true si no hay dato OSM
    @SerializedName("none") val none: Boolean? = null       // true si no hay límite
)

data class Step(
    @SerializedName("geometry") val geometry: String, // Step segment geometry
    @SerializedName("maneuver") val maneuver: Maneuver,
    @SerializedName("name") val name: String,
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double
)

data class Maneuver(
    @SerializedName("type") val type: String, // e.g., "turn", "new name", "depart", "arrive", "roundabout"
    @SerializedName("modifier") val modifier: String?, // e.g., "left", "right", "slight right"
    @SerializedName("location") val location: List<Double>, // [lon, lat]
    @SerializedName("exit") val exit: Int? = null // FASE 2.2: Número de salida en rotondas (1, 2, 3...)
)

// GraphHopper Models (con tráfico)
data class GraphHopperResponse(
    @SerializedName("paths") val paths: List<GraphHopperPath>
)

data class GraphHopperPath(
    @SerializedName("distance") val distance: Double,
    @SerializedName("time") val time: Long, // en milisegundos
    @SerializedName("points") val points: GraphHopperGeometry,
    @SerializedName("instructions") val instructions: List<GraphHopperInstruction>
)

data class GraphHopperGeometry(
    @SerializedName("coordinates") val coordinates: List<List<Double>> // [[lon, lat], ...]
)

data class GraphHopperInstruction(
    @SerializedName("text") val text: String,
    @SerializedName("distance") val distance: Double,
    @SerializedName("time") val time: Long,
    @SerializedName("sign") val sign: Int, // código de maniobra
    @SerializedName("street_name") val streetName: String?
)
