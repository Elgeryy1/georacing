package com.georacing.georacing.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Premium HUD overlay for GeoRacing Android Auto.
 * Fully opaque dark panel, compact layout, automotive-grade readability.
 */
class NavigationHUD {
    
    // ── Colors ────────────────────────────────────────────
    private val PANEL_BG       = Color.parseColor("#FF0C1020")  // Fully opaque dark
    private val PANEL_BG_INNER = Color.parseColor("#FF101628")  // Slightly lighter inner
    private val ACCENT_CYAN    = Color.parseColor("#00E5FF")
    private val ACCENT_DIM     = Color.parseColor("#33007088")  // Very subtle cyan tint
    private val TEXT_WHITE     = Color.parseColor("#F0F2F5")
    private val TEXT_UNIT      = Color.parseColor("#6B7A8D")
    private val TEXT_SECONDARY = Color.parseColor("#7A8899")
    private val SPEED_OVER     = Color.parseColor("#FF3B4F")
    private val ETA_GREEN      = Color.parseColor("#4ADE80")
    private val DIVIDER_COLOR  = Color.parseColor("#18FFFFFF")
    private val LIMIT_RED      = Color.parseColor("#E53935")
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
    }

    fun createHUDBitmap(
        width: Int,
        height: Int,
        currentSpeedKmh: Int,
        speedLimitKmh: Int?,
        nextInstruction: String,
        distanceToManeuver: Double,
        etaMinutes: Int,
        arrowSymbol: String = "↑"
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val pad = 6f
        val left = pad
        val top = pad
        val right = width - pad
        val bottom = height - pad
        val rect = RectF(left, top, right, bottom)
        val radius = 14f
        
        // ── 1. Solid dark background ──────────────────────
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PANEL_BG
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        
        // Subtle inner gradient (slightly lighter at top)
        val innerRect = RectF(left + 1f, top + 1f, right - 1f, bottom - 1f)
        val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, top, 0f, bottom,
                Color.parseColor("#14FFFFFF"), Color.parseColor("#00000000"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(innerRect, radius, radius, gradPaint)
        
        // Thin border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1AFFFFFF")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
        
        // ── 2. Thin accent line at top ────────────────────
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left + 20f, top, right - 20f, top,
                ACCENT_CYAN, Color.parseColor("#0066CC"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(
            RectF(left + 20f, top + 1.5f, right - 20f, top + 3.5f),
            2f, 2f, accentPaint
        )
        
        // ── Layout positions ──────────────────────────────
        var y = top + 22f
        val xLeft = left + 14f
        val contentWidth = right - left - 28f
        
        // ── 3. SPEED ROW ─────────────────────────────────
        val isOver = speedLimitKmh != null && currentSpeedKmh > speedLimitKmh
        
        // Speed number
        textPaint.apply {
            color = if (isOver) SPEED_OVER else TEXT_WHITE
            textSize = 38f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText("$currentSpeedKmh", xLeft, y + 30f, textPaint)
        
        // "km/h" label next to speed
        val speedW = textPaint.measureText("$currentSpeedKmh")
        textPaint.apply {
            color = TEXT_UNIT
            textSize = 12f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        canvas.drawText("km/h", xLeft + speedW + 4f, y + 30f, textPaint)
        
        // Speed limit badge (compact, right-aligned)
        if (speedLimitKmh != null) {
            val badgeX = right - 46f
            val badgeY = y + 16f
            val badgeR = 16f
            
            // Red ring
            val ringP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = LIMIT_RED
            }
            canvas.drawCircle(badgeX, badgeY, badgeR, ringP)
            
            // White fill
            val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.WHITE
            }
            canvas.drawCircle(badgeX, badgeY, badgeR - 3f, fillP)
            
            // Number
            textPaint.apply {
                color = Color.parseColor("#222222")
                textSize = 14f
                typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("$speedLimitKmh", badgeX, badgeY + 5f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
        }
        
        y += 42f
        
        // ── 4. Divider ───────────────────────────────────
        val divP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = DIVIDER_COLOR
        }
        canvas.drawRect(xLeft, y, right - 14f, y + 1f, divP)
        y += 10f
        
        // ── 5. MANEUVER ROW ──────────────────────────────
        // Arrow icon
        textPaint.apply {
            color = ACCENT_CYAN
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(arrowSymbol, xLeft, y + 14f, textPaint)
        
        // Distance
        textPaint.apply {
            color = TEXT_WHITE
            textSize = 16f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }
        canvas.drawText(formatDistance(distanceToManeuver), xLeft + 26f, y + 14f, textPaint)
        
        // Instruction (truncated)
        textPaint.apply {
            color = TEXT_SECONDARY
            textSize = 11f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val maxW = contentWidth - 30f
        val truncInstr = ellipsize(nextInstruction, textPaint, maxW)
        canvas.drawText(truncInstr, xLeft + 26f, y + 28f, textPaint)
        
        y += 36f
        
        // ── 6. ETA ROW ───────────────────────────────────
        // Green dot
        val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ETA_GREEN
            style = Paint.Style.FILL
        }
        canvas.drawCircle(xLeft + 5f, y + 4f, 3f, dotP)
        
        textPaint.apply {
            color = ETA_GREEN
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val etaStr = if (etaMinutes < 60) "$etaMinutes min" else "${etaMinutes / 60}h ${etaMinutes % 60}min"
        canvas.drawText("ETA $etaStr", xLeft + 16f, y + 9f, textPaint)
        
        return bitmap
    }
    
    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        for (i in text.length downTo 1) {
            val candidate = text.substring(0, i) + "…"
            if (paint.measureText(candidate) <= maxWidth) return candidate
        }
        return "…"
    }
    
    private fun formatDistance(meters: Double): String {
        return when {
            meters < 100 -> "${meters.toInt()} m"
            meters < 1000 -> "${(meters / 100).toInt() * 100} m"
            else -> String.format("%.1f km", meters / 1000)
        }
    }
}
