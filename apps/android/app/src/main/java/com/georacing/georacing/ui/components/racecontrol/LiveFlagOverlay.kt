package com.georacing.georacing.ui.components.racecontrol

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.glass.LiquidSurface
import com.georacing.georacing.ui.glass.LocalBackdrop

@Composable
fun LiveFlagOverlay(
    state: CircuitState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flag_pulse")

    // Animations
    val heavyPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heavy_pulse"
    )

    val softBlinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "soft_blink"
    )

    // Main Container
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                // Full Screen Red Warning Border for Red Flag / Evacuation
                if (state.mode == CircuitMode.RED_FLAG || state.mode == CircuitMode.EVACUATION) {
                    Modifier.border(
                        width = 8.dp,
                        color = CircuitStop.copy(alpha = heavyPulseAlpha)
                    )
                } else {
                    Modifier
                }
            )
            .padding(top = 40.dp) // Avoid status bar slightly
    ) {
        when (state.mode) {
            CircuitMode.RED_FLAG, CircuitMode.EVACUATION -> {
                RaceControlBanner(
                    color = CircuitStop,
                    title = "SESSION STOPPED",
                    subtitle = state.message?.uppercase() ?: "RETURN TO PITS",
                    alpha = 1f, // Always visible
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            CircuitMode.SAFETY_CAR, CircuitMode.VSC -> {
                RaceControlBanner(
                    color = StatusAmber,
                    title = if (state.mode == CircuitMode.VSC) "VIRTUAL SAFETY CAR" else "SAFETY CAR",
                    subtitle = "REDUCE SPEED",
                    alpha = 1f, // Solid
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            CircuitMode.YELLOW_FLAG -> {
                RaceControlBanner(
                    color = StatusAmber,
                    title = "YELLOW FLAG",
                    subtitle = "SECTOR 2 - HAZARD", // Hardcoded per requirement or use state.message
                    alpha = softBlinkAlpha,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            CircuitMode.GREEN_FLAG -> {
                // Subtle Green Line
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.3f)
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, CircuitGreen, Color.Transparent)
                            )
                        )
                )
            }
            CircuitMode.NORMAL -> {
                // Invisible
            }
            else -> {}
        }
    }
}

@Composable
private fun RaceControlBanner(
    color: Color,
    title: String,
    subtitle: String,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    LiquidSurface(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        surfaceColor = color.copy(alpha = 0.7f * alpha)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = title,
            color = Color(0xFFF8FAFC),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.alpha(alpha)
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = Color(0xFFF8FAFC).copy(alpha = 0.9f),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
    }
}
