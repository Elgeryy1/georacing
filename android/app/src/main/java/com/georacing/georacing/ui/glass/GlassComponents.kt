package com.georacing.georacing.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


import dev.chrisbanes.haze.hazeChild

/**
 * Core Glass Surface that applies the capture/blur/lens effect.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    config: GlassConfig = LocalGlassConfig.current,
    surfaceTint: androidx.compose.ui.graphics.Color? = null,
    alpha: Float? = null, // Override alpha
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    // Liquid Glass Implementation
    // Priority: Haze Library (Glassmorphism) > Native Fallback (Translucency)
    
    val hazeState = LocalHazeState.current
    val effectiveTint = surfaceTint ?: config.surfaceTint
    val effectiveAlpha = alpha ?: config.surfaceTintAlpha
    
    // Determine background color/alpha based on quality
    val fallbackBackgroundColor = when (config.quality) {
        GlassQuality.Low -> effectiveTint.copy(alpha = 0.95f) 
        GlassQuality.Medium -> effectiveTint.copy(alpha = 0.85f)
        GlassQuality.High -> effectiveTint.copy(alpha = 0.60f) 
    }

    if (config.enabled && 
        config.quality != GlassQuality.Low && 
        hazeState != null) {
        
        // Haze Implementation
        Box(
            modifier = modifier
                .clip(shape)
                .hazeChild(
                    state = hazeState, 
                    style = dev.chrisbanes.haze.HazeStyle(
                        backgroundColor = effectiveTint.copy(alpha = effectiveAlpha),
                        blurRadius = config.blurRadius,
                        noiseFactor = config.noiseFactor,
                        tints = emptyList() // Explicitly select the constructor with 'tints' (1.1.0+)
                    )
                )
                // Optional: Add a subtle border or overlay if needed by design, but haze does the heavy lifting
        ) {
            content()
        }
        
    } else {
        // Native Fallback (Low Power or Haze Unavailable)
        Box(
            modifier = modifier
                .background(fallbackBackgroundColor, shape)
                .clip(shape)
        ) {
            content()
        }
    }
}

/**
 * Pre-configured Glass Bottom Bar
 */
@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig(),
    height: Dp = 80.dp,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        config = config,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Typical BottomBar shape
        content = content
    )
}
