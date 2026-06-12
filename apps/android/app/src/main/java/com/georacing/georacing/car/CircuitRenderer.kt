package com.georacing.georacing.car

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect

class CircuitRenderer {

    private val trackPaint = Paint().apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 30f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val trackSurfacePaint = Paint().apply {
        color = Color.parseColor("#262626")
        style = Paint.Style.STROKE
        strokeWidth = 24f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val centerLinePaint = Paint().apply {
        color = Color.WHITE
        alpha = 80
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val userPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val userStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val destinationPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val guidanceLinePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
        isAntiAlias = true
    }

    fun render(
        canvas: Canvas,
        visibleArea: Rect,
        userLat: Double,
        userLon: Double,
        destLat: Double?,
        destLon: Double?
    ) {
        canvas.drawColor(Color.BLACK)

        val width = visibleArea.width().toFloat()
        val height = visibleArea.height().toFloat()
        val centerX = width / 2
        val centerY = height / 2

        // Scale factor to fit the track
        val scale = 0.8f

        // Simplified track path (Barcelona Catalunya layout)
        // Coordinates are relative to center (0,0) and scaled by width/height
        val trackPath = Path().apply {
            // Start/Finish straight
            moveTo(centerX - width * 0.3f * scale, centerY + height * 0.25f * scale)
            lineTo(centerX + width * 0.1f * scale, centerY + height * 0.25f * scale)

            // Turn 1 (Elf)
            cubicTo(
                centerX + width * 0.15f * scale, centerY + height * 0.25f * scale,
                centerX + width * 0.2f * scale, centerY + height * 0.15f * scale,
                centerX + width * 0.2f * scale, centerY + height * 0.05f * scale
            )

            // Turn 2 (Renault)
            cubicTo(
                centerX + width * 0.2f * scale, centerY - height * 0.05f * scale,
                centerX + width * 0.15f * scale, centerY - height * 0.15f * scale,
                centerX + width * 0.05f * scale, centerY - height * 0.2f * scale
            )

            // Turn 3 (Repsol)
            cubicTo(
                centerX - width * 0.05f * scale, centerY - height * 0.25f * scale,
                centerX - width * 0.15f * scale, centerY - height * 0.25f * scale,
                centerX - width * 0.25f * scale, centerY - height * 0.2f * scale
            )

            // Turns 4-5 (Seat-Wurth)
            cubicTo(
                centerX - width * 0.3f * scale, centerY - height * 0.15f * scale,
                centerX - width * 0.32f * scale, centerY - height * 0.05f * scale,
                centerX - width * 0.3f * scale, centerY
            )

            // Turn 7 (Campsa)
            cubicTo(
                centerX - width * 0.28f * scale, centerY + height * 0.05f * scale,
                centerX - width * 0.25f * scale, centerY + height * 0.1f * scale,
                centerX - width * 0.2f * scale, centerY + height * 0.12f * scale
            )

            // Turns 9-10 (La Caixa-Banc Sabadell)
            lineTo(centerX - width * 0.1f * scale, centerY + height * 0.12f * scale)
            cubicTo(
                centerX - width * 0.05f * scale, centerY + height * 0.12f * scale,
                centerX, centerY + height * 0.15f * scale,
                centerX, centerY + height * 0.2f * scale
            )

            // Turn 12-13 (New chicane)
            cubicTo(
                centerX, centerY + height * 0.23f * scale,
                centerX - width * 0.05f * scale, centerY + height * 0.25f * scale,
                centerX - width * 0.1f * scale, centerY + height * 0.25f * scale
            )

            // Back to start
            lineTo(centerX - width * 0.3f * scale, centerY + height * 0.25f * scale)
            close()
        }

        // Draw track
        canvas.drawPath(trackPath, trackPaint)
        canvas.drawPath(trackPath, trackSurfacePaint)
        canvas.drawPath(trackPath, centerLinePaint)

        // Draw User Position (Simulated relative to track for demo)
        // In a real app, we would project lat/lon to screen coordinates.
        // For this demo, we'll place the user at the start line.
        val userX = centerX - width * 0.3f * scale
        val userY = centerY + height * 0.25f * scale
        canvas.drawCircle(userX, userY, 20f, userPaint)
        canvas.drawCircle(userX, userY, 20f, userStrokePaint)

        // Draw Destination and Guidance Line
        if (destLat != null && destLon != null) {
            // Simulate destination position based on lat/lon relative to circuit center
            // This is a rough approximation for visual feedback in "Rally Mode"
            // Circuit Center: 41.5687, 2.2567
            val circuitLat = 41.5687
            val circuitLon = 2.2567
            val latDiff = (destLat - circuitLat) * 10000 // Scale for visibility
            val lonDiff = (destLon - circuitLon) * 10000

            // Flip Y because screen Y grows downwards, but latitude grows upwards
            val destX = centerX + (lonDiff * 5).toFloat()
            val destY = centerY - (latDiff * 5).toFloat()

            // Draw guidance line
            canvas.drawLine(userX, userY, destX, destY, guidanceLinePaint)

            // Draw destination marker
            canvas.drawCircle(destX, destY, 25f, destinationPaint)
            canvas.drawCircle(destX, destY, 25f, userStrokePaint)
        }
    }
}
