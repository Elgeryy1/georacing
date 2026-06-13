package com.georacing.georacing.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.georacing.georacing.ui.glass.drawBackdropSafe
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * A glass dialog/modal overlay with strong blur and lens effects.
 * Use for confirmation dialogs, alerts, and modals.
 */
@Composable
fun LiquidDialog(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.8f),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier
            .drawBackdropSafe(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    if (GlassSupport.supportsVibrancy) vibrancy()
                    if (GlassSupport.supportsBlur) blur(16f.dp.toPx())
                    if (GlassSupport.supportsLens) lens(8f.dp.toPx(), 16f.dp.toPx())
                },
                highlight = {
                    Highlight.Ambient.copy(
                        alpha = 0.4f,
                        width = Highlight.Ambient.width * 1.5f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 24.dp,
                        color = Color.Black.copy(alpha = 0.3f)
                    )
                },
                innerShadow = {
                    InnerShadow(
                        radius = 2.dp,
                        alpha = 0.2f
                    )
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                },
                fallbackColor = surfaceColor
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Glass pill for status indicators, badges, etc.
 */
@Composable
fun LiquidPill(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.6f),
    tint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .drawBackdropSafe(
                backdrop = backdrop,
                shape = { com.kyant.shapes.Capsule },
                effects = {
                    if (GlassSupport.supportsVibrancy) vibrancy()
                    if (GlassSupport.supportsBlur) blur(4f.dp.toPx())
                },
                highlight = {
                    Highlight.Ambient.copy(alpha = 0.25f)
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                    if (tint != Color.Unspecified) {
                        drawRect(tint.copy(alpha = 0.3f))
                    }
                },
                fallbackColor = surfaceColor
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}
