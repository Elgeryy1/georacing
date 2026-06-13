package com.georacing.georacing.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

import android.util.AttributeSet

/**
 * Premium motorsport speedometer for Android Auto dashboard.
 * 
 * Design: Dark circular gauge with neon cyan glow ring,
 * pulsing red warning when exceeding speed limit,
 * and gradient accent effects. Photorealistic automotive feel.
 */
class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val NEON_CYAN     = "#00F0FF"
        private const val NEON_GREEN    = "#39FF14"
        private const val RACING_RED    = "#FF2A3C"
        private const val ASPHALT       = "#080B14"
        private const val ASPHALT_INNER = "#0C1020"
        private const val DARK_RING     = "#141824"
    }
    
    private var currentSpeed: Float = 0f
    private var speedLimit: Float = 50f
    private var isOverSpeed: Boolean = false
    
    // Outer glow layer (soft halo effect)
    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        color = Color.parseColor(NEON_CYAN)
        alpha = 40
    }
    
    // Main background
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(ASPHALT)
        style = Paint.Style.FILL
    }
    
    // Ring border (neon)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor(NEON_CYAN)
        setShadowLayer(14f, 0f, 0f, Color.parseColor(NEON_CYAN))
    }
    
    // Inner subtle ring
    private val innerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor(DARK_RING)
    }

    private val speedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        color = Color.WHITE
    }
    
    private val unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor(NEON_CYAN)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    
    // Over-speed pulsing ring
    private val overSpeedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(RACING_RED)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        setShadowLayer(18f, 0f, 0f, Color.parseColor(RACING_RED))
    }
    
    private var pulseAlpha = 255
    private var pulseDirection = -1
    
    fun updateSpeed(speed: Float, limit: Float) {
        currentSpeed = speed
        speedLimit = limit
        isOverSpeed = speed > limit
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = minOf(width, height) / 2f - 14f
        
        // ── 1. Outer neon glow halo ───────────────────────
        val glowColor = if (isOverSpeed) RACING_RED else NEON_CYAN
        outerGlowPaint.apply {
            color = Color.parseColor(glowColor)
            alpha = 35
            strokeWidth = 18f
            setShadowLayer(24f, 0f, 0f, Color.parseColor(glowColor))
        }
        canvas.drawCircle(centerX, centerY, radius + 4f, outerGlowPaint)
        
        // ── 2. Dark background with radial gradient ───────
        val bgGradient = RadialGradient(
            centerX, centerY, radius,
            Color.parseColor(ASPHALT_INNER),
            Color.parseColor(ASPHALT),
            Shader.TileMode.CLAMP
        )
        backgroundPaint.shader = bgGradient
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        backgroundPaint.shader = null
        
        // ── 3. Main ring (neon or warning) ────────────────
        if (isOverSpeed) {
            // Pulsing red ring
            pulseAlpha += pulseDirection * 12
            if (pulseAlpha <= 80 || pulseAlpha >= 255) pulseDirection *= -1
            
            overSpeedPaint.alpha = pulseAlpha
            canvas.drawCircle(centerX, centerY, radius - 3f, overSpeedPaint)
            
            // Inner secondary pulse
            overSpeedPaint.alpha = pulseAlpha / 3
            overSpeedPaint.strokeWidth = 2f
            canvas.drawCircle(centerX, centerY, radius - 10f, overSpeedPaint)
            overSpeedPaint.strokeWidth = 6f
            
            unitTextPaint.color = Color.parseColor(RACING_RED)
        } else {
            // Normal neon cyan ring with glow
            ringPaint.color = Color.parseColor(NEON_CYAN)
            ringPaint.setShadowLayer(14f, 0f, 0f, Color.parseColor(NEON_CYAN))
            canvas.drawCircle(centerX, centerY, radius - 3f, ringPaint)
            
            // Subtle inner ring
            canvas.drawCircle(centerX, centerY, radius - 12f, innerRingPaint)
            
            unitTextPaint.color = Color.parseColor(NEON_CYAN)
        }
        
        // ── 4. Speed number (high contrast) ───────────────
        speedTextPaint.textSize = radius * 0.85f
        speedTextPaint.setShadowLayer(4f, 0f, 2f, Color.parseColor("#80000000"))
        val speedInt = currentSpeed.toInt()
        canvas.drawText(
            speedInt.toString(),
            centerX,
            centerY + speedTextPaint.textSize * 0.28f,
            speedTextPaint
        )
        
        // ── 5. "km/h" label ──────────────────────────────
        unitTextPaint.textSize = radius * 0.22f
        unitTextPaint.setShadowLayer(4f, 0f, 0f, Color.parseColor(if (isOverSpeed) RACING_RED else NEON_CYAN))
        canvas.drawText(
            "km/h",
            centerX,
            centerY + radius * 0.62f,
            unitTextPaint
        )
        unitTextPaint.clearShadowLayer()
        
        // Continue animating if over speed
        if (isOverSpeed) {
            postInvalidateDelayed(40)
        }
    }
}
