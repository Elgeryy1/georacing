package com.georacing.georacing.domain.navigation

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object BearingCalculator {

    /**
     * Calculates the bearing (angle) from [currentLat], [currentLon] to [destLat], [destLon].
     * Returns degrees (0-360).
     */
    fun calculateBearing(
        currentLat: Double,
        currentLon: Double,
        destLat: Double,
        destLon: Double
    ): Float {
        val lat1 = Math.toRadians(currentLat)
        val lon1 = Math.toRadians(currentLon)
        val lat2 = Math.toRadians(destLat)
        val lon2 = Math.toRadians(destLon)

        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360 // Normalize to 0-360

        return bearing.toFloat()
    }

    /**
     * Calculates the horizontal offset (X position) for an AR label on the screen.
     * Returns a value between -1.0 (left) and 1.0 (right), or null if out of FOV.
     * 
     * @param azimuth Device's current heading (0-360).
     * @param targetBearing Truth bearing to the target (0-360).
     * @param fov Field of View of the camera (e.g., 60 degrees).
     */
    fun calculateScreenPosition(azimuth: Float, targetBearing: Float, fov: Float = 60f): Float? {
        var diff = targetBearing - azimuth
        
        // Normalize diff to -180 to 180
        if (diff > 180) diff -= 360
        if (diff < -180) diff += 360

        // If absolute difference is greater than half FOV, it's off-screen
        if (Math.abs(diff) > (fov / 2)) {
            return null
        }

        // Map diff (-fov/2 to fov/2) to (-1 to 1)
        // Example: FOV=60. Diff=-30 -> -1. Diff=0 -> 0. Diff=30 -> 1.
        return diff / (fov / 2)
    }
}
