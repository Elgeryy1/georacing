package com.georacing.georacing.ui.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.georacing.georacing.ui.glass.drawBackdropSafe
import androidx.compose.foundation.shape.RoundedCornerShape
import com.georacing.georacing.ui.glass.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * A glass card container with blur, lens distortion, and optional press effects.
 * Use this as a replacement for GlassCard, Box with rounded corners, etc.
 */
@Composable
fun LiquidCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isInteractive: Boolean = onClick != null,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 8.dp,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier
            .drawBackdropSafe(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    if (GlassSupport.supportsVibrancy) vibrancy()
                    if (GlassSupport.supportsBlur) blur(blurRadius.toPx())
                    if (GlassSupport.supportsLens) lens(6f.dp.toPx(), 12f.dp.toPx())
                },
                highlight = {
                    Highlight.Ambient.copy(
                        alpha = 0.3f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 8.dp,
                        color = Color.Black.copy(alpha = 0.15f)
                    )
                },
                fallbackColor = surfaceColor,
                layerBlock = if (isInteractive) {
                    {
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1.02f, progress)
                        scaleX = scale
                        scaleY = scale

                        val maxOffset = size.minDimension * 0.1f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(0.02f * offset.x / maxOffset)
                        translationY = maxOffset * tanh(0.02f * offset.y / maxOffset)
                    }
                } else null,
                onDrawSurface = {
                    if (surfaceColor.isSpecified) {
                        drawRect(surfaceColor)
                    }
                    if (tint.isSpecified) {
                        drawRect(tint, blendMode = BlendMode.Overlay)
                    }
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = if (isInteractive) null else LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .then(
                if (isInteractive && onClick != null) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * Non-interactive version of LiquidCard (no press effects, just glass appearance)
 */
@Composable
fun LiquidSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 8.dp,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.5f),
    content: @Composable BoxScope.() -> Unit
) {
    LiquidCard(
        backdrop = backdrop,
        modifier = modifier,
        onClick = null,
        isInteractive = false,
        cornerRadius = cornerRadius,
        blurRadius = blurRadius,
        surfaceColor = surfaceColor,
        content = content
    )
}
