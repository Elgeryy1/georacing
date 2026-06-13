package com.georacing.georacing.domain.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BearingCalculator].
 *
 * Covers great-circle bearing computation (used for the AR compass and the
 * navigation arrow) and the field-of-view projection used to place AR labels
 * on screen.
 */
class BearingCalculatorTest {

    private val tolerance = 0.5f // degrees / normalized units

    @Test
    fun `bearing due north is approximately zero`() {
        val bearing = BearingCalculator.calculateBearing(0.0, 0.0, 1.0, 0.0)
        // Both 0 and 360 are "north"; normalize before comparing.
        val normalized = if (bearing > 180f) bearing - 360f else bearing
        assertEquals(0f, normalized, tolerance)
    }

    @Test
    fun `bearing due east is approximately ninety degrees`() {
        val bearing = BearingCalculator.calculateBearing(0.0, 0.0, 0.0, 1.0)
        assertEquals(90f, bearing, tolerance)
    }

    @Test
    fun `bearing due south is approximately one eighty`() {
        val bearing = BearingCalculator.calculateBearing(0.0, 0.0, -1.0, 0.0)
        assertEquals(180f, bearing, tolerance)
    }

    @Test
    fun `bearing due west is approximately two seventy`() {
        val bearing = BearingCalculator.calculateBearing(0.0, 0.0, 0.0, -1.0)
        assertEquals(270f, bearing, tolerance)
    }

    @Test
    fun `bearing is always normalized into the 0 to 360 range`() {
        // Sweep a ring of destinations around an origin and assert every result
        // stays within [0, 360).
        val originLat = 41.5685
        val originLon = 2.2555
        for (deg in 0 until 360 step 15) {
            val rad = Math.toRadians(deg.toDouble())
            val destLat = originLat + 0.01 * Math.cos(rad)
            val destLon = originLon + 0.01 * Math.sin(rad)
            val bearing = BearingCalculator.calculateBearing(originLat, originLon, destLat, destLon)
            assertTrue("bearing $bearing out of range for deg=$deg", bearing >= 0f && bearing < 360f)
        }
    }

    @Test
    fun `target directly ahead maps to screen center`() {
        val pos = BearingCalculator.calculateScreenPosition(azimuth = 90f, targetBearing = 90f, fov = 60f)
        assertEquals(0f, pos!!, tolerance)
    }

    @Test
    fun `target at right edge of fov maps to plus one`() {
        // Half of a 60 degree FOV is 30 degrees to the right.
        val pos = BearingCalculator.calculateScreenPosition(azimuth = 90f, targetBearing = 120f, fov = 60f)
        assertEquals(1f, pos!!, tolerance)
    }

    @Test
    fun `target at left edge of fov maps to minus one`() {
        val pos = BearingCalculator.calculateScreenPosition(azimuth = 90f, targetBearing = 60f, fov = 60f)
        assertEquals(-1f, pos!!, tolerance)
    }

    @Test
    fun `target outside fov returns null`() {
        // 45 degrees off-axis is beyond half of a 60 degree FOV.
        val pos = BearingCalculator.calculateScreenPosition(azimuth = 90f, targetBearing = 135f, fov = 60f)
        assertNull(pos)
    }

    @Test
    fun `screen position handles wraparound across the 360 boundary`() {
        // Azimuth 350, target 10 -> a 20 degree difference to the right, not 340.
        val pos = BearingCalculator.calculateScreenPosition(azimuth = 350f, targetBearing = 10f, fov = 60f)
        // 20 degrees of a 30 degree half-FOV -> 0.666...
        assertEquals(0.6667f, pos!!, 0.01f)
    }
}
