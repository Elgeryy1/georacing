package com.georacing.georacing.car

import org.maplibre.android.geometry.LatLng
import kotlin.math.floor
import kotlin.math.pow

object RouteSmoothing {

    fun smoothRoute(points: List<LatLng>, precision: Int = 10): List<LatLng> {
        if (points.size < 3) return points

        val smoothed = ArrayList<LatLng>()
        
        // Add start point
        smoothed.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = if (i == 0) points[i] else points[i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 1 == points.size - 1) p2 else points[i + 2]

            for (t in 1..precision) {
                val tNorm = t.toDouble() / precision
                smoothed.add(catmullRom(p0, p1, p2, p3, tNorm))
            }
        }
        
        return smoothed
    }

    private fun catmullRom(p0: LatLng, p1: LatLng, p2: LatLng, p3: LatLng, t: Double): LatLng {
        val t2 = t * t
        val t3 = t2 * t

        val lat = 0.5 * ((2.0 * p1.latitude) +
                (-p0.latitude + p2.latitude) * t +
                (2.0 * p0.latitude - 5.0 * p1.latitude + 4.0 * p2.latitude - p3.latitude) * t2 +
                (-p0.latitude + 3.0 * p1.latitude - 3.0 * p2.latitude + p3.latitude) * t3)

        val lon = 0.5 * ((2.0 * p1.longitude) +
                (-p0.longitude + p2.longitude) * t +
                (2.0 * p0.longitude - 5.0 * p1.longitude + 4.0 * p2.longitude - p3.longitude) * t2 +
                (-p0.longitude + 3.0 * p1.longitude - 3.0 * p2.longitude + p3.longitude) * t3)

        return LatLng(lat, lon)
    }
}
