package com.georacing.georacing.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * 🏎️ Sistema de Movimiento GeoRacing — Premium Racing Physics
 * Define la "física" de la interfaz con sensación deportiva.
 */
object Motion {
    // ── Durations ──
    const val DurationInstant = 80        // Micro-feedback (tap, press)
    const val DurationFast = 150          // Quick transitions
    const val DurationMedium = 300        // Standard transitions
    const val DurationSlow = 500          // Emphasized transitions
    const val DurationXSlow = 800         // Map / large background transitions
    const val DurationCinematic = 1200    // Splash / hero animations

    // ── Easings ──
    // "Launch" — Aggressive start, smooth landing (button presses, entries)
    val EasingLaunch = CubicBezierEasing(0.16f, 0.0f, 0.13f, 1.0f)
    
    // "Racing" — Sharp acceleration (micro-animations)
    val EasingRacing = CubicBezierEasing(0.25f, 0.0f, 0.15f, 1.0f)
    
    // "Smooth" — Natural flow (screen transitions, panels)
    val EasingSmooth = FastOutSlowInEasing
    
    // "Decelerate" — Fast start, gentle stop (exit animations)
    val EasingDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    
    // "Overshoot" — Sporty bounce (card entrances, FAB)
    val EasingOvershoot = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    val EasingLinear = LinearEasing

    // ── Spring Specs ──
    /** Snappy spring for buttons and interactive elements */
    fun <T> springSnappy() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = 600f
    )
    
    /** Bouncy spring for cards and entrance animations */
    fun <T> springBouncy() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    /** Gentle spring for large panels and navigation */
    fun <T> springGentle() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow
    )
    
    /** Racing spring — aggressive with minimal bounce */
    fun <T> springRacing() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = 800f
    )

    // ── Animation Specs ──
    fun <T> fadeIn() = tween<T>(
        durationMillis = DurationMedium,
        easing = EasingDecelerate
    )
    
    fun <T> slideSmooth() = tween<T>(
        durationMillis = DurationMedium,
        easing = EasingSmooth
    )
    
    fun <T> slideUp() = tween<T>(
        durationMillis = DurationMedium,
        easing = EasingLaunch
    )
    
    fun <T> staggeredEntry(index: Int) = tween<T>(
        durationMillis = DurationMedium,
        delayMillis = index * 50,
        easing = EasingLaunch
    )
    
    fun <T> cinematic() = tween<T>(
        durationMillis = DurationCinematic,
        easing = EasingSmooth
    )
}
