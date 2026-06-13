package com.georacing.georacing.ui.evacuation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.ui.theme.GeoRacingTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

// MapLibre Imports
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.MapLibre

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

class EvacuationActivity : ComponentActivity() {
    
    private val exitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.georacing.georacing.EXIT_EVACUATION") {
                val mainIntent = Intent(context, com.georacing.georacing.MainActivity::class.java).apply {
                   flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }
    
    private val bleMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.georacing.georacing.BLE_EVACUATION_MESSAGE") {
                val message = intent.getStringExtra("message") ?: return
                val zone = intent.getStringExtra("zone") ?: ""
                val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
                _bleMessages.value = _bleMessages.value + BleEvacuationMessage(message, zone, timestamp)
            }
        }
    }
    
    private val _bleMessages = androidx.compose.runtime.mutableStateOf<List<BleEvacuationMessage>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val filter = IntentFilter("com.georacing.georacing.EXIT_EVACUATION")
        val bleFilter = IntentFilter("com.georacing.georacing.BLE_EVACUATION_MESSAGE")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(exitReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(bleMessageReceiver, bleFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(exitReceiver, filter)
            registerReceiver(bleMessageReceiver, bleFilter)
        }

        setContent {
            GeoRacingTheme {
                EvacuationScreen(bleMessages = _bleMessages.value)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(exitReceiver)
        try { unregisterReceiver(bleMessageReceiver) } catch (_: Exception) {}
    }
}

data class BleEvacuationMessage(
    val message: String,
    val zone: String,
    val timestamp: Long
)

@Composable
fun EvacuationScreen(bleMessages: List<BleEvacuationMessage> = emptyList()) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5D1010)) // Dark Red Background
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Home,
                 contentDescription = "Home",
                 tint = Color.White.copy(alpha = 0.7f),
                 modifier = Modifier.size(32.dp)
             )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning Icon
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.Warning, 
            contentDescription = "Warning",
            tint = Color(0xFFE0E0E0), // Off-white
            modifier = Modifier.size(80.dp) // Slightly smaller to fit content
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Titles
        Text(
            text = "EMERGENCIA",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "EVACUACIÓN ACTIVADA",
            color = Color.White.copy(alpha = alpha),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions Card (Restored)
        androidx.compose.material3.Card(
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFF3E0A0A)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INSTRUCCIONES\nINMEDIATAS",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                InstructionItem(number = "1", text = "Mantenga la calma y no corra")
                Spacer(modifier = Modifier.height(16.dp))
                InstructionItem(number = "2", text = "Siga las indicaciones del personal")
                Spacer(modifier = Modifier.height(16.dp))
                InstructionItem(number = "3", text = "Diríjase a la salida más cercana")
                Spacer(modifier = Modifier.height(16.dp))
                InstructionItem(number = "4", text = "No regrese por objetos personales")
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Arrow & Exit Text
        androidx.compose.material3.Icon(
            imageVector = Icons.Filled.ArrowUpward,
            contentDescription = "Arrow Up",
            tint = Color.White,
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = "SALIDAS DE EMERGENCIA",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )

        // Map Container
        Box(
             modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(Color.Black, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(2.dp)
        ) {
             EvacuationMap(
                 modifier = Modifier
                     .fillMaxSize()
                     .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
             )
        }
        
        // ── Mensajes BLE en tiempo real ──
        if (bleMessages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "📡 MENSAJES DEL CIRCUITO",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            bleMessages.takeLast(5).reversed().forEach { msg ->
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color(0xFF3E0A0A)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 20.sp)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = msg.message,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (msg.zone.isNotEmpty()) {
                                Text(
                                    text = "Zona: ${msg.zone}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Return Button
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.material3.Button(
            onClick = { 
                 // Return to Main/Home
                 val intent = android.content.Intent(context, com.georacing.georacing.MainActivity::class.java).apply {
                     flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                 }
                 context.startActivity(intent)
            },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "VOLVER", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Circle Number
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Text(
                text = number,
                color = Color(0xFF5D1010), // Dark Red Text
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start
        )
    }
}



@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun EvacuationScreenPreview() {
    GeoRacingTheme {
        EvacuationScreen()
    }
}

@Composable
fun EvacuationMap(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (androidx.compose.ui.platform.LocalInspectionMode.current) {
         Box(
            modifier = modifier
                .background(Color.Gray)
                .padding(16.dp),
            contentAlignment = Alignment.Center
         ) {
             Text("Map Preview", color = Color.White)
         }
         return
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Initialize MapLibre instance
    remember { MapLibre.getInstance(context) }
    
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }
    
    // Manage Lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { _: Context -> mapView },
        modifier = modifier,
        update = { mv: MapView ->
            mv.getMapAsync { map ->
                // Dark Style for Emergency
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.uiSettings.isTiltGesturesEnabled = false
                
                // Set Style
                map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                    // 1. Add Emergency Exits
                    // ... (Simplification: Just showing map for now)
                }

                // Initial Position: Circuit de Barcelona-Catalunya
                val circuitPos = LatLng(41.569308, 2.257692) // Grandstand
                
                map.cameraPosition = CameraPosition.Builder()
                    .target(circuitPos)
                    .zoom(16.0)
                    .build()
                
                // Add Marker for Safe Zone (Green)
                map.addMarker(MarkerOptions()
                    .position(LatLng(41.5710, 2.2600))
                    .title("PUNTO SEGURO")
                    .snippet("Diríjase aquí"))
            }
        }
    )
}
