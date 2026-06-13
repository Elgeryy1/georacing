package com.georacing.georacing.ui.components

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.georacing.georacing.data.parking.ParkingLocation
import com.georacing.georacing.ui.glass.LiquidDialog
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import kotlin.math.roundToInt

@Composable
fun ParkingConfirmationDialog(
    location: Location,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onAddPhoto: () -> Unit // Placeholder for future
) {
    val backdrop = LocalBackdrop.current
    Dialog(onDismissRequest = onDismiss) {
        LiquidDialog(
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            cornerRadius = 32.dp,
            surfaceColor = Color(0xFF0E0E18).copy(alpha = 0.90f)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🚗",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Coche desconectado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¿Guardar la ubicación actual como parking?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "Ignorar",
                            color = Color(0xFFE8253A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8253A)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            "Guardar Parking",
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enhanced ParkingStatusCard with distance display.
 * 
 * @param parkingLocation Saved parking location
 * @param currentLocation User's current location (nullable)
 * @param onNavigate Callback when navigate button is clicked
 */
@Composable
fun ParkingStatusCard(
    parkingLocation: ParkingLocation,
    currentLocation: Location? = null,
    onNavigate: () -> Unit
) {
    // 🛡️ Don't show if coordinates are (0, 0) - invalid data
    if (parkingLocation.latitude == 0.0 && parkingLocation.longitude == 0.0) {
        return
    }
    
    // 📏 Calculate distance using Location.distanceBetween()
    val distanceText = remember(parkingLocation, currentLocation) {
        if (currentLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude,
                currentLocation.longitude,
                parkingLocation.latitude,
                parkingLocation.longitude,
                results
            )
            formatDistance(results[0])
        } else {
            null
        }
    }
    
    val backdrop = LocalBackdrop.current
    
    LiquidCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        cornerRadius = 24.dp,
        surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TU COCHE ESTÁ APARCADO",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (distanceText != null) "A $distanceText" else "Ubicación guardada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onNavigate,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ir")
            }
        }
    }
}

/**
 * Formats distance in meters to human-readable string.
 */
private fun formatDistance(meters: Float): String {
    return when {
        meters < 1000 -> "${meters.roundToInt()}m"
        else -> "${"%.1f".format(meters / 1000)}km"
    }
}
