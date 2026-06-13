package com.georacing.georacing.ui.screens.eco

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georacing.georacing.data.health.HealthConnectManager
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoMeterScreen(navController: NavController, appContainer: com.georacing.georacing.di.AppContainer? = null) {
    val context = LocalContext.current
    
    // Resolve HealthConnectManager via MainActivity's AppContainer
    val healthManager = (context as? com.georacing.georacing.MainActivity)?.appContainer?.healthConnectManager 
        ?: com.georacing.georacing.data.health.FakeHealthConnectManager(context)
    val userPrefs = com.georacing.georacing.data.local.UserPreferencesDataStore(context)
        
    val viewModel: EcoViewModel = viewModel(factory = EcoViewModel.Factory(healthManager, userPrefs))


    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EcoMeter", color = Color(0xFFE2E8F0)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFE2E8F0))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Title Section
            Text(
                text = "TU HUELLA VERDE",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF22C55E),
                letterSpacing = 1.5.sp
            )

            // 2. Main Circular Progress (Daily Goal)
            Box(contentAlignment = Alignment.Center) {
                CircularProgress(
                    percentage = (state.steps / 10000f).coerceIn(0f, 1f),
                    color = Color(0xFF22C55E),
                    size = 200.dp,
                    strokeWidth = 12.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.steps}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "PASOS HOY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // 3. Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    title = "DISTANCIA",
                    value = String.format("%.2f km", state.distanceMeters / 1000),
                    modifier = Modifier.weight(1f)
                )
                InfoCard(
                    title = "CO2 EVITADO",
                    value = String.format("%.1f g", state.co2SavedGrams),
                    modifier = Modifier.weight(1f),
                    valueColor = Color(0xFF22C55E)
                )
            }

            // 4. Permission / Sync / Settings Button
            if (!state.hasPermissions) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                         viewModel.checkAndRequestPermissions() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if(state.isHealthConnectAvailable) "CONECTAR SALUD" else "INSTALAR HEALTH CONNECT", color = Color(0xFF080810), fontWeight = FontWeight.Bold)
                }

                if (state.isHealthConnectAvailable && !state.hasPermissions) {
                     Spacer(modifier = Modifier.height(8.dp))
                     Button(
                        onClick = {
                             val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                 data = android.net.Uri.fromParts("package", context.packageName, null)
                             }
                             context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14141C)),
                        modifier = Modifier.fillMaxWidth()
                     ) {
                         Text("ABRIR CONFIGURACIÓN DE APP", color = Color(0xFFE2E8F0))
                     }
                }
                Text(
                    text = "Si el sistema no pregunta, ábrelo manualmente en Configuración.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                 Text(
                    text = "Datos sincronizados con Health Connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            // Debug Info
            if (state.isHealthConnectAvailable && !state.hasPermissions) {
                 Text(
                    text = "Estado: Disponible pero sin permisos.\nIntenta abrir 'Permisos' manualmente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFA726),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else if (!state.isHealthConnectAvailable) {
                 Text(
                    text = "Estado: Health Connect NO detectado/disponible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color(0xFFE2E8F0)
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CircularProgress(
    percentage: Float,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp
) {
    val animatedProgress by animateFloatAsState(targetValue = percentage, label = "Progress")

    Canvas(modifier = Modifier.size(size)) {
        // Track
        drawArc(
            color = Color(0xFF1E1E2A),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        // Progress
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360 * animatedProgress,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}
