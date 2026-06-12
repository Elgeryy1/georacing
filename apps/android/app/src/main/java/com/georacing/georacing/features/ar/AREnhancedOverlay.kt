package com.georacing.georacing.features.ar

import android.graphics.Paint
import android.graphics.RectF
import android.hardware.SensorManager
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.georacing.georacing.car.PoiModel
import com.georacing.georacing.car.PoiType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Overlay AR mejorado con:
 *
 * 1. Indicador de calibración de brújula (barra de precisión)
 * 2. Marcadores POI por categoría con colores e iconos
 * 3. Escala de tamaño según distancia (más cerca = más grande)
 * 4. Flecha de navegación al siguiente waypoint
 * 5. Brújula mini en esquina superior
 * 6. Etiquetas con info de distancia y zona
 */
object AREnhancedOverlay {

    // ── Colores por tipo de POI ──
    private val poiColors = mapOf<PoiType, Color>(
        PoiType.PARKING to Color(0xFF9C27B0),
        PoiType.GATE to Color(0xFF4CAF50),
        PoiType.FANZONE to Color(0xFFFF9800),
        PoiType.SERVICE to Color(0xFF2196F3),
        PoiType.MEDICAL to Color(0xFFF44336),
        PoiType.OTHER to Color(0xFF00BCD4)
    )

    private val poiEmojis = mapOf<PoiType, String>(
        PoiType.PARKING to "🅿️",
        PoiType.GATE to "🚪",
        PoiType.FANZONE to "🎉",
        PoiType.SERVICE to "🔧",
        PoiType.MEDICAL to "🏥",
        PoiType.OTHER to "📍"
    )

    // ── Parámetros ──
    private const val FOV_HORIZONTAL = 60.0
    private const val FOV_VERTICAL = 45.0
    private const val MAX_DISTANCE = 500f
    private const val MIN_MARKER_SIZE = 40f
    private const val MAX_MARKER_SIZE = 100f

    // ── Renderizado principal ──

