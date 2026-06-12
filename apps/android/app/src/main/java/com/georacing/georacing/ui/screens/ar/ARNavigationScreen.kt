package com.georacing.georacing.ui.screens.ar

import android.Manifest
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
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

    // Using Mock Location for now OR ParkingRepository's location if available, 
    // ideally we should inject a LocationManager in AppContainer.
    // For this task, I'll check if ParkingRepository has a location, otherwise fallback to Circuit Center Mock.
    val userLocationState = appContainer.parkingRepository.parkingLocation.collectAsStateWithLifecycle(initialValue = null)
    
    // Mock user location near the circuit if no real GPS (Hardcoded near Circuit de Catalunya entrance)
    val userLocation = remember(userLocationState.value) {
        val parkingLoc = userLocationState.value
        if (parkingLoc != null) {
            Location("GPS").apply { 
                latitude = parkingLoc.latitude
                longitude = parkingLoc.longitude 
                altitude = 0.0
            }
        } else {
            // Default Mock: Circuit Entrance
            Location("Mock").apply { 
                latitude = 41.569 // Near Gate 3
                longitude = 2.254 
                altitude = 0.0
            }
        }
    }

    val pois = remember { PoiRepository.getAllPois() }

    Box(modifier = Modifier.fillMaxSize()) {
        
        // A. Camera Layer
        ARCameraView()

        // B. Overlay Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // F1 Style Crosshair
            drawLine(Color.Green, Offset(width/2 - 30, height/2), Offset(width/2 + 30, height/2), 2.dp.toPx())
            drawLine(Color.Green, Offset(width/2, height/2 - 30), Offset(width/2, height/2 + 30), 2.dp.toPx())

            // Draw POIs
            val paintText = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 40f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
            }
            
            val paintBox = Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.argb(150, 0, 0, 0) // Semi-transparent black bg
                style = android.graphics.Paint.Style.FILL
            }

            // Limit to max 5 visible items to reduce clutter
            var visibleCount = 0

            pois.forEach { poi ->
                if (visibleCount >= 10) return@forEach

                val screenPos = ARCalculator.calculatePosition(
                    userLocation = userLocation,
                    poi = poi,
                    deviceAzimuth = orientationState.azimuth,
                    devicePitch = orientationState.pitch
                )

                if (screenPos.isVisible) {
                    visibleCount++
                    val x = screenPos.x * width
                    // Adjust Y to center-ish but float based on pitch (already handled by ARCalculator)
                    // If y is 0.5 (horizon), draw at height/2.
                    // ARCalculator maps 0.5 -> horizon.
                    val y = screenPos.y * height

                    // Draw Marker
                    drawLine(
                        color = if(poi.type == com.georacing.georacing.car.PoiType.GATE) Color.White else Color.Green,
                        start = Offset(x, y),
                        end = Offset(x, y - 50),
                        strokeWidth = 3.dp.toPx()
                    )

                    // Draw Label Box
                    val text = "${poi.name} (${screenPos.distanceMeters.toInt()}m)"
                    drawContext.canvas.nativeCanvas.drawRect(
                        x - 150, y - 110, x + 150, y - 50, paintBox
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        text, x, y - 70, paintText
                    )
                }
            }
        }

        // C. Technical HUD decorations
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                text = "HDG: ${orientationState.azimuth.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Green,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "PITCH: ${orientationState.pitch.toInt()}°",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Green,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            Text(
                text = "GPS: ${userLocation.latitude}, ${userLocation.longitude}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }

        // Close Button
        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha=0.6f))
        ) {
            Text("CERRAR AR")
        }
    }
}
