package com.georacing.georacing.ui.screens.ar

import android.Manifest
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.georacing.georacing.car.PoiRepository
import com.georacing.georacing.data.sensors.OrientationEngine
import com.georacing.georacing.di.AppContainer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARNavigationScreen(
    appContainer: AppContainer?,
    onBack: () -> Unit
) {
    if (appContainer == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: Dependencies missing") }
        return
    }

    // 1. Energy Check
    val energyProfile by appContainer.energyMonitor.energyProfile.collectAsStateWithLifecycle()
    if (!energyProfile.canUseAR) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚠️ AR Desactivada",
                    color = Color.Red,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Batería baja (<30%) o Modo Ahorro activo.",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onBack) {
                    Text("Volver")
                }
            }
        }
        return
    }

    // 2. Permissions
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de cámara para AR", color = Color.White)
        }
        return
    }

    // 3. Sensor & Location Logic
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val orientationEngine = remember { OrientationEngine(context) }
    val orientationState by orientationEngine.getOrientationFlow().collectAsStateWithLifecycle(
        initialValue = OrientationEngine.Orientation(0f, 0f),
        lifecycleOwner = lifecycleOwner
    )

    // Ubicación del usuario (parking guardado o posición por defecto del circuito)
    val userLocationState = appContainer.parkingRepository.parkingLocation.collectAsStateWithLifecycle(initialValue = null)
    
    val userLocation = remember(userLocationState.value) {
        val parkingLoc = userLocationState.value
        if (parkingLoc != null) {
            Location("GPS").apply { 
                latitude = parkingLoc.latitude
                longitude = parkingLoc.longitude 
                altitude = 0.0
            }
        } else {
            Location("GPS").apply { 
                latitude = 41.569
                longitude = 2.254
                altitude = 0.0
            }
        }
    }

    val pois = remember { PoiRepository.getAllPois() }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // A. Camera Layer
        ARCameraView()

        // B. Enhanced AR Overlay (con brújula, calibración, POIs por categoría)
        com.georacing.georacing.features.ar.AREnhancedOverlay.EnhancedOverlay(
            pois = pois,
            userLocation = userLocation,
            azimuth = orientationState.azimuth,
            pitch = orientationState.pitch,
            compassAccuracy = android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        )

        // C. Status Badge
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .background(
                    Color.Black.copy(alpha = 0.6f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Green, shape = androidx.compose.foundation.shape.CircleShape)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(
                text = "AR ACTIVO • ${orientationState.azimuth.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Green,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Close Button (Racing style)
        androidx.compose.material3.FilledTonalButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color(0xFFE8253A).copy(alpha = 0.85f),
                contentColor = Color.White
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) {
            Text("✕ CERRAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}
