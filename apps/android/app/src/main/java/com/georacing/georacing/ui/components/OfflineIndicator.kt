package com.georacing.georacing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LiquidPill
import com.georacing.georacing.ui.glass.LocalBackdrop

/**
 * Indicador de conectividad que aparece en la parte superior de la pantalla.
 * 
 * - Cuando está ONLINE: No muestra nada
 * - Cuando está OFFLINE: Muestra banner con icono BLE pulsando
 * 
 * Uso:
 * ```
 * Box {
 *     // Tu contenido
 *     OfflineIndicator(
 *         isOnline = isOnline,
 *         bleBeaconsDetected = detectedBeacons.size
 *     )
 * }
 * ```
 */
@Composable
fun OfflineIndicator(
    isOnline: Boolean,
    bleBeaconsDetected: Int = 0,
    modifier: Modifier = Modifier
) {
    // Animación de pulso para icono BLE
    val infiniteTransition = rememberInfiniteTransition(label = "ble_pulse")
    val blePulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ble_pulse_alpha"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isOnline) Color.Transparent else Color(0xFFE8253A),
        animationSpec = tween(500),
        label = "bg_color"
    )

    val backdrop = LocalBackdrop.current
    
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier.fillMaxWidth()
    ) {
        LiquidCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 0.dp, // Full width panel
            surfaceColor = backgroundColor.copy(alpha = 0.8f),
            tint = backgroundColor.copy(alpha = 0.2f),
            blurRadius = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
            // Icono offline
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Sin conexión WiFi",
                tint = Color(0xFFF8FAFC),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "SIN CONEXIÓN",
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Icono BLE pulsando
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "Estado Bluetooth",
                tint = Color(0xFFF8FAFC).copy(alpha = blePulse.coerceIn(0f, 1f)),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = if (bleBeaconsDetected > 0) 
                    "$bleBeaconsDetected BALIZAS" 
                else 
                    "BUSCANDO BALIZAS...",
                color = Color(0xFFF8FAFC).copy(alpha = 0.9f),
                letterSpacing = 1.5.sp
            )
        }
    }
    }
}

/**
 * Versión compacta del indicador para usar en TopAppBar.
 */
@Composable
fun OfflineChip(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    AnimatedVisibility(visible = !isOnline, modifier = modifier) {
        LiquidPill(
            backdrop = backdrop,
            surfaceColor = Color(0xFFE8253A).copy(alpha = 0.8f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = Color(0xFFF8FAFC),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "BLE",
                color = Color(0xFFF8FAFC),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
    }
}
