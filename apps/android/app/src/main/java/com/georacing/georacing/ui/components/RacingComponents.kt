package com.georacing.georacing.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.georacing.georacing.ui.theme.*

// ═══════════════════════════════════════════════════════
// 🏎️  GeoRacing — Industrial Grade Components (Phase 6)
// ═══════════════════════════════════════════════════════
// Cero blur, cero transparencias, cero overdraw.
// Alto contraste. Botones enormes para interactuar en movimiento.

enum class RacingStyle {
    DEFAULT, 
    ACTIVE, 
    DANGER
}

/**
 * Contenedor base plano. Reemplaza a LiquidCard.
 * Fondo AsphaltDark, sin elevación. Bordes duros.
 */
@Composable
fun RacingCard(
    modifier: Modifier = Modifier,
    style: RacingStyle = RacingStyle.DEFAULT,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val borderColor = when (style) {
        RacingStyle.DEFAULT -> AsphaltLight
        RacingStyle.ACTIVE -> KerbYellow
        RacingStyle.DANGER -> CatalunyaRed
    }

    Surface(
        color = AsphaltDark,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 0.dp, // 0 Overdraw GPU
        tonalElevation = 0.dp,
        border = BorderStroke(if (style != RacingStyle.DEFAULT) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Botón masivo para el piloto/staff. Min height 56dp.
 */
@Composable
fun RacingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    val containerColor = if (isPrimary) CatalunyaRed else Color.Transparent
    val contentColor = KerbWhite
    val borderStroke = if (isPrimary) null else BorderStroke(2.dp, KerbWhite)

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        shape = MaterialTheme.shapes.small, // Sharp corners
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AsphaltLight,
            disabledContentColor = MutedText
        ),
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp) // Massivo para interactuar sin mirar
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Barra superior ultra-minimalista, fondo negro OLED nativo.
 */
@Composable
fun RacingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    logoSlot: @Composable (() -> Unit)? = null,
    statusSlot: @Composable (() -> Unit)? = null
) {
    Surface(
        color = TarmacBlack,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoSlot != null) {
                    logoSlot()
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = KerbWhite
                )
            }
            if (statusSlot != null) {
                statusSlot()
            }
        }
    }
}

/**
 * Badge estilo telemetría, altamente visible.
 * Usar para tags como "LIVE", "OFFLINE", "DANGER".
 */
@Composable
fun TelemetryBadge(
    text: String,
    style: RacingStyle = RacingStyle.DEFAULT,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (style) {
        RacingStyle.DEFAULT -> AsphaltLight to KerbWhite
        RacingStyle.ACTIVE -> KerbYellow to TarmacBlack
        RacingStyle.DANGER -> CatalunyaRed to KerbWhite
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50)) // Píldora
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = textColor
        )
    }
}
