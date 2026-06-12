package com.georacing.georacing.ui.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import com.georacing.georacing.ui.glass.drawBackdropSafe

/**
 * A floating glass top bar with blur and vibrancy effects.
 * Use as a replacement for TopAppBar in screens.
 */
@Composable
fun LiquidTopBar(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    surfaceColor: Color = Color(0xFF1C1C1E).copy(alpha = 0.7f),
    navigationIcon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .drawBackdropSafe(
                backdrop = backdrop,
                shape = { Capsule },
                effects = {
                    if (GlassSupport.supportsVibrancy) vibrancy()
                    if (GlassSupport.supportsBlur) blur(12f.dp.toPx())
                },
                highlight = {
                    Highlight.Ambient.copy(
                        alpha = 0.2f,
                        width = Highlight.Ambient.width / 2f
                    )
                },
                shadow = {
                    Shadow(
                        radius = 12.dp,
                        color = Color.Black.copy(alpha = 0.2f)
                    )
                },
                onDrawSurface = {
                    drawRect(surfaceColor)
                },
                fallbackColor = surfaceColor
            )
            .height(56.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Navigation icon
        if (navigationIcon != null) {
            navigationIcon()
        }
        
        // Title (center weight)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            title()
        }
        
        // Actions
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
