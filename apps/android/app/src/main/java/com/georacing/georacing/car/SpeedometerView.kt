package com.georacing.georacing.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View

import android.util.AttributeSet

/**
 * Waze-style speedometer circle overlay for Android Auto.
 * Draws a circular speed indicator with pulsing red when exceeding speed limit.
 */
class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var currentSpeed: Float = 0f
    private var speedLimit: Float = 50f
    private var isOverSpeed: Boolean = false
    
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F1015") // AsphaltGrey
        style = Paint.Style.FILL
        setShadowLayer(16f, 0f, 4f, Color.parseColor("#00F0FF")) // Neon Cyan glow
    }
    
    private val speedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
    }
    
    private val unitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#00F0FF")
    }
    
    private val overSpeedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF2A3C") // Racing Red
        style = Paint.Style.STROKE
        strokeWidth = 8f
        setShadowLayer(20f, 0f, 0f, Color.parseColor("#FF2A3C"))
    }
    
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.parseColor("#00F0FF") // Neon Cyan
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
        val radius = minOf(width, height) / 2f - 10f
        
        // Base Dark Background
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Pulse animation for over-speed
        if (isOverSpeed) {
            pulseAlpha += pulseDirection * 15
            if (pulseAlpha <= 100 || pulseAlpha >= 255) {
                pulseDirection *= -1
            }
            overSpeedPaint.alpha = pulseAlpha
            canvas.drawCircle(centerX, centerY, radius - 4f, overSpeedPaint)
            unitTextPaint.color = Color.parseColor("#FF2A3C")
            backgroundPaint.setShadowLayer(16f, 0f, 4f, Color.parseColor("#FF2A3C"))
        } else {
            canvas.drawCircle(centerX, centerY, radius - 4f, borderPaint)
            unitTextPaint.color = Color.parseColor("#00F0FF")
            backgroundPaint.setShadowLayer(16f, 0f, 4f, Color.parseColor("#00F0FF"))
        }
        
        // Speed number
        speedTextPaint.textSize = radius * 0.8f
        val speedInt = currentSpeed.toInt()
        canvas.drawText(
            speedInt.toString(),
            centerX,
            centerY + speedTextPaint.textSize * 0.3f,
            speedTextPaint
        )
        
        // "km/h" label
        unitTextPaint.textSize = radius * 0.25f
        canvas.drawText(
            "km/h",
            centerX,
            centerY + radius * 0.6f,
            unitTextPaint
        )
        
        // Continuous animation if over speed
        if (isOverSpeed) {
            postInvalidateDelayed(50)
        }
    }
}
