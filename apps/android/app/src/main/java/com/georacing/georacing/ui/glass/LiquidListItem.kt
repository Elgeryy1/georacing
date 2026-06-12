package com.georacing.georacing.ui.glass

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.georacing.georacing.ui.glass.drawBackdropSafe
import androidx.compose.foundation.shape.RoundedCornerShape
import com.georacing.georacing.ui.glass.utils.InteractiveHighlight
import kotlin.math.tanh

/**
 * A glass list item row with blur effects and press animations.
 * Use as a replacement for settings rows, POI items, alert items, etc.
 */
@Composable
fun LiquidListItem(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.4f),
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier
            .fillMaxWidth()
            .drawBackdropSafe(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    if (GlassSupport.supportsVibrancy) vibrancy()
                    if (GlassSupport.supportsBlur) blur(4f.dp.toPx())
                },
                fallbackColor = surfaceColor,
                layerBlock = {
                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1.01f, progress)
                    scaleX = scale
                    scaleY = scale

                    val maxOffset = size.minDimension * 0.05f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.02f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.02f * offset.y / maxOffset)
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .height(60.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading content (icon)
        if (leadingContent != null) {
            leadingContent()
        }
        
        // Main content
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        
        // Trailing content (switch, arrow, etc.)
        if (trailingContent != null) {
            trailingContent()
        }
    }
}
