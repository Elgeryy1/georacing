package com.georacing.georacing.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface

/**
 * HUD (Heads-Up Display) para mostrar información de navegación en el mapa.
 * 
 * Muestra:
 * - Velocidad actual y límite de velocidad
 * - Próxima instrucción con distancia
 * - Tiempo estimado de llegada
 * 
 * Se dibuja como un overlay en la esquina superior del mapa.
 */
class NavigationHUD {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.LEFT
    }
    
    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#DD000000") // Negro semi-transparente
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val accentPaint = Paint().apply {
        color = Color.parseColor("#4285F4") // Azul Google
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val warningPaint = Paint().apply {
        color = Color.parseColor("#EA4335") // Rojo Google
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    /**
     * Dibuja el HUD en un bitmap que luego se puede superponer en el mapa.
     * 
     * @param width Ancho del canvas
     * @param height Alto del canvas
     * @param currentSpeedKmh Velocidad actual en km/h
     * @param speedLimitKmh Límite de velocidad (null si no disponible)
     * @param nextInstruction Próxima instrucción de navegación
     * @param distanceToManeuver Distancia a la próxima maniobra en metros
     * @param etaMinutes ETA en minutos
     * @return Bitmap con el HUD dibujado
     */
    fun createHUDBitmap(
        width: Int,
        height: Int,
        currentSpeedKmh: Int,
        speedLimitKmh: Int?,
        nextInstruction: String,
        distanceToManeuver: Double,
        etaMinutes: Int,
        arrowSymbol: String = "↑" // Default straight
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Ajustar dimensiones para HUD más compacto
        val padding = 8f
        val hudWidth = width.toFloat() - (padding * 2)
        val hudHeight = height.toFloat() - (padding * 2)
        
        // Posición: esquina superior izquierda
        val left = padding
        val top = padding
        val right = left + hudWidth
        val bottom = top + hudHeight
        
        // Fondo redondeado más compacto
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, 12f, 12f, backgroundPaint)
        
        // Línea superior azul (acento) más delgada
        val accentRect = RectF(left, top, right, top + 4f)
        canvas.drawRoundRect(accentRect, 12f, 12f, accentPaint)
        
        var yPos = top + 24f
        
        // 1. VELOCÍMETRO (más compacto)
        drawSpeedometer(canvas, left + 12f, yPos, currentSpeedKmh, speedLimitKmh)
        
        yPos += 50f
        
        // 2. PRÓXIMA INSTRUCCIÓN (compacta)
        drawNextInstruction(canvas, left + 12f, yPos, nextInstruction, distanceToManeuver, arrowSymbol)
        
        yPos += 35f
        
        // 3. ETA (compacta)
        drawETA(canvas, left + 20f, yPos, etaMinutes)
        
        return bitmap
    }
    
    private fun drawSpeedometer(
        canvas: Canvas,
        x: Float,
        y: Float,
        currentSpeed: Int,
        speedLimit: Int?
    ) {
        // Velocidad actual (más pequeña para HUD compacto)
        paint.apply {
            color = if (speedLimit != null && currentSpeed > speedLimit) {
                Color.parseColor("#EA4335") // Rojo si excede
            } else {
                Color.WHITE
            }
            textSize = 36f  // Reducido de 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("$currentSpeed", x, y, paint)
        
        // "km/h" pequeño
        paint.apply {
            color = Color.parseColor("#9AA0A6")
            textSize = 14f  // Reducido de 18f
            typeface = Typeface.DEFAULT
        }
        val speedWidth = paint.measureText("$currentSpeed")
        canvas.drawText("km/h", x + speedWidth + 6f, y, paint)
        
        // Límite de velocidad (círculo rojo más pequeño)
        if (speedLimit != null) {
            val limitX = x + 140f  // Más cerca
            val limitY = y - 20f   // Más cerca
            
            // Círculo rojo exterior más pequeño
            paint.apply {
                color = Color.parseColor("#EA4335")
                style = Paint.Style.STROKE
                strokeWidth = 4f  // Más delgado
            }
            canvas.drawCircle(limitX, limitY, 22f, paint)  // Más pequeño
            
            // Fondo blanco interior
            paint.apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(limitX, limitY, 18f, paint)
            
            // Número del límite
            paint.apply {
                color = Color.BLACK
                textSize = 16f  // Reducido de 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("$speedLimit", limitX, limitY + 8f, paint)
            
            // Resetear alineación
            paint.textAlign = Paint.Align.LEFT
        }
    }
    
    private fun drawNextInstruction(
        canvas: Canvas,
        x: Float,
        y: Float,
        instruction: String,
        distance: Double,
        arrowSymbol: String
    ) {
        // Icono de flecha más pequeño
        paint.apply {
            color = Color.parseColor("#4285F4")
            textSize = 18f  // Reducido de 24f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(arrowSymbol, x, y, paint)
        
        // Distancia más compacta
        paint.apply {
            color = Color.WHITE
            textSize = 16f  // Reducido de 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val distText = formatDistance(distance)
        canvas.drawText(distText, x + 28f, y, paint)
        
        // Instrucción más corta para espacio reducido
        paint.apply {
            color = Color.parseColor("#E8EAED")
            textSize = 13f  // Reducido de 16f
            typeface = Typeface.DEFAULT
        }
        val truncatedInstruction = if (instruction.length > 25) {  // Más corto
            instruction.substring(0, 22) + "..."
        } else {
            instruction
        }
        canvas.drawText(truncatedInstruction, x + 28f, y + 18f, paint)  // Menos espacio
    }
    
    private fun drawETA(canvas: Canvas, x: Float, y: Float, etaMinutes: Int) {
        paint.apply {
            color = Color.parseColor("#34A853") // Verde Google
            textSize = 13f  // Reducido de 16f
            typeface = Typeface.DEFAULT
        }
        canvas.drawText("⏱", x, y, paint)
        
        paint.apply {
            color = Color.parseColor("#E8EAED")
            textSize = 16f
        }
        val etaText = if (etaMinutes < 60) {
            "$etaMinutes min"
        } else {
            val hours = etaMinutes / 60
            val mins = etaMinutes % 60
            "${hours}h ${mins}min"
        }
        canvas.drawText("ETA: $etaText", x + 30f, y, paint)
    }
    
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 100 -> "${meters.toInt()} m"
            meters < 1000 -> "${(meters / 100).toInt() * 100} m"
            else -> String.format("%.1f km", meters / 1000)
        }
    }
}
