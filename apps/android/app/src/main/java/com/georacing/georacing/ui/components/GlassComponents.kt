package com.georacing.georacing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.georacing.georacing.ui.theme.*

/**
 * Tarjeta base del sistema Liquid Glass.
 *
 * Se usa como contenedor principal para modulos de informacion.
 * Ahora con inner highlight y borde specular mejorado.
 *
 * @param modifier Modificador para layout (padding, size).
 * @param shape Forma del contenedor (Default: Rounded 24dp).
 * @param content Contenido composable (ColumnScope).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.xl), // Updated to use Token
    accentColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                accentGlow = accentColor
            )
            .padding(Layout.cardPadding),
        content = content
    )
}

/**
 * 🏎️ Botón de acción principal Racing — Diseño F1 Premium.
 * Corte angular agresivo con gradiente sutil.
 */
@Composable
fun RacingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    color: Color = RacingRed
) {
    val shape = CutCornerShape(topStart = 0.dp, bottomEnd = 18.dp, topEnd = 6.dp, bottomStart = 6.dp)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp).fillMaxWidth(),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 1.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * 🏎️ Píldora de estado — LED style con glow.
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .glassSmall(color = color.copy(alpha = 0.25f))
            .border(
                width = 0.5.dp,
                color = color.copy(alpha = 0.4f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * 🏎️ Fila de datos estilo HUD (Head Up Display) — Data telemetry feel.
 * Label pequeño arriba en color terciario, Valor grande abajo en blanco.
 */
@Composable
fun HUDRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    valueColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MetalGrey.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = valueColor
            )
        }
    }
}
