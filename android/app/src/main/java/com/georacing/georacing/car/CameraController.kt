package com.georacing.georacing.car

import android.location.Location
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng

/**
 * Manages atomic camera state calculation based on driving context.
 * Implements "Google Maps-like" dynamic zoom and tilt.
 */
class CameraController {

    data class CameraState(
        val target: LatLng,
        val zoom: Double,
        val tilt: Double,
        val bearing: Double
    )

    /**
     * Calculates the ideal camera state.
     * @param location The current user location.
     * @param speedKmh Current speed in km/h.
     * @param distanceToTurnMeters Distance to the next maneuver (0 if not navigating).
     * @param isNavigationActive True if turn-by-turn is active.
     */
    fun update(
        location: Location,
        speedKmh: Float,
        distanceToTurnMeters: Double,
        isNavigationActive: Boolean
    ): CameraState {
        val latLng = LatLng(location.latitude, location.longitude)
        val bearing = location.bearing.toDouble()

        // BASE LOGIC:
        // High speed -> Zoom out, tilt up (see further)
        // Low speed -> Zoom in, tilt down (see details)
        // Maneuver approaching -> Zoom in drastically

        var targetZoom: Double
        var targetTilt: Double

        if (isNavigationActive) {
            // --- NAVIGATION MODE ---
            
            // 1. Calculate Base Zoom from Speed
            // 0-30 km/h -> Zoom 17.5
            // 120+ km/h -> Zoom 14.5
            val speedFactor = (speedKmh / 120f).coerceIn(0f, 1f)
            val baseZoom = 17.5 - (3.0 * speedFactor)

            // 2. Adjust for Maneuver
            // If < 200m to turn, force zoom in to at least 16.5
            if (distanceToTurnMeters < 200 && distanceToTurnMeters > 0) {
                 // Interpolate between current baseZoom and 17.0 based on proximity
                 val proximityFactor = (1.0 - (distanceToTurnMeters / 200.0)).coerceIn(0.0, 1.0)
                 targetZoom = baseZoom + (17.5 - baseZoom) * proximityFactor
            } else {
                targetZoom = baseZoom
            }

            // 3. Tilt Logic
            // Faster = More tilt (up to 60)
            targetTilt = 30.0 + (30.0 * speedFactor)

        } else {
            // --- FREE DRIVE MODE ---
            // Simpler logic, generally more zoomed out
            val speedFactor = (speedKmh / 100f).coerceIn(0f, 1f)
            targetZoom = 16.0 - (1.5 * speedFactor)
            targetTilt = 30.0 + (15.0 * speedFactor)
        }

        return CameraState(
            target = latLng,
            zoom = targetZoom,
            tilt = targetTilt,
            bearing = bearing
        )
    }
}
