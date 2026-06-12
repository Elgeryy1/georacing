package com.georacing.georacing.car

import android.util.Log
import com.georacing.georacing.car.config.OsrmConfig
import com.google.gson.GsonBuilder
import org.maplibre.android.geometry.LatLng
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

/**
 * Repositorio de rutas usando OSRM como motor principal.
 * GraphHopper está deshabilitado/comentado.
 * 
 * FASE 3.1: Ahora usa OsrmConfig para alternar entre servidor público y local.
 * 
 * Para usar OSRM local:
 * ```kotlin
 * OsrmConfig.setEnvironment(OsrmConfig.OsrmEnvironment.LOCAL)
 * OsrmConfig.setLocalHost("192.168.1.100", 5000)
 * ```
 * 
 * Ver OsrmConfig.kt para instrucciones de cómo levantar OSRM con Docker.
 */
class RouteRepository {
    
    private val osrmService: OsrmService
    
    init {
        // FASE 3.1: Obtener base URL desde configuración centralizada
        val baseUrl = OsrmConfig.getBaseUrl()
        
        val osrmRetrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
        osrmService = osrmRetrofit.create(OsrmService::class.java)
        
        Log.i("RouteRepository", "OSRM configurado: $baseUrl (Environment: ${OsrmConfig.getCurrentEnvironment()})")
    }

    /**
     * Obtiene ruta usando OSRM
     */
    suspend fun getRoute(origin: LatLng, dest: LatLng, avoidTraffic: Boolean = true): RouteResult? {
        return getRouteFromOsrm(origin, dest)
    }
    
    /* GraphHopper deshabilitado - usar solo OSRM
    private suspend fun getRouteFromGraphHopper(...): RouteResult? {
        // Código comentado - no se usa
    }
    */
    private suspend fun getRouteFromOsrm(origin: LatLng, dest: LatLng): RouteResult? {
        return try {
            val coordinates = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", 
                origin.longitude, origin.latitude, 
                dest.longitude, dest.latitude
            )
            
            // FASE 3.1: Logging de URL completa para debugging
            val fullUrl = "${OsrmConfig.getBaseUrl()}route/v1/driving/$coordinates?overview=full&geometries=polyline6&steps=true"
            Log.d("RouteRepository", "Obteniendo ruta OSRM desde: $fullUrl")
            
            val response = osrmService.getRoute(coordinates)
            
            if (response.code == "Ok" && response.routes.isNotEmpty()) {
                val route = response.routes[0]
                val points = decodePolyline(route.geometry)
                val steps = if (route.legs.isNotEmpty()) route.legs[0].steps else emptyList()
                
                Log.i("RouteRepository", "Ruta obtenida: ${route.distance}m, ${route.duration}s, ${steps.size} pasos")
                
                RouteResult(
                    points = points,
                    distance = route.distance,
                    duration = route.duration,
                    steps = steps,
                    trafficSegments = emptyList() // Sin tráfico en OSRM básico
                )
            } else {
                Log.e("RouteRepository", "No route found: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("RouteRepository", "Error fetching route from OSRM: ${e.message}", e)
            null
        }
    }
    
    // GraphHopper helpers comentados - no necesarios para OSRM
    /* 
    private fun getManeuverType(sign: Int): String { ... }
    private fun getManeuverModifier(sign: Int): String? { ... }
    private fun calculateTrafficSegments(...): List<TrafficSegment> { ... }
    */

    // Decodes OSRM polyline6
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }
}

data class RouteResult(
    val points: List<LatLng>,
    val distance: Double, // meters
    val duration: Double, // seconds
    val steps: List<Step> = emptyList(),
    val trafficSegments: List<TrafficSegment> = emptyList() // Segmentos con información de tráfico
)

data class TrafficSegment(
    val startIndex: Int,
    val endIndex: Int,
    val congestionLevel: CongestionLevel // nivel de congestión
)

enum class CongestionLevel {
    FREE_FLOW,    // Verde - fluido
    MODERATE,     // Amarillo - moderado
    HEAVY,        // Naranja - denso
    SEVERE        // Rojo - muy denso
}
