package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
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
import com.georacing.georacing.ui.glass.GlassSupport
import com.georacing.georacing.ui.glass.drawBackdropSafe
import androidx.compose.foundation.shape.RoundedCornerShape
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.glass.utils.InteractiveHighlight
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

    val shape = RoundedCornerShape(22.dp)

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
        // Icon Container with Solid Glass effect (OLED optimized)
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(shape) // Clip before border
                .background(Color(0xFF14141C)) // Solid ultra-dark base
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.4f),
                    shape = shape
                )
                // Maintain the interactive press scale logic
                .graphicsLayer {
                    val progress = interactiveHighlight.pressProgress
                    val animScale = lerp(1f, 1.05f, progress)
                    scaleX = animScale
                    scaleY = animScale

                    val maxOffset = size.minDimension * 0.15f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(0.03f * offset.x / maxOffset)
                    translationY = maxOffset * tanh(0.03f * offset.y / maxOffset)
                    
                    // Add standard shadow for glow effect
                    shadowElevation = 8f
                    this.shape = shape // Use 'this.shape' or the outer 'shape'
                    ambientShadowColor = accentColor
                    spotShadowColor = accentColor
                }
                .then(interactiveHighlight.modifier)
                .then(interactiveHighlight.gestureModifier),
            contentAlignment = Alignment.Center
        ) {
            // Accent tint overlay to replace the old drawRect overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accentColor.copy(alpha = 0.1f))
            )
            
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(32.dp) // Slightly larger icon
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title Label
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
