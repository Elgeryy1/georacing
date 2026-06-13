package com.georacing.georacing.ui.components.contextual

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.ui.components.racecontrol.RaceControlWidget
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.*

/**
 * Context state enum - determines what the contextual card shows.
 * Priority: Emergency > Evacuation > RedFlag > SafetyCar > Offline > Route > Race > Status
 * Mirrors iOS: ContextState.swift
 */
enum class ContextState {
    EMERGENCY,
    EVACUATION,
    RED_FLAG,
    SAFETY_CAR,
    OFFLINE,
    ROUTE_GUIDANCE,
    RACE_LIVE,
    CIRCUIT_STATUS
}

/**
 * ContextualCardWidget — Smart card that changes content based on circuit state.
 * Mirrors iOS: ContextualCardView.swift + ContextualCardViewModel.swift
 *
 * Priorities:
 * 1. Emergency/Evacuation → Red pulsing card
 * 2. Red Flag → Dark red card
 * 3. Safety Car → Orange card
 * 4. Offline → Grey card
 * 5. Route Guidance → Blue card (if navigating)
 * 6. Race Live → Circuit status card
 * 7. Default → Green status card
 */
@Composable
fun ContextualCardWidget(
    circuitState: CircuitState?,
    isOnline: Boolean = true,
    isNavigating: Boolean = false,
    navigationInstruction: String? = null,
    navigationDistance: String? = null,
    modifier: Modifier = Modifier
) {
    val contextState = remember(circuitState, isOnline, isNavigating) {
        resolveContextState(circuitState, isOnline, isNavigating)
    }

    val backdrop = LocalBackdrop.current

    AnimatedContent(
        targetState = contextState,
        transitionSpec = {
            fadeIn(tween(400)) + slideInVertically(tween(400)) { -30 } togetherWith
                    fadeOut(tween(300)) + slideOutVertically(tween(300)) { 30 }
        },
        label = "contextual_card"
    ) { state ->
        when (state) {
            ContextState.EMERGENCY, ContextState.EVACUATION -> EmergencyWidget(
                circuitState = circuitState,
                modifier = modifier
            )
            ContextState.RED_FLAG -> FlagWidget(
                title = "BANDERA ROJA",
                subtitle = circuitState?.message ?: "Sesión detenida",
                icon = Icons.Default.Flag,
                backgroundColor = Color(0xFF8B0000),
                accentColor = RacingRed,
                modifier = modifier
            )
            ContextState.SAFETY_CAR -> FlagWidget(
                title = "SAFETY CAR",
                subtitle = circuitState?.message ?: "Precaución en pista",
                icon = Icons.Default.DirectionsCar,
                backgroundColor = Color(0xFF5C3D00),
                accentColor = StatusAmber,
                modifier = modifier
            )
            ContextState.OFFLINE -> OfflineWidget(modifier = modifier)
            ContextState.ROUTE_GUIDANCE -> RouteGuidanceWidget(
                instruction = navigationInstruction ?: "",
                distance = navigationDistance ?: "",
                modifier = modifier
            )
            ContextState.RACE_LIVE -> RaceLiveWidget(
                circuitState = circuitState,
                modifier = modifier
            )
            ContextState.CIRCUIT_STATUS -> CircuitStatusWidget(
                circuitState = circuitState,
                modifier = modifier
            )
        }
    }
}

/**
 * Emergency card — pulsing red, evacuation instructions
 */
@Composable
private fun EmergencyWidget(
    circuitState: CircuitState?,
    modifier: Modifier = Modifier
) {
    val pulseAnim = rememberInfiniteTransition(label = "emergency_pulse")
    val pulseAlpha by pulseAnim.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        RacingRed.copy(alpha = pulseAlpha * 0.9f),
                        Color(0xFF8B0000).copy(alpha = pulseAlpha * 0.7f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "EMERGENCIA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "ORDEN DE EVACUACIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                circuitState?.message ?: "Sigue las instrucciones del personal y dirígete a la salida más cercana.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

/**
 * Flag widget — Red flag or Safety Car
 */
@Composable
private fun FlagWidget(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    LiquidCard(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        surfaceColor = backgroundColor.copy(alpha = 0.85f),
        tint = accentColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * Offline widget — no connection indicator
 */
@Composable
private fun OfflineWidget(modifier: Modifier = Modifier) {
    val backdrop = LocalBackdrop.current
    val visuals = LocalEventVisualStyle.current
    LiquidCard(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        surfaceColor = visuals.panelSurfaceStrong,
        tint = visuals.panelMuted.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.WifiOff,
                contentDescription = null,
                tint = visuals.navSelected,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "SIN CONEXIÓN",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = visuals.heroHeadlineColor,
                    letterSpacing = 1.sp
                )
                Text(
                    "Usando datos en caché. Algunas funciones pueden estar limitadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = visuals.heroBodyColor
                )
            }
        }
    }
}

/**
 * Route guidance widget — turn-by-turn instruction
 */
@Composable
private fun RouteGuidanceWidget(
    instruction: String,
    distance: String,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    val visuals = LocalEventVisualStyle.current
    LiquidCard(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        surfaceColor = visuals.panelSurfaceStrong,
        tint = visuals.navSelected.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(visuals.navSelected.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = visuals.navSelected,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    instruction,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = visuals.heroHeadlineColor
                )
                Text(
                    distance,
                    style = MaterialTheme.typography.bodySmall,
                    color = visuals.heroBodyColor
                )
            }
        }
    }
}

