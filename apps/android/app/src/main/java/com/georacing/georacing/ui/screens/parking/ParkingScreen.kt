package com.georacing.georacing.ui.screens.parking

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.georacing.georacing.data.parking.ParkingLocation
import com.georacing.georacing.data.parking.ParkingRepository
import com.georacing.georacing.ui.components.GlassCard
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.components.RacingButton
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParkingScreen(
    navController: NavController,
    parkingRepository: ParkingRepository? = null
) {
    val backdrop = LocalBackdrop.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { parkingRepository ?: ParkingRepository(context) }
    val parkingLocation by repo.parkingLocation.collectAsState(initial = null)

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Glass Top Bar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentParking)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "MI COCHE",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    }
                },
                actions = {
                    HomeIconButton {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(
                    visible = screenVisible,
                    enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.9f)
                ) {
                    if (parkingLocation != null) {
                        PremiumParkingCard(
                            location = parkingLocation!!,
                            onNavigate = {
                                val gmmIntentUri = Uri.parse(
                                    "google.navigation:q=${parkingLocation!!.latitude},${parkingLocation!!.longitude}&mode=w"
                                )
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    context.startActivity(Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${parkingLocation!!.latitude},${parkingLocation!!.longitude}&travelmode=walking")
                                    ))
                                }
                            },
                            onClear = { scope.launch { repo.clearParking() } }
                        )
                    } else {
                        PremiumNoParkingCard(
                            isSaving = isSaving,
                            error = saveError,
                            onSaveLocation = {
                                scope.launch {
                                    isSaving = true
                                    saveError = null
                                    try {
                                        val location = getCurrentLocation(context)
                                        if (location != null) {
                                            repo.saveParkingLocation(
                                                ParkingLocation(location.first, location.second, System.currentTimeMillis(), null)
                                            )
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
                }

                Spacer(Modifier.height(24.dp))

                // Info card
                AnimatedVisibility(
                    visible = screenVisible,
                    enter = fadeIn(tween(800, 200)) + slideInVertically(tween(800, 200)) { it / 3 }
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = NeonCyan
                    ) {
                        Text(
                            "CÓMO FUNCIONA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(10.dp))
                        val tips = listOf(
                            "Al desconectar Android Auto, se preguntará si guardar la ubicación",
                            "También puedes guardar manualmente desde esta pantalla",
                            "Toca \"Navegar al coche\" para que Maps te guíe de vuelta"
                        )
                        tips.forEach { tip ->
                            Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(tip, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// Premium Parking Location Card
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumParkingCard(
    location: ParkingLocation,
    onNavigate: () -> Unit,
    onClear: () -> Unit
) {
    val timeAgo = remember(location.timestamp) { getRelativeTimeString(location.timestamp) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Pulse animation for car circle
    val infiniteTransition = rememberInfiniteTransition(label = "car_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "pulse"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = StatusGreen
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Car icon with glow
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(StatusGreen.copy(alpha = 0.15f), Color.Transparent)
                            ),
                            radius = size.width
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(StatusGreen.copy(alpha = 0.2f), CarbonBlack)
                        )
                    )
                    .border(1.dp, StatusGreen.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, "Coche", tint = StatusGreen, modifier = Modifier.size(42.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "COCHE APARCADO",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = StatusGreen
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}",
                    fontSize = 11.sp, color = TextTertiary
                )
            }
            Text("Guardado $timeAgo", fontSize = 10.sp, color = TextTertiary.copy(alpha = 0.6f), modifier = Modifier.padding(top = 2.dp))

            Spacer(Modifier.height(20.dp))

            // Navigate button
            RacingButton(
                text = "NAVEGAR AL COCHE",
                onClick = onNavigate,
                icon = Icons.Default.Navigation,
                color = StatusGreen
            )

            Spacer(Modifier.height(10.dp))

            // Clear button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, tint = StatusRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar ubicación", fontSize = 12.sp, color = StatusRed.copy(alpha = 0.6f))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        PremiumDeleteDialog(
            onConfirm = { showDeleteConfirm = false; onClear() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

// ═══════════════════════════════════════════════════════
// Premium No Parking Card
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumNoParkingCard(
    isSaving: Boolean,
    error: String?,
    onSaveLocation: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Empty state icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MetalGrey.copy(alpha = 0.3f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocalParking, null, tint = TextTertiary, modifier = Modifier.size(42.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "SIN UBICACIÓN",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Guarda la ubicación de tu coche para no perderlo en el circuito.",
                fontSize = 13.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error, fontSize = 11.sp, color = StatusRed)
            }

            Spacer(Modifier.height(20.dp))

            RacingButton(
                text = if (isSaving) "GUARDANDO..." else "GUARDAR UBICACIÓN ACTUAL",
                onClick = onSaveLocation,
                icon = Icons.Default.LocationOn,
                enabled = !isSaving
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// Premium Delete Dialog
// ═══════════════════════════════════════════════════════

@Composable
private fun PremiumDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var dialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dialogVisible = true }

    val dialogAlpha by animateFloatAsState(
        targetValue = if (dialogVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "del_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .graphicsLayer { alpha = dialogAlpha }
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF1A1A24), Color(0xFF12121A))),
                        RoundedCornerShape(20.dp)
                    )
                    .border(0.5.dp, StatusRed.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                                startY = 0f, endY = size.height * 0.3f
                            )
                        )
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = {}
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¿Borrar ubicación?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Se eliminará la ubicación guardada de tu coche. No podrás navegar de vuelta.",
                        fontSize = 13.sp, color = TextTertiary, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MetalGrey.copy(alpha = 0.5f))
                                .clickable(onClick = onDismiss)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CANCELAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = TextTertiary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusRed.copy(alpha = 0.2f))
                                .border(0.5.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable(onClick = onConfirm)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("BORRAR", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = StatusRed)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun getCurrentLocation(context: android.content.Context): Pair<Double, Double>? {
    return try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return null

        val location = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).await()

        if (location != null) {
            Pair(location.latitude, location.longitude)
        } else {
            val lastLocation = fusedLocationClient.lastLocation.await()
            lastLocation?.let { Pair(it.latitude, it.longitude) }
        }
    } catch (e: Exception) { null }
}

private fun getRelativeTimeString(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "hace un momento"
        diff < 3600_000 -> "hace ${diff / 60_000} min"
        diff < 86400_000 -> "hace ${diff / 3600_000} hora(s)"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
