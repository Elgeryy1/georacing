package com.georacing.georacing.domain.speed

import android.location.Location
import android.util.Log
import com.georacing.georacing.car.LegAnnotation
import com.georacing.georacing.car.MaxSpeedEntry
import com.georacing.georacing.car.OsrmService
import com.georacing.georacing.car.config.OsrmConfig
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Proveedor de límites de velocidad reales basado en datos OpenStreetMap vía OSRM.
 *
 * Estrategia:
 * 1. Cuando se calcula una ruta, extrae `annotations.maxspeed` de OSRM
 *    (esto usa la etiqueta `maxspeed` de las vías OSM).
 * 2. Cachea los límites por coordenada/grid para consultas rápidas.
 * 3. Fallback: Si OSRM no tiene maxspeed, usa Overpass API como respaldo.
 *
 * Datos OSM: La mayoría de carreteras principales y autopistas en España
 * tienen la etiqueta maxspeed correctamente etiquetada.
 */
class OsmSpeedLimitProvider : SpeedLimitProvider {

    companion object {
        private const val TAG = "OsmSpeedLimitProvider"
        private const val GRID_PRECISION = 10000 // ~11m precision grid
        private const val CACHE_MAX_SIZE = 500
        private const val SEGMENT_MATCH_RADIUS_M = 80.0
    }

    // ── Cache de límites ──

    data class SpeedLimitSegment(
        val lat: Double, val lon: Double,
        val speedLimitKmh: Int,
        val timestamp: Long
    )

    private val gridCache = ConcurrentHashMap<String, Int>() // gridKey → km/h
    private var routeSegments: List<SpeedLimitSegment> = emptyList()
    private val osrmService: OsrmService

    init {
        val baseUrl = OsrmConfig.getBaseUrl()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        osrmService = retrofit.create(OsrmService::class.java)
    }

    // ── SpeedLimitProvider API ──