@Composable
private fun RaceLiveWidget(
    circuitState: CircuitState?,
    modifier: Modifier = Modifier
) {
    val visuals = LocalEventVisualStyle.current
    if (circuitState == null) {
        CircuitStatusWidget(
            circuitState = null,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!circuitState.message.isNullOrBlank()) {
            val backdrop = LocalBackdrop.current
            LiquidCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                surfaceColor = visuals.panelSurface,
                tint = visuals.navSelected.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(visuals.navSelected)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = circuitState.message ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = visuals.heroHeadlineColor
                    )
                }
            }
        }

        RaceControlWidget(
            state = circuitState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Default circuit status widget — green/normal state
 * Redesigned to match CircuitStatusCard style
 */
@Composable
private fun CircuitStatusWidget(
    circuitState: CircuitState?,
    modifier: Modifier = Modifier
) {
    val visuals = LocalEventVisualStyle.current
    val mode = circuitState?.mode ?: CircuitMode.NORMAL
    val statusColor = when (mode) {
        CircuitMode.NORMAL -> StatusGreen
        CircuitMode.GREEN_FLAG -> StatusGreen
        CircuitMode.SAFETY_CAR -> StatusAmber
        CircuitMode.YELLOW_FLAG -> StatusAmber
        CircuitMode.VSC -> StatusAmber
        CircuitMode.RED_FLAG -> RacingRed
        CircuitMode.EVACUATION -> RacingRed
        CircuitMode.UNKNOWN -> TextTertiary
    }
    val statusText = when (mode) {
        CircuitMode.NORMAL -> "PISTA LIBRE"
        CircuitMode.GREEN_FLAG -> "BANDERA VERDE"
        CircuitMode.YELLOW_FLAG -> "PRECAUCIÓN"
        CircuitMode.VSC -> "VSC"
        CircuitMode.SAFETY_CAR -> "SAFETY CAR"
        CircuitMode.RED_FLAG -> "BANDERA ROJA"
        CircuitMode.EVACUATION -> "EVACUACIÓN"
        CircuitMode.UNKNOWN -> "DESCONECTADO"
    }

    // Pulse animation for status dot
    val pulseAnim = rememberInfiniteTransition(label = "ctx_pulse")
    val pulseRadius by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "ctx_pulse_r"
    )

    // Format timestamp
    val formattedTime = remember(circuitState?.updatedAt) {
        try {
            val raw = circuitState?.updatedAt ?: ""
            if (raw.isNotBlank()) {
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val outputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val date = inputFormat.parse(raw)
                if (date != null) outputFormat.format(date) else ""
            } else ""
        } catch (_: Exception) { "" }
    }

    val backdrop = LocalBackdrop.current
    LiquidCard(
        backdrop = backdrop,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        surfaceColor = visuals.panelSurfaceStrong,
        tint = statusColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Top: Flag icon + title + pulse dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flag icon in accent circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.2f),
                                    Color(0xFF1E293B).copy(alpha = 0.5f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = visuals.heroHeadlineColor,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                    if (!circuitState?.message.isNullOrBlank()) {
                        Text(
                            circuitState?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = visuals.heroBodyColor,
                            maxLines = 1
                        )
                    }
                }

                // Pulse dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .drawBehind {
                            drawCircle(statusColor.copy(alpha = 0.15f), radius = size.minDimension * pulseRadius)
                        }
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Bottom: Temperature + Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                circuitState?.temperature?.let { temp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = visuals.panelMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            temp,
                            style = MaterialTheme.typography.labelMedium,
                            color = visuals.panelMuted
                        )
                    }
                }
                if (formattedTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = visuals.panelMuted.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            color = visuals.panelMuted.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Resolves which context state should be shown based on current conditions.
 */
private fun resolveContextState(
    circuitState: CircuitState?,
    isOnline: Boolean,
    isNavigating: Boolean
): ContextState {
    val mode = circuitState?.mode

    // Priority 1: Emergency/Evacuation
    if (mode == CircuitMode.EVACUATION) return ContextState.EVACUATION

    // Priority 2: Red Flag
    if (mode == CircuitMode.RED_FLAG) return ContextState.RED_FLAG

    // Priority 3: Safety Car
    if (mode == CircuitMode.SAFETY_CAR) return ContextState.SAFETY_CAR

    // Priority 4: Offline
    if (!isOnline) return ContextState.OFFLINE

    // Priority 5: Route guidance
    if (isNavigating) return ContextState.ROUTE_GUIDANCE

    if (circuitState.hasLiveSession()) return ContextState.RACE_LIVE

    // Default: Circuit status
    return ContextState.CIRCUIT_STATUS
}

private fun CircuitState?.hasLiveSession(): Boolean {
    val session = this?.sessionInfo ?: return false
    return session.currentLap > 0 || session.topDrivers.isNotEmpty() || session.sessionTime != "00:00:00"
}
