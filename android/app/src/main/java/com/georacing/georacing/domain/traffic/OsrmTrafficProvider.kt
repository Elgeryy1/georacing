package com.georacing.georacing.domain.traffic

import android.location.Location
import android.util.Log
import com.georacing.georacing.car.OsrmResponse
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
 * Proveedor de tráfico real basado en datos OSRM.
 *
 * Estrategia:
 * 1. Al calcular ruta, obtiene anotaciones OSRM (speed por segmento)
 * 2. Compara la velocidad actual GPS con la velocidad esperada OSRM del segmento
 * 3. Si GPS speed << OSRM speed → hay tráfico
 * 4. Cachea datos de la ruta actual para consultas rápidas
 *
 * Limitación: OSRM público no tiene tráfico en tiempo real,
 * pero los segmentos con velocidades reducidas respecto al perfil
 * de la carretera SÍ indican congestión habitual (datos históricos OSM).
 */
class OsrmTrafficProvider : TrafficProvider {

    companion object {
        private const val TAG = "OsrmTrafficProvider"
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L  // 5 minutos
        private const val SEGMENT_MATCH_RADIUS_M = 50.0        // metros para snap
    }

    // ── Datos cacheados de la ruta ──

    data class SegmentSpeed(
        val startLat: Double, val startLon: Double,
        val endLat: Double, val endLon: Double,
        val expectedSpeedMs: Double,  // velocidad esperada m/s (perfil OSM)
        val distanceM: Double
    )

    private var cachedSegments: List<SegmentSpeed> = emptyList()
    private var cacheTimestamp: Long = 0L
    private val osrmService: OsrmService

    // Cache de consultas recientes por gridCell
    private val gridCache = ConcurrentHashMap<String, Double>()

    init {
        val baseUrl = OsrmConfig.getBaseUrl()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        osrmService = retrofit.create(OsrmService::class.java)
    }

    // ── API TrafficProvider ──

    override fun getTrafficFactor(location: Location): Double? {
        // Si tenemos segmentos cacheados de la ruta, usar comparación GPS vs esperado
        val nearestSegment = findNearestSegment(location)
        if (nearestSegment != null) {
            return calculateTrafficFactor(location, nearestSegment)
        }

        // Sin ruta cacheada → consultar OSRM con un mini-tramo ficticio
        val gridKey = "${(location.latitude * 100).toInt()},${(location.longitude * 100).toInt()}"
        gridCache[gridKey]?.let { return it }

        return null // No hay datos
    }

    override fun getTrafficFactorForSegment(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): Double? {
        // Buscar segmento cacheado que cubra este tramo
        val midLat = (startLat + endLat) / 2
        val midLon = (startLon + endLon) / 2
        val midLoc = Location("").apply { latitude = midLat; longitude = midLon }
        val segment = findNearestSegment(midLoc) ?: return null

        return if (segment.expectedSpeedMs > 0) {
            // Calcular factor basado en velocidad del perfil vs velocidad libre
            val freeFlowMs = estimateFreeFlowSpeed(segment)
            (freeFlowMs / segment.expectedSpeedMs).coerceIn(0.8, 3.0)
        } else null
    }

    override fun isAvailable(): Boolean = cachedSegments.isNotEmpty()

    override fun getTrafficDescription(location: Location): String? {
        val factor = getTrafficFactor(location) ?: return null
        return when {
            factor < 0.9 -> "🟢 Tráfico fluido"
            factor < 1.1 -> "🟢 Tráfico normal"
            factor < 1.3 -> "🟡 Tráfico moderado"
            factor < 1.6 -> "🟠 Tráfico intenso"
            else -> "🔴 Tráfico muy intenso"
        }
    }

    // ── Alimentar datos desde ruta OSRM ──