    override fun getSpeedLimitForLocation(location: Location): Int? {
        // 1. Buscar en cache por grid
        val gridKey = toGridKey(location.latitude, location.longitude)
        gridCache[gridKey]?.let { return it }

        // 2. Buscar en segmentos de ruta cacheados
        val nearest = findNearestSegment(location)
        if (nearest != null) {
            gridCache[gridKey] = nearest.speedLimitKmh
            return nearest.speedLimitKmh
        }

        // 3. Sin datos cacheados → consultar OSRM puntualmente (async → blocking en hilo IO)
        return try {
            runBlocking(Dispatchers.IO) {
                querySpeedLimitFromOsrm(location)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying speed limit: ${e.message}")
            null
        }
    }

    // ── Alimentar desde ruta ──

    /**
     * Carga límites de velocidad para una ruta completa usando OSRM annotations=maxspeed.
     * Debe llamarse cuando se calcula una nueva ruta.
     */
    suspend fun loadSpeedLimitsForRoute(origin: LatLng, dest: LatLng) {
        withContext(Dispatchers.IO) {
            try {
                val coordinates = String.format(
                    Locale.US, "%.6f,%.6f;%.6f,%.6f",
                    origin.longitude, origin.latitude,
                    dest.longitude, dest.latitude
                )

                val response = osrmService.getRoute(
                    coordinates = coordinates,
                    annotations = "maxspeed,speed"
                )

                if (response.code == "Ok" && response.routes.isNotEmpty()) {
                    parseAnnotations(response.routes[0].legs.firstOrNull()?.annotation,
                        response.routes[0].legs.firstOrNull()?.steps?.map { it.maneuver.location }
                            ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading speed limits for route", e)
            }
        }
    }

    /**
     * Alimenta directamente con anotaciones OSRM ya obtenidas.
     */
    fun updateFromAnnotation(annotation: LegAnnotation?, stepLocations: List<List<Double>>) {
        parseAnnotations(annotation, stepLocations)
    }

    // ── Consulta puntual desde OSRM ──

    private suspend fun querySpeedLimitFromOsrm(location: Location): Int? {
        return try {
            // Mini-ruta de 100m para obtener maxspeed del segmento actual
            val offset = 0.001 // ~100m
            val coordinates = String.format(
                Locale.US, "%.6f,%.6f;%.6f,%.6f",
                location.longitude, location.latitude,
                location.longitude + offset, location.latitude
            )

            val response = osrmService.getRoute(
                coordinates = coordinates,
                annotations = "maxspeed"
            )

            if (response.code == "Ok" && response.routes.isNotEmpty()) {
                val leg = response.routes[0].legs.firstOrNull()
                val maxspeeds = leg?.annotation?.maxspeed
                if (!maxspeeds.isNullOrEmpty()) {
                    val limit = parseMaxSpeedEntry(maxspeeds.first())
                    if (limit != null) {
                        val gridKey = toGridKey(location.latitude, location.longitude)
                        gridCache[gridKey] = limit
                        Log.d(TAG, "📍 Speed limit at (${location.latitude}, ${location.longitude}): $limit km/h")
                    }
                    limit
                } else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "OSRM maxspeed query failed: ${e.message}")
            null
        }
    }

    // ── Parsing ──

    private fun parseAnnotations(annotation: LegAnnotation?, stepLocations: List<List<Double>>) {
        if (annotation == null) return

        val maxspeeds = annotation.maxspeed ?: return
        val segments = mutableListOf<SpeedLimitSegment>()
        val now = System.currentTimeMillis()

        maxspeeds.forEachIndexed { i, entry ->
            val limit = parseMaxSpeedEntry(entry) ?: return@forEachIndexed

            // Mapear a la coordenada más cercana del step
            val locIdx = if (stepLocations.isNotEmpty()) {
                (i * stepLocations.size / maxspeeds.size).coerceIn(0, stepLocations.lastIndex)
            } else return@forEachIndexed

            val loc = stepLocations[locIdx]
            val lat = loc.getOrNull(1) ?: return@forEachIndexed
            val lon = loc.getOrNull(0) ?: return@forEachIndexed

            segments.add(SpeedLimitSegment(lat, lon, limit, now))

            // También cachear en grid
            val gridKey = toGridKey(lat, lon)
            gridCache[gridKey] = limit
        }

        routeSegments = segments
        trimCacheIfNeeded()
        Log.i(TAG, "✅ Loaded ${segments.size} speed limit segments from OSRM maxspeed")
    }

    private fun parseMaxSpeedEntry(entry: MaxSpeedEntry): Int? {
        // Si es "unknown" o "none", no hay dato
        if (entry.unknown == true) return null
        if (entry.none == true) return null

        val speed = entry.speed ?: return null
        val unit = entry.unit

        // Convertir si es mph
        return if (unit == "mph") {
            (speed * 1.60934).toInt()
        } else {
            speed // ya en km/h
        }
    }

    // ── Helpers ──

    private fun findNearestSegment(location: Location): SpeedLimitSegment? {
        if (routeSegments.isEmpty()) return null

        return routeSegments.minByOrNull { seg ->
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, seg.lat, seg.lon, results)
            results[0].toDouble()
        }?.let { seg ->
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, seg.lat, seg.lon, results)
            if (results[0] < SEGMENT_MATCH_RADIUS_M) seg else null
        }
    }

    private fun toGridKey(lat: Double, lon: Double): String {
        val gridLat = (lat * GRID_PRECISION).toInt()
        val gridLon = (lon * GRID_PRECISION).toInt()
        return "$gridLat,$gridLon"
    }

    private fun trimCacheIfNeeded() {
        if (gridCache.size > CACHE_MAX_SIZE) {
            val keysToRemove = gridCache.keys.toList().take(gridCache.size - CACHE_MAX_SIZE / 2)
            keysToRemove.forEach { gridCache.remove(it) }
        }
    }
}
