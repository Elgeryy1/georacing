package com.georacing.georacing.fake

import android.location.Location
import org.maplibre.android.geometry.LatLng
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FASE 2.3: Proveedor de ubicaciones GPS falsas para tests de navegación.
 * 
 * Permite simular navegación completa sin salir a la calle, reproduciendo
 * comportamientos reales como:
 * - Seguimiento de ruta (sin off-route)
 * - Saltos de GPS (simular túneles)
 * - Diferentes velocidades (ciudad, autopista)
 * - Diferentes precisiones (GPS bueno/malo)
 * 
 * Uso típico en tests:
 * ```
 * val provider = FakeLocationProvider.circuitTrack()
 * val loc1 = provider.nextLocation()
 * val loc2 = provider.nextLocation()
 * ```
 */
class FakeLocationProvider(
    private val track: List<LatLng>,
    private val speedKmh: Float = 50f,
    private val accuracyMeters: Float = 10f,
    private val provider: String = "fake"
) {
    private var index = 0
    
    /**
     * Obtiene la siguiente ubicación en el track.
     * Calcula bearing automáticamente según la dirección de movimiento.
     */
    fun nextLocation(): Location {
        if (track.isEmpty()) {
            throw IllegalStateException("Track cannot be empty")
        }
        
        val point = track[index % track.size]
        val nextPoint = track[(index + 1) % track.size]
        
        index++
        
        return Location(provider).apply {
            latitude = point.latitude
            longitude = point.longitude
            accuracy = accuracyMeters
            speed = speedKmh / 3.6f  // km/h a m/s
            bearing = calculateBearing(point, nextPoint).toFloat()
            time = System.currentTimeMillis()
            
            // Android requiere estos campos para que Location sea válido
            elapsedRealtimeNanos = System.nanoTime()
        }
    }
    
    /**
     * Reinicia el índice para empezar el track desde el principio.
     */
    fun reset() {
        index = 0
    }
    
    /**
     * Salta N posiciones en el track (simular túnel GPS o salto repentino).
     */
    fun skip(positions: Int) {
        index += positions
    }
    
    /**
     * Obtiene el progreso actual en el track (0.0 = inicio, 1.0 = final).
     */
    fun getProgress(): Double {
        return (index.toDouble() / track.size).coerceIn(0.0, 1.0)
    }
    
    /**
     * Verifica si se completó el track.
     */
    fun isCompleted(): Boolean {
        return index >= track.size
    }
    
    /**
     * Calcula el bearing (rumbo) desde un punto al siguiente.
     * @return Ángulo en grados (0-360, donde 0 es norte)
     */
    private fun calculateBearing(from: LatLng, to: LatLng): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lon2 = Math.toRadians(to.longitude)
        
        val dLon = lon2 - lon1
        
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        
        val bearing = Math.toDegrees(atan2(y, x))
        
        // Normalizar a 0-360
        return (bearing + 360) % 360
    }
    
    companion object {
        /**
         * Track de prueba simple: ruta recta por Barcelona.
         * Útil para tests básicos de snap, ETA, etc.
         */
        fun straightTrack(): FakeLocationProvider {
            val track = listOf(
                LatLng(41.3851, 2.1734),  // Plaza Catalunya
                LatLng(41.3870, 2.1700),  // Passeig de Gràcia
                LatLng(41.3890, 2.1665),  // Diagonal
                LatLng(41.3910, 2.1630),  // Francesc Macià
                LatLng(41.3930, 2.1595)   // Zona Universitaria
            )
            return FakeLocationProvider(track, speedKmh = 50f, accuracyMeters = 10f)
        }
        
        /**
         * Track con curva pronunciada.
         * Útil para tests de snap en curvas y TTS.
         */
        fun curvedTrack(): FakeLocationProvider {
            val track = listOf(
                LatLng(41.3851, 2.1734),  // Inicio recto
                LatLng(41.3860, 2.1720),
                LatLng(41.3870, 2.1705),
                // Curva cerrada a la derecha
                LatLng(41.3875, 2.1695),
                LatLng(41.3878, 2.1680),
                LatLng(41.3880, 2.1665),
                LatLng(41.3881, 2.1650),
                // Continúa recto
                LatLng(41.3890, 2.1640),
                LatLng(41.3900, 2.1630)
            )
            return FakeLocationProvider(track, speedKmh = 40f, accuracyMeters = 15f)
        }
        
        /**
         * Track simulando rotonda (círculo de 8 puntos).
         * Útil para tests de instrucciones de rotonda con ordinales.
         */
        fun roundaboutTrack(): FakeLocationProvider {
            val centerLat = 41.3900
            val centerLon = 2.1650
            val radius = 0.0005  // ~50m de radio
            
            val track = mutableListOf<LatLng>()
            
            // Aproximación a la rotonda
            track.add(LatLng(centerLat - radius * 2, centerLon))
            track.add(LatLng(centerLat - radius * 1.5, centerLon))
            
            // 8 puntos alrededor del círculo (rotonda)
            for (i in 0..7) {
                val angle = Math.toRadians(i * 45.0)  // 45° entre puntos
                val lat = centerLat + radius * sin(angle)
                val lon = centerLon + radius * cos(angle)
                track.add(LatLng(lat, lon))
            }
            
            // Salida de la rotonda
            track.add(LatLng(centerLat + radius * 1.5, centerLon))
            track.add(LatLng(centerLat + radius * 2, centerLon))
            
            return FakeLocationProvider(track, speedKmh = 30f, accuracyMeters = 12f)
        }
        
        /**
         * Track del Circuit de Barcelona-Catalunya (simplificado).
         * Útil para tests de navegación en el circuito real.
         */
        fun circuitTrack(): FakeLocationProvider {
            val track = listOf(
                // Recta principal y curva Elf
                LatLng(41.5703, 2.2578),
                LatLng(41.5710, 2.2590),
                LatLng(41.5715, 2.2600),
                
                // Renault (curvas rápidas)
                LatLng(41.5720, 2.2610),
                LatLng(41.5723, 2.2618),
                
                // Repsol (curva lenta)
                LatLng(41.5725, 2.2625),
                LatLng(41.5724, 2.2632),
                
                // Seat (curvas rápidas)
                LatLng(41.5720, 2.2640),
                LatLng(41.5715, 2.2645),
                
                // Campsa (curva final)
                LatLng(41.5708, 2.2650),
                LatLng(41.5703, 2.2578)  // Vuelta a meta
            )
            return FakeLocationProvider(track, speedKmh = 80f, accuracyMeters = 8f)
        }
        
        /**
         * Track con GPS inestable (precisión variable).
         * Útil para tests del filtro de accuracy FASE 1.4.
         */
        fun unstableGPSTrack(): FakeLocationProvider {
            val track = listOf(
                LatLng(41.3851, 2.1734),
                LatLng(41.3860, 2.1720),
                LatLng(41.3870, 2.1705),
                LatLng(41.3880, 2.1690)
            )
            // Accuracy alto = GPS malo (debería ser ignorado por filtro)
            return FakeLocationProvider(track, speedKmh = 50f, accuracyMeters = 150f)
        }
    }
}
