package com.georacing.georacing.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow


object GlassSupport {
    
    /**
     * Detects if running on an emulator where GPU shaders may crash.
     * Checks multiple indicators to reliably detect emulators.
     */
    val isEmulator: Boolean by lazy {
        (Build.FINGERPRINT.startsWith("google/sdk_gphone")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("sdk_gphone")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * True if the device can safely run RuntimeShader-based effects (lens, vibrancy).
     * Requires API 33+ AND a real device (emulators crash on complex shaders).
     */
    val supportsLens: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isEmulator

    /**
     * True if the device can safely run blur effects via RenderEffect.
     * Requires API 31+ AND a real device.
     */
    val supportsBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isEmulator
    
    /**
     * True if the device can run vibrancy (saturation shader).
     * Same requirements as lens.
     */
    val supportsVibrancy: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isEmulator
}

/**
 * Safe version of drawBackdrop that falls back to simple clip+background on emulators.
 * On real devices, uses the full kyant backdrop pipeline.
 * On emulators, uses clip+background to avoid SIGSEGV from RuntimeShader.
 */
fun Modifier.drawBackdropSafe(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit = {},
    highlight: (() -> Highlight?)? = null,
    shadow: (() -> Shadow?)? = null,
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    fallbackColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.6f)
): Modifier {
    return if (!GlassSupport.isEmulator) {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = shape,
            effects = effects,
            highlight = highlight,
            shadow = shadow,
            innerShadow = innerShadow,
            layerBlock = layerBlock,
            onDrawSurface = onDrawSurface
        )
    } else {
        // Emulator fallback: simple clip + background, no shaders
        val s = shape()
        this
            .then(if (layerBlock != null) Modifier.graphicsLayer(layerBlock) else Modifier)
            .shadow(4.dp, s, clip = false, ambientColor = Color.Black.copy(alpha = 0.2f))
            .background(fallbackColor, s)
            .border(1.dp, Color.White.copy(alpha = 0.1f), s)
            .clip(s)
    }
}

/**
 * Builds the appropriate backdrop effects based on quality and device support.
 */