    /**
     * Llama a OSRM con annotations=speed,duration,distance y cachea segmentos.
     * Debe llamarse cuando se calcula una nueva ruta.
     */
    suspend fun loadRouteTrafficData(origin: LatLng, dest: LatLng) {
        withContext(Dispatchers.IO) {
            try {
                val coordinates = String.format(
                    Locale.US, "%.6f,%.6f;%.6f,%.6f",
                    origin.longitude, origin.latitude,
                    dest.longitude, dest.latitude
                )

                val response = osrmService.getRoute(
                    coordinates = coordinates,
                    annotations = "speed,duration,distance"
                )

                if (response.code == "Ok" && response.routes.isNotEmpty()) {
                    val route = response.routes[0]
                    val segments = mutableListOf<SegmentSpeed>()

                    route.legs.forEach { leg ->
                        val annotation = leg.annotation ?: return@forEach
                        val speeds = annotation.speed ?: return@forEach
                        val distances = annotation.distance ?: emptyList()

                        // Extraer coordenadas de los steps para mapear segmentos
                        val stepLocations = leg.steps.map { step ->
                            step.maneuver.location // [lon, lat]
                        }

                        speeds.forEachIndexed { i, speedMs ->
                            val dist = distances.getOrNull(i) ?: 0.0
                            // Aproximar start/end desde steps
                            val locIdx = (i * stepLocations.size / speeds.size)
                                .coerceIn(0, stepLocations.lastIndex)
                            val nextLocIdx = ((i + 1) * stepLocations.size / speeds.size)
                                .coerceIn(0, stepLocations.lastIndex)

                            val startLoc = stepLocations.getOrNull(locIdx) ?: return@forEachIndexed
                            val endLoc = stepLocations.getOrNull(nextLocIdx) ?: startLoc

                            segments.add(
                                SegmentSpeed(
                                    startLat = startLoc[1], startLon = startLoc[0],
                                    endLat = endLoc[1], endLon = endLoc[0],
                                    expectedSpeedMs = speedMs,
                                    distanceM = dist
                                )
                            )
                        }
                    }

                    cachedSegments = segments
                    cacheTimestamp = System.currentTimeMillis()
                    gridCache.clear()
                    Log.i(TAG, "✅ Loaded ${segments.size} traffic segments from OSRM")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading traffic data", e)
            }
        }
    }

    /**
     * Alimenta directamente con una respuesta OSRM ya obtenida (evita doble llamada).
     */
    fun updateFromResponse(response: OsrmResponse, routePoints: List<LatLng>) {
        val segments = mutableListOf<SegmentSpeed>()

        response.routes.firstOrNull()?.legs?.forEach { leg ->
            val annotation = leg.annotation ?: return@forEach
            val speeds = annotation.speed ?: return@forEach
            val distances = annotation.distance ?: emptyList()

            speeds.forEachIndexed { i, speedMs ->
                val dist = distances.getOrNull(i) ?: 0.0
                val pointIdx = (i * routePoints.size / speeds.size)
                    .coerceIn(0, routePoints.lastIndex)
                val nextPointIdx = ((i + 1) * routePoints.size / speeds.size)
                    .coerceIn(0, routePoints.lastIndex)

                val start = routePoints[pointIdx]
                val end = routePoints[nextPointIdx]

                segments.add(
                    SegmentSpeed(
                        startLat = start.latitude, startLon = start.longitude,
                        endLat = end.latitude, endLon = end.longitude,
                        expectedSpeedMs = speedMs,
                        distanceM = dist
                    )
                )
            }
        }

        cachedSegments = segments
        cacheTimestamp = System.currentTimeMillis()
        gridCache.clear()
        Log.i(TAG, "✅ Updated ${segments.size} traffic segments from existing response")
    }

    // ── Lógica interna ──

    private fun findNearestSegment(location: Location): SegmentSpeed? {
        if (cachedSegments.isEmpty()) return null
        if (System.currentTimeMillis() - cacheTimestamp > CACHE_VALIDITY_MS) return null

        return cachedSegments.minByOrNull { seg ->
            val midLat = (seg.startLat + seg.endLat) / 2
            val midLon = (seg.startLon + seg.endLon) / 2
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, midLat, midLon, results)
            results[0].toDouble()
        }?.let { seg ->
            val midLat = (seg.startLat + seg.endLat) / 2
            val midLon = (seg.startLon + seg.endLon) / 2
            val results = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, midLat, midLon, results)
            if (results[0] < SEGMENT_MATCH_RADIUS_M) seg else null
        }
    }

    private fun calculateTrafficFactor(location: Location, segment: SegmentSpeed): Double {
        val expectedSpeedMs = segment.expectedSpeedMs
        if (expectedSpeedMs <= 0) return 1.0

        // Si tenemos velocidad GPS, comparar
        if (location.hasSpeed() && location.speed > 1.0f) {
            val actualSpeedMs = location.speed.toDouble()
            // factor = esperado / real → >1 significa más lento de lo esperado (tráfico)
            val factor = expectedSpeedMs / actualSpeedMs
            return factor.coerceIn(0.5, 3.0)
        }

        // Sin GPS speed → usar perfil OSRM como indicador
        val freeFlowMs = estimateFreeFlowSpeed(segment)
        return if (freeFlowMs > expectedSpeedMs) {
            (freeFlowMs / expectedSpeedMs).coerceIn(1.0, 2.5)
        } else {
            1.0 // Velocidad OSRM >= free flow → no hay congestión
        }
    }

    /**
     * Estima velocidad en flujo libre basándose en la velocidad OSRM del segmento.
     * OSRM ya usa perfiles de velocidad conservadores, así que usamos un factor.
     */
    private fun estimateFreeFlowSpeed(segment: SegmentSpeed): Double {
        val osrmSpeed = segment.expectedSpeedMs
        return when {
            osrmSpeed > 30.0 -> osrmSpeed * 1.1  // Autopista: OSRM ~= free flow
            osrmSpeed > 20.0 -> osrmSpeed * 1.15  // Autovía
            osrmSpeed > 10.0 -> osrmSpeed * 1.2   // Carretera
            else -> osrmSpeed * 1.3                // Urbano
        }
    }
}
