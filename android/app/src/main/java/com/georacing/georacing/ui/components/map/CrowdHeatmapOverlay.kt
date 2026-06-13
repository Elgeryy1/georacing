package com.georacing.georacing.ui.components.map

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.georacing.georacing.data.repository.HeatPoint
import kotlinx.coroutines.delay

@Composable
fun CrowdHeatmapOverlay(
    heatPoints: List<HeatPoint>,
    cameraPositionLatitude: Double, // To project Points to Screen (Simplified for Demo)
    cameraPositionLongitude: Double,
    zoomLevel: Float
) {
    // Pulse Animation for High Intensity
    val infiniteTransition = rememberInfiniteTransition(label = "HeatmapPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        heatPoints.forEach { point: HeatPoint ->
            // --- SIMPLIFIED PROJECTION FOR DEMO ---
            // In a real app we'd use map projection.
            // Here we assume the map is roughly centered on the circuit and place points relatively.
            // This is a "Visual Hack" for the demo to ensure points appear on screen.
            
            // Map Center (Circuit Approx Center)
            val centerLat = 41.57
            val centerLon = 2.26
            
            // Scale diff based on zoom (rough approximation)
            val scaleFactor = 100000f // Arbitrary scale for lat/lon diff to pixels
            
            val dx = (point.lon - centerLon) * scaleFactor
            val dy = (centerLat - point.lat) * scaleFactor // Latitude inverted y-axis
            
            val screenX = size.width / 2 + dx.toFloat()
            val screenY = size.height / 2 + dy.toFloat()
            
            // Color Logic
            val isCritical = point.intensity > 0.8f
            val baseColor = if (isCritical) Color.Red else Color.Green
            val alpha = if (isCritical) pulseAlpha else 0.2f
            
            // Draw Heat Circle
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = point.radius * (if (isCritical) 1.5f else 1.0f),
                center = Offset(screenX, screenY)
            )
            
            // Draw Core
            drawCircle(
                color = baseColor.copy(alpha = 1f),
                radius = 10f,
                center = Offset(screenX, screenY)
            )
        }
    }
}
