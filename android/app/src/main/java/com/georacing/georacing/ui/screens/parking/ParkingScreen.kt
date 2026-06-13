package com.georacing.georacing.ui.screens.parking

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.parking.ParkingLocation
import com.georacing.georacing.data.parking.ParkingRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(
    navController: NavController,
    parkingRepository: ParkingRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Use passed repository or create local instance as fallback
    val repo = remember { parkingRepository ?: ParkingRepository(context) }
    
    // Observe parking location
    val parkingLocation by repo.parkingLocation.collectAsState(initial = null)
    
    // Save state
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🚗 Mi Coche") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    com.georacing.georacing.ui.components.HomeIconButton {
                        navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                            popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            if (parkingLocation != null) {
                // ===== CAR PARKED STATE =====
                ParkingLocationCard(
                    location = parkingLocation!!,
                    onNavigate = {
                        // Open Google Maps for navigation
                        val gmmIntentUri = Uri.parse(
                            "google.navigation:q=${parkingLocation!!.latitude},${parkingLocation!!.longitude}&mode=w"
                        )
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            // Fallback: Open in browser
                            val browserIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${parkingLocation!!.latitude},${parkingLocation!!.longitude}&travelmode=walking")
                            )
                            context.startActivity(browserIntent)
                        }
                    },
                    onClear = {
                        scope.launch {
                            repo.clearParking()
                        }
                    }
                )
            } else {
                // ===== NO PARKING SAVED STATE =====
                NoParkingCard(
                    isSaving = isSaving,
                    error = saveError,
                    onSaveLocation = {
                        scope.launch {
                            isSaving = true
                            saveError = null
                            try {
                                val location = getCurrentLocation(context)
                                if (location != null) {
                                    val parkingLoc = ParkingLocation(
                                        latitude = location.first,
                                        longitude = location.second,
                                        timestamp = System.currentTimeMillis(),
                                        photoUri = null
                                    )
                                    repo.saveParkingLocation(parkingLoc)
                                } else {
                                    saveError = "No se pudo obtener la ubicación. Verifica los permisos GPS."
                                }
                            } catch (e: Exception) {
                                saveError = "Error: ${e.message}"
                            }
                            isSaving = false
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14141C)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "ℹ️ ¿Cómo funciona?",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Cuando desconectes tu coche de Android Auto, se te preguntará si quieres guardar la ubicación.\n" +
                        "• También puedes guardar la ubicación manualmente desde esta pantalla.\n" +
                        "• Toca \"Navegar al coche\" para que Google Maps te guíe de vuelta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        lineHeight = 20.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ParkingLocationCard(
    location: ParkingLocation,
    onNavigate: () -> Unit,
    onClear: () -> Unit
) {
    val timeAgo = remember(location.timestamp) {
        getRelativeTimeString(location.timestamp)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E0E18)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big car icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF22C55E), Color(0xFF0E0E18))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🚗", fontSize = 48.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "COCHE APARCADO",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF22C55E),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
            
            Text(
                "Guardado $timeAgo",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B).copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Navigate Button (Primary)
            Button(
                onClick = onNavigate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("NAVEGAR AL COCHE", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Clear Button (Secondary)
            TextButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Borrar ubicación", color = Color(0xFFEF4444).copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun NoParkingCard(
    isSaving: Boolean,
    error: String?,
    onSaveLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14141C)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Empty state icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF1E1E2A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🅿️", fontSize = 48.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Sin ubicación guardada",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Guarda la ubicación de tu coche para no perderlo en el circuito.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
            
            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSaveLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFFE2E8F0),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDAR UBICACIÓN ACTUAL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: android.content.Context): Pair<Double, Double>? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // Check permission first
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        // Try to get current location with high accuracy
        val location = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()
        
        if (location != null) {
            Pair(location.latitude, location.longitude)
        } else {
            // Fallback to last known location
            val lastLocation = fusedLocationClient.lastLocation.await()
            lastLocation?.let { Pair(it.latitude, it.longitude) }
        }
    } catch (e: Exception) {
        null
    }
}

private fun getRelativeTimeString(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "hace un momento"
        diff < 3600_000 -> "hace ${diff / 60_000} min"
        diff < 86400_000 -> "hace ${diff / 3600_000} hora(s)"
        else -> {
            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
