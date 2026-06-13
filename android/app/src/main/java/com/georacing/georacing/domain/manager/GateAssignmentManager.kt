package com.georacing.georacing.domain.manager

import android.location.Location
import com.georacing.georacing.data.repository.CircuitLocationsRepository
import com.georacing.georacing.domain.model.CircuitNode

/**
 * Representa la asignación de una puerta peatonal óptima para el usuario.
 */
data class GateAssignment(
    val gateName: String,
    val gateId: String,
    val walkingTimeMinutes: Int,
    val distanceMeters: Int,
    val parkingId: String?,
    val parkingName: String?
)

/**
 * Gestiona la asignación inteligente de puertas peatonales según la ubicación
 * del usuario en el Circuit de Barcelona-Catalunya.
 * 
 * Utiliza `Location.distanceBetween()` para calcular dinámicamente la puerta más cercana.
 */
object GateAssignmentManager {

    // Centro aproximado del circuito para validación de perímetro
    private const val CIRCUIT_CENTER_LAT = 41.5700
    private const val CIRCUIT_CENTER_LNG = 2.2600
    private const val CIRCUIT_RADIUS_METERS = 3000f  // 3km radius (Aumentado para parkings lejanos)

    // Velocidad promedio caminando en m/s (aprox 5 km/h)
    private const val WALKING_SPEED_MS = 1.4f

    /**
     * Asigna la puerta más cercana según la ubicación actual del usuario.
     * 
     * @param userLocation Ubicación del usuario (puede ser parking guardado o posición actual)
     * @return GateAssignment con la puerta recomendada, o null si está fuera del circuito
     */
    fun assignGate(userLocation: Location): GateAssignment? {
        // 1. Validar que está dentro del perímetro del circuito
        if (!isWithinCircuitPerimeter(userLocation)) {
            return null
        }

        // 2. Encontrar la puerta más cercana (usando distanceBetween)
        val nearestGate = findNearestGate(userLocation)
        
        // 3. Calcular distancia exacta
        val distanceMeters = calculateDistance(
            userLocation.latitude, userLocation.longitude,
            nearestGate.lat, nearestGate.lon
        )
        
        // 4. Estimar tiempo caminando
        val walkingTimeMinutes = (distanceMeters / WALKING_SPEED_MS / 60).toInt().coerceAtLeast(1)
        
        // 5. Encontrar el parking más cercano (para contexto)
        val nearestParking = findNearestParking(userLocation)
        
        return GateAssignment(
            gateName = nearestGate.name,
            gateId = nearestGate.id,
            walkingTimeMinutes = walkingTimeMinutes,
            distanceMeters = distanceMeters.toInt(),
            parkingId = nearestParking?.id,
            parkingName = nearestParking?.name
        )
    }

    /**
     * Verifica si la ubicación está dentro del perímetro del circuito.
     */
    fun isWithinCircuitPerimeter(location: Location): Boolean {
        val distance = calculateDistance(
            location.latitude, location.longitude,
            CIRCUIT_CENTER_LAT, CIRCUIT_CENTER_LNG
        )
        return distance <= CIRCUIT_RADIUS_METERS
    }

    /**
     * Encuentra la puerta más cercana a la ubicación dada.
     */
    private fun findNearestGate(location: Location): CircuitNode {
        val gates = CircuitLocationsRepository.getGates()
        
        return gates.minByOrNull { gate ->
            calculateDistance(
                location.latitude, location.longitude,
                gate.lat, gate.lon
            )
        } ?: gates.first()
    }

    /**
     * Encuentra el parking más cercano a la ubicación dada.
     * USA SOLO PARKINGS OFICIALES (HIGH/MEDIUM CONFIDENCE).
     */
    private fun findNearestParking(location: Location): CircuitNode? {
        val parkings = CircuitLocationsRepository.getNavigableParkings()
        
        if (parkings.isEmpty()) return null
        
        return parkings.minByOrNull { parking ->
            calculateDistance(
                location.latitude, location.longitude,
                parking.lat, parking.lon
            )
        }
    }

    /**
     * Calcula la distancia en metros entre dos puntos usando Location.distanceBetween().
     */
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    /**
     * Calcula la distancia desde la ubicación actual hasta una puerta específica.
     */
    fun distanceToGate(currentLocation: Location, gateId: String): Float {
        val gate = CircuitLocationsRepository.getNodeById(gateId) ?: return Float.MAX_VALUE
        return calculateDistance(
            currentLocation.latitude, currentLocation.longitude,
            gate.lat, gate.lon
        )
    }

    /**
     * Calcula la distancia desde la ubicación actual hasta un parking específico.
     */
    fun distanceToParking(currentLocation: Location, parkingId: String): Float {
        val parking = CircuitLocationsRepository.getNodeById(parkingId) ?: return Float.MAX_VALUE
        return calculateDistance(
            currentLocation.latitude, currentLocation.longitude,
            parking.lat, parking.lon
        )
    }
    
    /**
     * Obtiene todas las puertas ordenadas por distancia desde una ubicación.
     */
    fun getGatesSortedByDistance(location: Location): List<Pair<CircuitNode, Float>> {
        return CircuitLocationsRepository.getGates().map { gate ->
            val distance = calculateDistance(
                location.latitude, location.longitude,
                gate.lat, gate.lon
            )
            Pair(gate, distance)
        }.sortedBy { it.second }
    }
}