    @Composable
    fun EnhancedOverlay(
        pois: List<PoiModel>,
        userLocation: Location,
        azimuth: Float,
        pitch: Float,
        compassAccuracy: Int, // SensorManager.SENSOR_STATUS_*
        navigationTarget: PoiModel? = null // POI destino para flecha
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Crosshair mejorado
            drawEnhancedCrosshair(w, h)

            // 2. POI markers
            var visibleCount = 0
            pois.forEach { poi ->
                if (visibleCount >= 15) return@forEach

                val result = calculateScreenPosition(userLocation, poi, azimuth, pitch)
                if (result.isVisible && result.distance < MAX_DISTANCE) {
                    visibleCount++
                    drawPoiMarker(
                        poi = poi,
                        x = result.x * w,
                        y = result.y * h,
                        distance = result.distance,
                        w = w, h = h
                    )
                }
            }

            // 3. Flecha de navegación al destino
            if (navigationTarget != null) {
                drawNavigationArrow(userLocation, navigationTarget, azimuth, w, h)
            }

            // 4. Mini brújula
            drawMiniCompass(azimuth, w)

            // 5. Indicador de calibración
            drawCalibrationIndicator(compassAccuracy, w, h)

            // 6. Info bar inferior
            drawInfoBar(visibleCount, pois.size, w, h)
        }
    }

    // ── Dibujar POI marker mejorado ──

    private fun DrawScope.drawPoiMarker(
        poi: PoiModel,
        x: Float, y: Float,
        distance: Float,
        w: Float, h: Float
    ) {
        val color = poiColors[poi.type] ?: Color.Green
        val emoji = poiEmojis[poi.type] ?: "📍"

        // Escala inversamente proporcional a la distancia
        val scale = ((1f - (distance / MAX_DISTANCE)) * 0.7f + 0.3f).coerceIn(0.3f, 1f)
        val markerSize = MIN_MARKER_SIZE + (MAX_MARKER_SIZE - MIN_MARKER_SIZE) * scale

        // Línea vertical desde la base
        drawLine(
            color = color,
            start = Offset(x, y),
            end = Offset(x, y - markerSize * 0.6f),
            strokeWidth = (2f * scale).dp.toPx()
        )

        // Círculo marcador
        drawCircle(
            color = color,
            radius = (markerSize * 0.25f).dp.toPx() / 3,
            center = Offset(x, y - markerSize * 0.6f)
        )

        // Borde del círculo
        drawCircle(
            color = Color.White,
            radius = (markerSize * 0.25f).dp.toPx() / 3,
            center = Offset(x, y - markerSize * 0.6f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )

        // Texto: nombre + distancia
        val textPaint = Paint().apply {
            this.color = android.graphics.Color.WHITE
            textSize = (12f * scale + 8f)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }

        val distText = when {
            distance < 100 -> "${distance.toInt()}m"
            else -> "${"%.1f".format(distance / 1000)}km"
        }
        val labelText = "${poi.name} • $distText"

        // Fondo del label
        val textWidth = textPaint.measureText(labelText)
        val bgPaint = Paint().apply {
            this.color = android.graphics.Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }

        val labelY = y - markerSize * 0.8f
        val rect = RectF(
            x - textWidth / 2 - 12, labelY - 28,
            x + textWidth / 2 + 12, labelY + 8
        )
        drawContext.canvas.nativeCanvas.drawRoundRect(rect, 8f, 8f, bgPaint)

        // Barra de color superior
        val colorBarPaint = Paint().apply {
            this.color = color.toArgb()
            style = Paint.Style.FILL
        }
        drawContext.canvas.nativeCanvas.drawRoundRect(
            RectF(rect.left, rect.top, rect.right, rect.top + 4),
            8f, 8f, colorBarPaint
        )

        // Texto
        drawContext.canvas.nativeCanvas.drawText(labelText, x, labelY, textPaint)
    }

    // ── Crosshair mejorado ──

    private fun DrawScope.drawEnhancedCrosshair(w: Float, h: Float) {
        val cx = w / 2
        val cy = h / 2
        val lineLen = 25.dp.toPx()
        val gap = 5.dp.toPx()
        val color = Color.Green.copy(alpha = 0.6f)

        // 4 líneas con gap central
        drawLine(color, Offset(cx - lineLen - gap, cy), Offset(cx - gap, cy), 1.5.dp.toPx())
        drawLine(color, Offset(cx + gap, cy), Offset(cx + lineLen + gap, cy), 1.5.dp.toPx())
        drawLine(color, Offset(cx, cy - lineLen - gap), Offset(cx, cy - gap), 1.5.dp.toPx())
        drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + lineLen + gap), 1.5.dp.toPx())

        // Punto central
        drawCircle(color, 2.dp.toPx(), Offset(cx, cy))
    }

    // ── Flecha de navegación ──

    private fun DrawScope.drawNavigationArrow(
        userLocation: Location,
        target: PoiModel,
        azimuth: Float,
        w: Float, h: Float
    ) {
        val targetLoc = Location("").apply {
            latitude = target.latitude
            longitude = target.longitude
        }

        val bearing = userLocation.bearingTo(targetLoc)
        val distance = userLocation.distanceTo(targetLoc)

        val normalizedBearing = (bearing + 360) % 360
        var delta = normalizedBearing - azimuth
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        // Si el target no está en FOV, mostrar flecha en el borde
        if (abs(delta) > FOV_HORIZONTAL / 2) {
            val arrowX = if (delta > 0) w - 60.dp.toPx() else 60.dp.toPx()
            val arrowY = h / 2

            // Triángulo flecha
            val arrowSize = 20.dp.toPx()
            val path = androidx.compose.ui.graphics.Path().apply {
                if (delta > 0) {
                    moveTo(arrowX + arrowSize, arrowY)
                    lineTo(arrowX - arrowSize / 2, arrowY - arrowSize)
                    lineTo(arrowX - arrowSize / 2, arrowY + arrowSize)
                } else {
                    moveTo(arrowX - arrowSize, arrowY)
                    lineTo(arrowX + arrowSize / 2, arrowY - arrowSize)
                    lineTo(arrowX + arrowSize / 2, arrowY + arrowSize)
                }
                close()
            }
            drawPath(path, Color(0xFF00D9FF))

            // Texto distancia
            val arrowPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 32f
                textAlign = if (delta > 0) Paint.Align.RIGHT else Paint.Align.LEFT
                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
            }
            val distStr = if (distance < 100) "${distance.toInt()}m" else "${"%.0f".format(distance)}m"
            drawContext.canvas.nativeCanvas.drawText(
                "→ ${target.name} $distStr",
                if (delta > 0) arrowX - 10 else arrowX + 10,
                arrowY + arrowSize + 30,
                arrowPaint
            )
        }
    }

    // ── Mini brújula ──

    private fun DrawScope.drawMiniCompass(azimuth: Float, w: Float) {
        val cx = w - 50.dp.toPx()
        val cy = 60.dp.toPx()
        val radius = 25.dp.toPx()

        // Fondo circular
        drawCircle(Color.Black.copy(alpha = 0.5f), radius, Offset(cx, cy))
        drawCircle(
            Color.Green.copy(alpha = 0.3f), radius, Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
        )

        // Aguja norte
        val northAngle = Math.toRadians((-azimuth).toDouble())
        val needleLen = radius * 0.7f
        val nx = cx + (sin(northAngle) * needleLen).toFloat()
        val ny = cy - (cos(northAngle) * needleLen).toFloat()

        drawLine(Color.Red, Offset(cx, cy), Offset(nx, ny), 2.dp.toPx())

        // Aguja sur (más corta)
        val sx = cx - (sin(northAngle) * needleLen * 0.5).toFloat()
        val sy = cy + (cos(northAngle) * needleLen * 0.5).toFloat()
        drawLine(Color.White.copy(alpha = 0.5f), Offset(cx, cy), Offset(sx, sy), 1.dp.toPx())

        // Etiqueta N
        val nPaint = Paint().apply {
            color = android.graphics.Color.RED
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawContext.canvas.nativeCanvas.drawText("N", nx, ny - 8, nPaint)

        // Grados
        val degPaint = Paint().apply {
            color = android.graphics.Color.GREEN
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("${azimuth.toInt()}°", cx, cy + radius + 20, degPaint)
    }

    // ── Indicador de calibración ──

    private fun DrawScope.drawCalibrationIndicator(accuracy: Int, w: Float, h: Float) {
        val label = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Calibración: Alta"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Calibración: Media"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "⚠️ Calibración: Baja"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "❌ Calibración: No fiable"
            else -> "Calibrando..."
        }
        val color = when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF4CAF50)
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFF9800)
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Color(0xFFFF5722)
            else -> Color(0xFFF44336)
        }

        // Barra en la parte superior
        val barWidth = w * when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 1f
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> 0.65f
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> 0.35f
            else -> 0.15f
        }
        drawRect(color=color.copy(alpha = 0.8f), topLeft=Offset(0f, 0f), size=androidx.compose.ui.geometry.Size(barWidth, 4.dp.toPx()))

        // Texto
        if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
            val paint = Paint().apply {
                this.color = color.toArgb()
                textSize = 28f
                textAlign = Paint.Align.LEFT
                setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawContext.canvas.nativeCanvas.drawText(label, 16f, 30f, paint)

            if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                val hintPaint = Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 22f
                    textAlign = Paint.Align.LEFT
                    setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "Mueve el móvil en forma de 8 para calibrar",
                    16f, 56f, hintPaint
                )
            }
        }
    }

    // ── Info bar inferior ──

    private fun DrawScope.drawInfoBar(visible: Int, total: Int, w: Float, h: Float) {
        val bgPaint = Paint().apply {
            color = android.graphics.Color.argb(120, 0, 0, 0)
            style = Paint.Style.FILL
        }
        drawContext.canvas.nativeCanvas.drawRect(0f, h - 40.dp.toPx(), w, h, bgPaint)

        val textPaint = Paint().apply {
            color = android.graphics.Color.GREEN
            textSize = 24f
            textAlign = Paint.Align.LEFT
        }
        drawContext.canvas.nativeCanvas.drawText(
            "AR POIs: $visible/$total visibles • FOV ${FOV_HORIZONTAL.toInt()}°",
            16f, h - 15.dp.toPx(), textPaint
        )
    }

    // ── Cálculo de posición ──

    private data class PosResult(
        val x: Float, val y: Float,
        val isVisible: Boolean, val distance: Float
    )

    private fun calculateScreenPosition(
        userLocation: Location,
        poi: PoiModel,
        azimuth: Float,
        pitch: Float
    ): PosResult {
        val poiLoc = Location("POI").apply {
            latitude = poi.latitude; longitude = poi.longitude
        }
        val distance = userLocation.distanceTo(poiLoc)
        if (distance > MAX_DISTANCE) return PosResult(0f, 0f, false, distance)

        val bearing = userLocation.bearingTo(poiLoc)
        val normalizedBearing = (bearing + 360) % 360
        var delta = normalizedBearing - azimuth
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360

        if (abs(delta) > FOV_HORIZONTAL / 2) {
            return PosResult(0f, 0f, false, distance)
        }

        val x = (0.5f + (delta / FOV_HORIZONTAL)).toFloat()
        val y = (0.5f + (pitch / FOV_VERTICAL)).toFloat().coerceIn(0.1f, 0.9f)

        return PosResult(x, y, true, distance)
    }
}
