package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.glass.utils.InteractiveHighlight
import com.georacing.georacing.ui.theme.LocalEventVisualStyle
import kotlinx.coroutines.delay
import kotlin.math.tanh

/**
 * A Liquid Glass feature card for the dashboard grid.
 * Uses blur, lens distortion, and interactive highlight effects.
 */
@Composable
fun FeatureCard(
    title: String,
    description: String, // unused in new design
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0
) {
    val backdrop = LocalBackdrop.current
    val visuals = LocalEventVisualStyle.current
    val animationScope = rememberCoroutineScope()
    
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    
    // Entrance Animation
    var hasAnimated by remember { mutableStateOf(false) }
    val entranceScale by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0.6f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "entranceScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "entranceAlpha"
    )
    val entranceOffsetY by animateFloatAsState(
        targetValue = if (hasAnimated) 0f else 20f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "entranceOffsetY"
    )

    LaunchedEffect(Unit) {
        delay(index * 60L) 
        hasAnimated = true
    }

    val shape = MaterialTheme.shapes.large

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer {
                scaleX = entranceScale
                scaleY = entranceScale
                alpha = entranceAlpha
                translationY = entranceOffsetY
            }
            .clickable(onClick = onClick)
    ) {
        // Icon Container with Liquid Glass effect
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(6f.dp.toPx())
                        lens(4f.dp.toPx(), 8f.dp.toPx())
                    },
                    highlight = {
                        Highlight.Ambient.copy(
                            alpha = 0.4f
                            // color parameter removed as it's not supported in Highlight.Ambient copy
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 8.dp,
                            color = accentColor.copy(alpha = 0.3f)
                        )
                    },
                    layerBlock = {
                        val progress = interactiveHighlight.pressProgress
                        val scale = lerp(1f, 1.08f, progress)
                        scaleX = scale
                        scaleY = scale

                        val maxOffset = size.minDimension * 0.15f
                        val offset = interactiveHighlight.offset
                        translationX = maxOffset * tanh(0.03f * offset.x / maxOffset)
                        translationY = maxOffset * tanh(0.03f * offset.y / maxOffset)
                    },
                    onDrawSurface = {
                        // Glass base
                        drawRect(visuals.featureShell)
                        // Accent tint overlay
                        drawRect(accentColor.copy(alpha = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.ELECTRIC) 0.22f else 0.15f), blendMode = BlendMode.Overlay)
                    }
                )
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(30.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title Label
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.NIGHT) 0.2.sp else 0.5.sp
            ),
            color = visuals.featureLabel,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
