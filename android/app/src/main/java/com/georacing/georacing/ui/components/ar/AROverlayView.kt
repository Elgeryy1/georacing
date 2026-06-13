package com.georacing.georacing.ui.components.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.navigation.BearingCalculator

data class ARLabel(
    val id: String,
    val text: String,
    val lat: Double,
    val lon: Double
)

@Composable
fun AROverlayView(
    azimuth: Float,
    userLat: Double,
    userLon: Double,
    targets: List<ARLabel>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val fov = 60f // Camera FOV (approx)

        // Draw HUD Center Crosshair
        drawLine(
            color = Color(0xFF00FF00), // Neon Green
            start = Offset(width / 2 - 20, height / 2),
            end = Offset(width / 2 + 20, height / 2),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00FF00),
            start = Offset(width / 2, height / 2 - 20),
            end = Offset(width / 2, height / 2 + 20),
            strokeWidth = 2.dp.toPx()
        )

        // Draw targets
        val paint = Paint().asFrameworkPaint().apply {
            textSize = 32f
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
        }

        val linePaint = Paint().asFrameworkPaint().apply {
            color = android.graphics.Color.GREEN
            strokeWidth = 2f
        }

        targets.forEach { target ->
            val bearing = BearingCalculator.calculateBearing(userLat, userLon, target.lat, target.lon)
            val screenPos = BearingCalculator.calculateScreenPosition(azimuth, bearing, fov)

            if (screenPos != null) {
                // screenPos is -1 (left) to 1 (right)
                // Map to 0 to width
                val x = (screenPos + 1) / 2 * width
                val y = height / 2

                // Draw Marker Line
                drawLine(
                    color = Color.Red,
                    start = Offset(x, y - 50),
                    end = Offset(x, y + 50),
                    strokeWidth = 4f
                )

                // Draw Text
                drawContext.canvas.nativeCanvas.drawText(
                    target.text,
                    x,
                    y - 60, // Above line
                    paint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "${bearing.toInt()}°",
                    x,
                    y + 80, // Below line
                    paint
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewAROverlay() {
    AROverlayView(
        azimuth = 0f, // Looking North
        userLat = 0.0,
        userLon = 0.0,
        targets = listOf(
            ARLabel("1", "NORTH", 1.0, 0.0), // Should be center
            ARLabel("2", "EAST", 0.0, 1.0)   // Should be right (90 deg) -> Off screen if fov=60
        )
    )
}
