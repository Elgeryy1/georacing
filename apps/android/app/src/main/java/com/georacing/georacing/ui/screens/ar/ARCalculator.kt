package com.georacing.georacing.ui.screens.ar

import android.location.Location
import com.georacing.georacing.car.PoiModel
import kotlin.math.*

object ARCalculator {

    data class ScreenPosition(
        val x: Float, // Relative 0..1 (or pixels if passed screen size)
        val y: Float, // Relative 0..1
        val isVisible: Boolean,
        val distanceMeters: Float
    )

    private const val FOV_HORIZONTAL = 60.0 // Approximate Phone Camera FOV
    private const val FOV_VERTICAL = 45.0
    private const val MAX_DISTANCE_METERS = 500.0 // Don't show POIs further than this

    fun calculatePosition(
        userLocation: Location,
        poi: PoiModel,
        deviceAzimuth: Float,
        devicePitch: Float
    ): ScreenPosition {
        val poiLocation = Location("POI").apply {
            latitude = poi.latitude
            longitude = poi.longitude
        }

        // 1. Calculate Distance
        val distance = userLocation.distanceTo(poiLocation)
        if (distance > MAX_DISTANCE_METERS) {
            return ScreenPosition(0f, 0f, false, distance)
        }

        // 2. Calculate Bearing (Direction to POI)
        val bearingToPoi = userLocation.bearingTo(poiLocation) // -180 to 180

        // 3. Calculate Delta Azimuth (Difference between where we look and where POI is)
        // Normalize both to 0-360
        val normalizedBearing = (bearingToPoi + 360) % 360
        var deltaAzimuth = normalizedBearing - deviceAzimuth

        // Handle wrap-around (e.g. 350 vs 10 degrees -> diff is 20, not 340)
        if (deltaAzimuth > 180) deltaAzimuth -= 360
        if (deltaAzimuth < -180) deltaAzimuth += 360

        // 4. Determine if visible (within FOV)
        val halfFov = FOV_HORIZONTAL / 2
        if (abs(deltaAzimuth) > halfFov) {
            return ScreenPosition(0f, 0f, false, distance)
        }

        // 5. Calculate X Position
        // -halfFov -> x=0, 0 -> x=0.5, +halfFov -> x=1
        val x = (0.5f + (deltaAzimuth / FOV_HORIZONTAL)).toFloat()

        // 6. Calculate Y Position based on Pitch (simplified)
        // If pitch is 0 (horizon), y=0.5.
        // If looking DOWN (positive pitch), object goes UP on screen.
        // We assume POI is at same altitude (horizon).
        val y = (0.5f + (devicePitch / FOV_VERTICAL)).toFloat()

        return ScreenPosition(x, y.coerceIn(0f, 1f), true, distance)
    }
}
