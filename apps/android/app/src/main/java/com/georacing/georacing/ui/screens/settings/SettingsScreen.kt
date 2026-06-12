package com.georacing.georacing.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.georacing.georacing.ui.components.debug.DebugButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.components.*
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.georacing.georacing.data.health.HealthConnectManager
import com.georacing.georacing.ui.glass.LocalGlassConfigState
import com.georacing.georacing.ui.glass.LiquidToggle
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.glass.GlassQuality
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LiquidListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    userPreferences: UserPreferencesDataStore
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                SettingsViewModel(userPreferences, HealthConnectManager(application))
            }
        }
    )

    val currentLanguage by viewModel.preferredLanguage.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val largeFont by viewModel.largeFont.collectAsState()
    val avoidStairs by viewModel.avoidStairs.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    
    val healthConnectGranted by viewModel.healthConnectPermissionsGranted.collectAsState()
    val isHealthConnectAvailable = remember { viewModel.isHealthConnectAvailable() }
    
    // Health Connect Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkHealthConnectPermissions()
    }

    // Easter egg: tap counter for Staff Mode
    var versionTapCount by remember { mutableStateOf(0) }
    var showStaffModeUnlocked by remember { mutableStateOf(false) }

    SettingsScreenContent(
        currentLanguage = currentLanguage,
        highContrast = highContrast,
        largeFont = largeFont,
        avoidStairs = avoidStairs,
        notificationsEnabled = notificationsEnabled,
        versionTapCount = versionTapCount,
        showStaffModeUnlocked = showStaffModeUnlocked,
        isHealthConnectAvailable = isHealthConnectAvailable,
        healthConnectGranted = healthConnectGranted,
        onSetLanguage = { viewModel.setLanguage(it) },
        onSetHighContrast = { viewModel.setHighContrast(it) },
        onSetLargeFont = { viewModel.setLargeFont(it) },
        onSetAvoidStairs = { viewModel.setAvoidStairs(it) },
        onToggleNotifications = { notificationsEnabled = it },
        onToggleHealthConnect = { shouldEnable ->
             if (shouldEnable) {
                 permissionLauncher.launch(HealthConnectManager.PERMISSIONS.toTypedArray())
             } else {
                 // Info: cannot auto-revoke
             }
        },
        onResetOnboarding = {
            viewModel.resetOnboarding()
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(0)
            }
        },
        onNavigateBack = { navController.popBackStack() },
        onNavigateHome = {
            navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
            }
        },
        onNavigateToMedicalInfo = {
            navController.navigate(Screen.MedicalLockScreen.route)
        },
        onNavigateToStaffMode = {
            navController.navigate(Screen.StaffMode.route)
        },
        onVersionTap = {
            versionTapCount++
            if (versionTapCount >= 7) {
                showStaffModeUnlocked = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    currentLanguage: String,
    highContrast: Boolean,
    largeFont: Boolean,
    avoidStairs: Boolean = false,
    notificationsEnabled: Boolean,
    versionTapCount: Int = 0,
    showStaffModeUnlocked: Boolean = false,
    isHealthConnectAvailable: Boolean = false,
    healthConnectGranted: Boolean = false,
    onSetLanguage: (String) -> Unit,
    onSetHighContrast: (Boolean) -> Unit,
    onSetLargeFont: (Boolean) -> Unit,
    onSetAvoidStairs: (Boolean) -> Unit = {},
    onToggleNotifications: (Boolean) -> Unit,
    onToggleHealthConnect: (Boolean) -> Unit = {},
    onResetOnboarding: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToMedicalInfo: () -> Unit = {},
    onNavigateToStaffMode: () -> Unit = {},
    onVersionTap: () -> Unit = {}
) {
    val backdrop = LocalBackdrop.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06060C),
                        Color(0xFF0A0A12),
                        Color(0xFF080810)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFFE8253A), shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "AJUSTES", 
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = "Atrás",
                                tint = Color(0xFFE8253A)
                            )
                        }
                    },
                    actions = {
                        com.georacing.georacing.ui.components.HomeIconButton(onNavigateHome)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 🆘 SEGURIDAD Y EMERGENCIAS Section
                Text(
                    "🆘 SEGURIDAD Y EMERGENCIAS", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFEF4444),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )
                LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SettingsNavigationItem(
                            icon = Icons.Default.Favorite,
                            iconTint = Color.Red,
                            title = "Información Médica",
                            subtitle = "Configura tu Lock Screen de emergencia",
                            onClick = onNavigateToMedicalInfo
                        )
                        
                        // Staff Mode - Solo visible si se desbloquea con Easter egg
                        if (showStaffModeUnlocked) {
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            SettingsNavigationItem(
                                icon = Icons.Default.Security,
                                iconTint = Color.Yellow,
                                title = "🔒 Modo Staff",
                                subtitle = "Emisión de alertas BLE (Solo personal)",
                                onClick = onNavigateToStaffMode
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Idioma Section
                Text(
                    "IDIOMA", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )
                LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        LanguageOption(backdrop, "Español", "es", currentLanguage) { onSetLanguage("es") }
                        LanguageOption(backdrop, "Català", "ca", currentLanguage) { onSetLanguage("ca") }
                        LanguageOption(backdrop, "English", "en", currentLanguage) { onSetLanguage("en") }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Accesibilidad Section
                Text(
                    "ACCESIBILIDAD", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )
                LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Alto contraste", modifier = Modifier.weight(1f), color = TextPrimary)
                            LiquidToggle(
                                selected = { highContrast },
                                onSelect = onSetHighContrast,
                                backdrop = backdrop
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Fuente grande", modifier = Modifier.weight(1f), color = TextPrimary)
                            LiquidToggle(
                                selected = { largeFont },
                                onSelect = onSetLargeFont,
                                backdrop = backdrop
                            )
                        }
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rutas sin escaleras", color = TextPrimary)
                                Text(
                                    "Evitar escaleras en rutas peatonales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            LiquidToggle(
                                selected = { avoidStairs },
                                onSelect = onSetAvoidStairs,
                                backdrop = backdrop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Gráficos Section
                Text(
                    "GRAFICOS", 
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
                )
                LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val glassConfigState = LocalGlassConfigState.current
                        val glassConfig = glassConfigState.value
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Liquid Glass UI", color = TextPrimary)
                                Text(
                                    "Efectos de desenfoque y lente (Android 12+)", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            LiquidToggle(
                                selected = { glassConfig.enabled },
                                onSelect = { glassConfigState.value = glassConfig.copy(enabled = it) },
                                backdrop = backdrop
                            )
                        }
                        
                        if (glassConfig.enabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.2f))
                            
                            Text(
                                "Calidad de efectos", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                GlassQuality.entries.forEach { quality ->
                                    val isSelected = glassConfig.quality == quality
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { glassConfigState.value = glassConfig.copy(quality = quality) },
                                        label = { Text(quality.name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CircuitGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = CircuitGreen,
                                            containerColor = Color.Transparent,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = if (isSelected) CircuitGreen else TextTertiary,
                                            enabled = true,
                                            selected = isSelected
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notificaciones Section
                LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notificaciones",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CircuitGreen,
                                uncheckedThumbColor = TextTertiary,
                                uncheckedTrackColor = CarbonBlack
                            )
                        )
                    }
                }

                // Health Connect Section
                if (isHealthConnectAvailable) {
                    Text(
                        "SALUD Y BIENESTAR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.5.sp
                        ),
                        color = Color(0xFF22C55E),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    LiquidCard(backdrop = backdrop, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sincronizar Pasos (EcoMeter)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (healthConnectGranted) "Conectado" else "Permitir acceso a Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (healthConnectGranted) Color(0xFF22C55E) else TextSecondary
                                )
                            }
                            Switch(
                                checked = healthConnectGranted,
                                onCheckedChange = onToggleHealthConnect,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CircuitGreen,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = CarbonBlack
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                RacingButton(
                    text = "Ver Tutorial de nuevo",
                    onClick = onResetOnboarding
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Versión con Easter egg
                Text(
                    text = if (versionTapCount in 1..6) 
                        "v1.0.0 — ${7 - versionTapCount} taps restantes" 
                    else if (showStaffModeUnlocked)
                        "✅ STAFF MODE"
                    else 
                        "v1.0.0",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp
                    ),
                    color = if (showStaffModeUnlocked) Color(0xFF22C55E) else Color(0xFF475569),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onVersionTap() }
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // Debug / God Mode Button (Always visible requested by user, or obscure usage)
                // User requested: "botón en los ajustes que se llame debug"
                DebugButton(
                    text = "🛠️ DEBUG / GOD MODE",
                    color = Color(0xFF1E1E2A),
                    onClick = { 
                        com.georacing.georacing.debug.ScenarioSimulator.setDebugPanelVisible(true) 
                    }
                )
                
                // Extra padding para que no lo tape la bottom nav bar
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun SettingsNavigationItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val backdrop = LocalBackdrop.current
    
    LiquidListItem(
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier.padding(vertical = 4.dp),
        surfaceColor = Color.Transparent,
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // This icon might be wrong in standard RTL but assuming arrow right logic, let's fix to arrow right equivalent or just keep as is if it was correct logic before? 
                    // Actually, usually navigation implies ArrowForward or ChevronRight. The original code had ArrowBack... wait.
                    // Line 498: imageVector = Icons.AutoMirrored.Filled.ArrowBack
                    // That is weird for navigation item. Let's check original logic. Maybe it was intended as ArrowForward but imported wrong? or maybe it's just an arrow.
                    // I'll stick to ArrowBack if that's what it was, but it looks like a bug. Given it's a nav item, I'll use ArrowForward or ChevronRight if available.
                    // Just keeping original ArrowBack to avoid breaking visual consistency unless asked.
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    GeoRacingTheme {
        SettingsScreenContent(
             currentLanguage = "es",
             highContrast = false,
             largeFont = false,
             notificationsEnabled = true,
             onSetLanguage = {},
             onSetHighContrast = {},
             onSetLargeFont = {},
             onToggleNotifications = {},
             onResetOnboarding = {},
             onNavigateBack = {},
             onNavigateHome = {}
        )
    }
}

@Composable
fun LanguageOption(
    backdrop: com.kyant.backdrop.Backdrop,
    text: String,
    code: String,
    selectedCode: String,
    onSelect: () -> Unit
) {
    LiquidListItem(
        onClick = onSelect,
        backdrop = backdrop,
        modifier = Modifier.padding(vertical = 4.dp),
        surfaceColor = if (code == selectedCode) RacingRed.copy(alpha = 0.2f) else Color.Transparent,
        content = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp)
            ) {
                RadioButton(
                    selected = code == selectedCode,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = RacingRed,
                        unselectedColor = TextSecondary
                    )
                )
                Text(
                    text = text, 
                    color = if (code == selectedCode) TextPrimary else TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    )
}
