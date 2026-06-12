package com.georacing.georacing.car

import android.location.Location
import com.georacing.georacing.data.parking.ParkingLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Tier 1: Parking Proximity Detector
 * 
 * Detecta cuando el usuario está cerca de su parking asignado y determina
 * la puerta de acceso más cercana basándose en el bearing.
 */
object ParkingProximityDetector {
    
    private const val PROXIMITY_THRESHOLD_METERS = 200.0
    
    data class ParkingCluster(
        val name: String,              // "PARKING C"
        val accessGate: String,        // "ACCESO 2"
        val gateImageRes: Int,         // R.drawable.parking_c_gate2 (placeholder)
        val distance: Double
    )
    
    /**
     * Detecta si el usuario está cerca del parking asignado.
     * Retorna ParkingCluster si está dentro del threshold, null si no.
     */
    fun detectNearbyParking(
        currentLocation: Location,
        assignedParking: ParkingLocation?
    ): ParkingCluster? {
        if (assignedParking == null) return null
        
        val distance = calculateDistance(
            currentLocation.latitude,
            currentLocation.longitude,
            assignedParking.latitude,
            assignedParking.longitude
        )
        
        if (distance > PROXIMITY_THRESHOLD_METERS) return null
        
        // Determinar puerta más cercana basada en bearing
        val bearing = currentLocation.bearing
        val accessGate = determineAccessGate(bearing)
        
        return ParkingCluster(
            name = "Tu Coche",
            accessGate = accessGate.name,
            gateImageRes = accessGate.imageRes,
            distance = distance
        )
    }
    
    /**
     * Determina la puerta de acceso más cercana basándose en el bearing.
     */
    private fun determineAccessGate(bearing: Float): AccessGate {
        // Lógica simple basada en bearing (0-360 grados)
        // Norte = 0°, Este = 90°, Sur = 180°, Oeste = 270°
        return when {
            bearing < 45 || bearing >= 315 -> AccessGate("ACCESO NORTE", android.R.drawable.ic_menu_directions)
            bearing < 135 -> AccessGate("ACCESO ESTE", android.R.drawable.ic_menu_directions)
            bearing < 225 -> AccessGate("ACCESO SUR", android.R.drawable.ic_menu_directions)
            else -> AccessGate("ACCESO OESTE", android.R.drawable.ic_menu_directions)
        }
    }
    
    /**
     * Calcula la distancia entre dos puntos GPS usando fórmula Haversine.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    private data class AccessGate(val name: String, val imageRes: Int)
}
