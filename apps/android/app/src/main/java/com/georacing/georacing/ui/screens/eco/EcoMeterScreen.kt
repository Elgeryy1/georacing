package com.georacing.georacing.ui.screens.eco

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georacing.georacing.data.health.HealthConnectManager
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.*

/**
 * EcoMeterScreen — Premium Sustainability HUD
 * Pasos, distancia, CO2 evitado con Health Connect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoMeterScreen(navController: NavController, appContainer: com.georacing.georacing.di.AppContainer? = null) {
    val backdrop = LocalBackdrop.current
    val context = LocalContext.current

    // Resolve HealthConnectManager via MainActivity's AppContainer
    val healthManager = (context as? com.georacing.georacing.MainActivity)?.appContainer?.healthConnectManager
        ?: com.georacing.georacing.data.health.FakeHealthConnectManager(context)

    // Instantiate UserPreferencesDataStore instance
    val userPrefs = com.georacing.georacing.data.local.UserPreferencesDataStore(context)

    val viewModel: EcoViewModel = viewModel(factory = EcoViewModel.Factory(healthManager, userPrefs))

    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkAvailabilityAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(StatusGreen))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("ECOMETER", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Tu huella verde en el circuito", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Section Label ──
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -20 }
                ) {
                    Text(
                        "TU HUELLA VERDE",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusGreen,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // ── Hero Circular Progress ──
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(600, 100)) + slideInVertically(tween(600, 100)) { 40 }
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(32.dp))
                            .liquidGlass(RoundedCornerShape(32.dp), GlassLevel.L3, accentGlow = StatusGreen)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow pulse
                        val pulseAnim = rememberInfiniteTransition(label = "eco")
                        val glowAlpha by pulseAnim.animateFloat(
                            initialValue = 0.08f, targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "g"
                        )

                        Box(
                            Modifier
                                .size(220.dp)
                                .drawBehind {
                                    drawCircle(StatusGreen.copy(alpha = glowAlpha), radius = size.minDimension / 1.8f)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            PremiumCircularProgress(
                                percentage = (state.steps / 10000f).coerceIn(0f, 1f),
                                trackColor = AsphaltGrey,
                                progressColors = listOf(StatusGreen, NeonCyan),
                                size = 200.dp,
                                strokeWidth = 14.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Leaf icon
                                Icon(
                                    Icons.Default.Eco,
                                    null,
                                    tint = StatusGreen.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${state.steps}",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    "PASOS HOY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // ── Info Cards Row ──
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { 30 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumInfoCard(
                            icon = Icons.Default.Route,
                            title = "DISTANCIA",
                            value = String.format("%.2f km", state.distanceMeters / 1000),
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumInfoCard(
                            icon = Icons.Default.Park,
                            title = "CO2 EVITADO",
                            value = String.format("%.1f g", state.co2SavedGrams),
                            accentColor = StatusGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Permission / Sync Section ──
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(500, 450)) + slideInVertically(tween(500, 450)) { 30 }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!state.hasPermissions) {
                            // Connect button - premium glass
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(StatusGreen)
                                    .clickable {
                                        viewModel.checkAndRequestPermissions()
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (state.isHealthConnectAvailable) "CONECTAR SALUD" else "INSTALAR HEALTH CONNECT",
                                    color = CarbonBlack,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (state.isHealthConnectAvailable && !state.hasPermissions) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .liquidGlass(RoundedCornerShape(14.dp), GlassLevel.L1)
                                        .clickable {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ABRIR CONFIGURACIÓN DE APP", color = TextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                }
                            }

                            Text(
                                "Si el sistema no pregunta, ábrelo manualmente en Configuración.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            // Connected status pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(StatusGreen.copy(alpha = 0.1f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StatusGreen)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Sincronizado con Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Debug status info
                        if (state.isHealthConnectAvailable && !state.hasPermissions) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StatusAmber.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Estado: Disponible pero sin permisos.\nIntenta abrir 'Permisos' manualmente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAmber,
                                    lineHeight = 18.sp
                                )
                            }
                        } else if (!state.isHealthConnectAvailable) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StatusRed.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Estado: Health Connect NO detectado/disponible.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusRed,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// ── Premium Composables ──
// ══════════════════════════════════════════════

@Composable
private fun PremiumInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = accentColor)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = accentColor,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun PremiumCircularProgress(
    percentage: Float,
    trackColor: Color,
    progressColors: List<Color>,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(1200, easing = EaseOutQuart),
        label = "Progress"
    )

    Canvas(modifier = Modifier.size(size)) {
        val sweep = 360 * animatedProgress
        // Track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        // Gradient progress
        drawArc(
            brush = Brush.sweepGradient(progressColors),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

private val EaseOutQuart: Easing = CubicBezierEasing(0.165f, 0.84f, 0.44f, 1f)
