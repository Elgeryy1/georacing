package com.georacing.georacing.ui.screens.home

import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.georacing.georacing.data.event.isRouteEnabled
import com.georacing.georacing.data.event.labelForMapLayer
import com.georacing.georacing.data.event.labelForRoute
import com.georacing.georacing.data.event.resolvedHomeWidgets
import com.georacing.georacing.domain.repository.CircuitStateRepository
import com.georacing.georacing.domain.model.WidgetType
import com.georacing.georacing.ui.components.*
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.georacing.georacing.ui.glass.LiquidPill
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import android.widget.Toast
import com.georacing.georacing.data.event.EventThemeVariant
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    circuitStateRepository: CircuitStateRepository,
    beaconScanner: com.georacing.georacing.data.ble.BeaconScanner? = null,
    isOnline: Boolean = true,
    bleBeaconsCount: Int = 0,
    userPreferences: com.georacing.georacing.data.local.UserPreferencesDataStore,
    appContainer: com.georacing.georacing.di.AppContainer? = null
) {
    val viewModel: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(circuitStateRepository, beaconScanner)
            }
        }
    )

    val circuitState by viewModel.circuitState.collectAsState()
    val appMode by viewModel.appMode.collectAsState()
    val newsItems by viewModel.newsItems.collectAsState()
    val isLoadingNews by viewModel.isLoadingNews.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current

    // Dashboard Layout
    val storedDashboardLayout by userPreferences.dashboardLayout.collectAsState(
        initial = com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
    )
    val dashboardLayoutOverride by userPreferences.dashboardLayoutOverride.collectAsState(initial = null)
    val dashboardLayout = when {
        activeEvent.navigation.allowUserLayoutOverride -> dashboardLayoutOverride
            ?: activeEvent.resolvedHomeWidgets(storedDashboardLayout)
        else -> activeEvent.resolvedHomeWidgets(storedDashboardLayout)
    }

    // Edit mode state
    var isEditMode by remember { mutableStateOf(false) }
    var editableLayout by remember { mutableStateOf<List<WidgetType>>(emptyList()) }
    var showAddSheet by remember { mutableStateOf(false) }

    // Sync editable layout with saved
    LaunchedEffect(dashboardLayout) {
        if (!isEditMode) {
            editableLayout = dashboardLayout
        }
    }

    val currentLayout = if (isEditMode) editableLayout else dashboardLayout

    // Wobble animation for edit mode
    val infiniteTransition = rememberInfiniteTransition(label = "wobble")
    val wobbleAngle by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(visuals.appBackgroundStops))
        ) {
            drawCircle(
                color = visuals.ambientPrimary,
                radius = size.width * 0.48f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.82f, size.height * 0.18f)
            )
            drawCircle(
                color = visuals.ambientSecondary,
                radius = size.width * 0.4f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.14f, size.height * 0.75f)
            )
        }

        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(if (!isOnline) 60.dp else 20.dp))
            }

            // Dynamic Widgets
            items(currentLayout.size) { index ->
                val widgetType = currentLayout[index]

                if (isEditMode) {
                    // Edit mode: widget with controls
                    EditableWidgetWrapper(
                        widget = widgetType,
                        index = index,
                        total = currentLayout.size,
                        wobbleAngle = wobbleAngle,
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = editableLayout.toMutableList()
                                java.util.Collections.swap(mutable, index, index - 1)
                                editableLayout = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < editableLayout.size - 1) {
                                val mutable = editableLayout.toMutableList()
                                java.util.Collections.swap(mutable, index, index + 1)
                                editableLayout = mutable
                            }
                        },
                        onRemove = {
                            val mutable = editableLayout.toMutableList()
                            mutable.removeAt(index)
                            editableLayout = mutable
                        }
                    ) {
                        RenderWidget(
                            type = widgetType,
                            navController = navController,
                            circuitState = circuitState,
                            temperature = circuitState?.temperature ?: "22Â°",
                            onNavigateToEdit = {},
                            appContainer = appContainer,
                            newsItems = newsItems,
                            isOnline = isOnline,
                            isLoadingNews = isLoadingNews
                        )
                    }
                } else {
                    // Normal mode: long press to enter edit
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                editableLayout = dashboardLayout
                                isEditMode = true
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    ) {
                        RenderWidget(
                            type = widgetType,
                            navController = navController,
                            circuitState = circuitState,
                            temperature = circuitState?.temperature ?: "22Â°",
                            onNavigateToEdit = {},
                            appContainer = appContainer,
                            newsItems = newsItems,
                            isOnline = isOnline,
                            isLoadingNews = isLoadingNews
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Edit mode toolbar at top
        AnimatedVisibility(
            visible = isEditMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = visuals.panelSurfaceStrong,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel
                    TextButton(
                        onClick = {
                            isEditMode = false
                            editableLayout = dashboardLayout
                        }
                    ) {
                        Text("Cancelar", color = visuals.panelMuted, fontWeight = FontWeight.Medium)
                    }

                    Text(
                        "Editando Inicio",
                        color = visuals.heroHeadlineColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )

                    // Save
                    TextButton(
                        onClick = {
                            scope.launch {
                                userPreferences.setDashboardLayout(editableLayout)
                                isEditMode = false
                                Toast.makeText(context, "âœ“ Guardado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Guardar", color = visuals.navSelected, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Floating Add button in edit mode
        AnimatedVisibility(
            visible = isEditMode,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 100.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = visuals.navSelected,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "AÃ±adir widget")
            }
        }

        // Offline Indicator
        OfflineIndicator(
            isOnline = isOnline,
            bleBeaconsDetected = bleBeaconsCount
        )
    }

    // Add widget bottom sheet
    if (showAddSheet) {
        val allWidgets = WidgetType.values().filter { it != WidgetType.STAFF_ACTIONS }
        val hiddenWidgets = allWidgets.filter { !editableLayout.contains(it) }

        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            containerColor = visuals.panelSurfaceStrong,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(visuals.panelMuted)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    "AÃ±adir Widgets",
                    color = visuals.heroHeadlineColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Toca un widget para aÃ±adirlo a tu inicio",
                    color = visuals.panelMuted,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (hiddenWidgets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = visuals.navSelected,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ya tienes todos los widgets activos",
                                color = visuals.panelMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    hiddenWidgets.forEach { widget ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = visuals.panelSurface),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    editableLayout = editableLayout + widget
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showAddSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(widget.accentColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = widget.icon,
                                        contentDescription = null,
                                        tint = widget.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        widget.displayName,
                                        color = visuals.heroHeadlineColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = "AÃ±adir",
                                    tint = visuals.panelMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

/**
 * Wrapper that adds edit controls around a widget (move up/down, remove badge).
 * Shows a wobble animation and dashed border.
 */
@Composable
private fun EditableWidgetWrapper(
    widget: WidgetType,
    index: Int,
    total: Int,
    wobbleAngle: Float,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = wobbleAngle }
            .padding(vertical = 2.dp)
    ) {
        // Widget content with edit border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = widget.accentColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            content()
        }

        // Remove badge (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444))
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Eliminar",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // Reorder controls (left side, centered)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-14).dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E2E).copy(alpha = 0.95f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Subir",
                    tint = if (index > 0) Color.White else Color(0xFF2A2A3A),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < total - 1,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Bajar",
                    tint = if (index < total - 1) Color.White else Color(0xFF2A2A3A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun GreetingsHeader(
    circuitState: com.georacing.georacing.domain.model.CircuitState?,
    isOnline: Boolean
) {
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current
    val currentDate = java.time.LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val dateString = currentDate.format(formatter)
        .replaceFirstChar { it.uppercase() }
    val sessionInfo = circuitState?.sessionInfo
    val eventLabel = when {
        !isOnline -> "MODO OFFLINE"
        circuitState.hasLiveSession() -> "SESION EN DIRECTO"
        circuitState.isEventDayActive() -> "JORNADA ACTIVA"
        else -> "BIENVENIDO"
    }
    val eventSummary = when {
        circuitState.hasLiveSession() -> buildString {
            append("Vuelta ${sessionInfo?.currentLap ?: 0}/${sessionInfo?.totalLaps ?: 0}")
            if (!sessionInfo?.sessionTime.isNullOrBlank()) {
                append(" Â· ${sessionInfo?.sessionTime}")
            }
        }
        !circuitState?.message.isNullOrBlank() -> circuitState?.message ?: ""
        else -> activeEvent.venueName
    }
    val weatherIcon = when {
        circuitState?.forecast?.contains("rain", ignoreCase = true) == true -> Icons.Default.Cloud
        circuitState?.forecast?.contains("lluv", ignoreCase = true) == true -> Icons.Default.Cloud
        circuitState?.forecast?.contains("wind", ignoreCase = true) == true -> Icons.Default.Air
        else -> Icons.Default.WbSunny
    }
    val primaryHeadline = when (visuals.variant) {
        EventThemeVariant.NIGHT -> activeEvent.shortName
        EventThemeVariant.ELECTRIC -> activeEvent.displayName.uppercase()
        EventThemeVariant.SUNSET -> activeEvent.venueName
        EventThemeVariant.CLASSIC -> dateString
        EventThemeVariant.RACING_RED -> dateString
    }
    val secondaryLine = when (visuals.variant) {
        EventThemeVariant.NIGHT -> "$dateString Â· ${activeEvent.locationLabel}"
        EventThemeVariant.ELECTRIC -> eventSummary
        EventThemeVariant.SUNSET -> "${activeEvent.locationLabel} Â· ${activeEvent.ctaLabel}"
        EventThemeVariant.CLASSIC -> eventSummary
        EventThemeVariant.RACING_RED -> eventSummary
    }
    val accentColor = if (circuitState.hasLiveSession()) visuals.navSelected else visuals.heroChipText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = eventLabel,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = if (visuals.variant == EventThemeVariant.NIGHT) 0.3.sp else 2.sp,
                color = visuals.panelMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = primaryHeadline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = visuals.heroHeadlineColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = secondaryLine,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1
            )
        }
        
        // Weather Pill
        val backdrop = LocalBackdrop.current
        LiquidPill(
            backdrop = backdrop,
            modifier = Modifier,
            surfaceColor = visuals.panelSurface
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = weatherIcon,
                    contentDescription = null,
                    tint = if (weatherIcon == Icons.Default.WbSunny) visuals.heroChipText else visuals.navSelected,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = circuitState?.temperature ?: "--",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = visuals.heroHeadlineColor
                )
            }
        }
    }
}

@Composable
fun DashboardGrid(
    navController: NavController,
    circuitState: com.georacing.georacing.domain.model.CircuitState?
) {
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current
    val isLiveEvent = circuitState.hasLiveSession()
    val palette = visuals.featurePalette

    val features = listOf(
        FeatureItem("Mapa", Icons.Filled.AltRoute, palette[0 % palette.size], Screen.Map.route),
        FeatureItem(
            if (isLiveEvent) "Live" else "Tienda",
            if (isLiveEvent) Icons.Filled.Speed else Icons.Filled.ShoppingCart,
            palette[1 % palette.size],
            if (isLiveEvent) Screen.FanImmersive.route else Screen.Orders.route
        ),
        FeatureItem(
            activeEvent.labelForMapLayer("FOOD", "Comida"),
            Icons.Filled.Restaurant,
            palette[2 % palette.size],
            Screen.Orders.route,
            preserveTitle = true
        ),
        FeatureItem(
            activeEvent.labelForMapLayer("RESTROOM", "Servicios"),
            Icons.Filled.Wc,
            palette[3 % palette.size],
            Screen.PoiList.route,
            preserveTitle = true
        ),
        FeatureItem("Parking", Icons.Filled.LocalParking, palette[0 % palette.size], Screen.Parking.route),
        FeatureItem("Agenda", Icons.Filled.List, palette[1 % palette.size], Screen.Roadmap.route),
        FeatureItem("Grupo", Icons.Filled.Groups, palette[2 % palette.size], Screen.Group.route),
        FeatureItem("Avisar", Icons.Filled.Warning, palette[3 % palette.size], Screen.IncidentReport.route)
    )

    val visibleFeatures = features.mapNotNull { feature ->
        val enabledByDefault = when (feature.route) {
            Screen.Parking.route -> activeEvent.featureFlags.enableParking
            Screen.Roadmap.route -> activeEvent.featureFlags.enableRoadmap
            Screen.Group.route -> activeEvent.featureFlags.enableGroup
            Screen.Orders.route -> activeEvent.featureFlags.enableOrders
            else -> true
        }
        if (!activeEvent.isRouteEnabled(feature.route, default = enabledByDefault)) {
            null
        } else {
            feature.copy(
                title = if (feature.preserveTitle) {
                    feature.title
                } else {
                    activeEvent.labelForRoute(feature.route, feature.title).replace("Ã±", "n")
                }
            )
        }
    }


    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        userScrollEnabled = false,
        modifier = Modifier.height(260.dp) 
    ) {
        items(visibleFeatures.size) { index ->
            val feature = visibleFeatures[index]
            FeatureCard(
                title = feature.title,
                description = "", 
                icon = feature.icon,
                accentColor = feature.accentColor,
                onClick = { navController.navigate(feature.route) },
                index = index
            )
        }
    }
}

@Composable
fun LatestNewsCard(
    circuitState: com.georacing.georacing.domain.model.CircuitState?,
    isLoading: Boolean = false
) {
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current
    val backdrop = LocalBackdrop.current
    val sessionInfo = circuitState?.sessionInfo
    val liveLeader = sessionInfo?.topDrivers?.firstOrNull()
    val headline = when {
        isLoading -> "Cargando resumen del GP"
        circuitState.hasLiveSession() -> "Vuelta ${sessionInfo?.currentLap ?: 0}/${sessionInfo?.totalLaps ?: 0} en curso"
        circuitState?.mode == com.georacing.georacing.domain.model.CircuitMode.RED_FLAG -> "Bandera roja en el circuito"
        circuitState?.mode == com.georacing.georacing.domain.model.CircuitMode.SAFETY_CAR -> "Safety Car desplegado"
        circuitState?.mode == com.georacing.georacing.domain.model.CircuitMode.YELLOW_FLAG ||
            circuitState?.mode == com.georacing.georacing.domain.model.CircuitMode.VSC -> "Precaucion en pista"
        else -> activeEvent.heroTitle
    }
    val subtitle = when {
        isLoading -> activeEvent.infoBanner
        circuitState.hasLiveSession() && liveLeader != null ->
            "Lidera ${liveLeader.name} Â· ${sessionInfo?.sessionTime ?: "--:--:--"}"
        !circuitState?.message.isNullOrBlank() -> circuitState?.message ?: ""
        else -> activeEvent.heroSubtitle
    }
    val chipLabel = when {
        isLoading -> "ACTUALIZANDO"
        circuitState.hasLiveSession() -> "EVENTO ACTIVO"
        circuitState.isEventDayActive() -> "ESTADO DEL DIA"
        else -> "HOY EN EL CIRCUITO"
    }
    val descriptor = when (visuals.variant) {
        EventThemeVariant.NIGHT -> "${activeEvent.shortName} Â· ${activeEvent.locationLabel}"
        EventThemeVariant.ELECTRIC -> activeEvent.infoBanner
        EventThemeVariant.SUNSET -> "${activeEvent.ctaLabel} Â· ${activeEvent.locationLabel}"
        EventThemeVariant.CLASSIC -> subtitle
        EventThemeVariant.RACING_RED -> subtitle
    }
    LiquidCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        cornerRadius = 24.dp,
        surfaceColor = visuals.panelSurface,
        tint = visuals.navSelected.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Tag with racing red accent
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(visuals.navSelected)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = chipLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = if (visuals.variant == EventThemeVariant.NIGHT) 0.2.sp else 1.5.sp
                    ),
                    color = visuals.navSelected
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = headline,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = visuals.heroHeadlineColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = descriptor,
                style = MaterialTheme.typography.bodyMedium,
                color = visuals.heroBodyColor
            )
        }
    }
}

internal fun com.georacing.georacing.domain.model.CircuitState?.hasLiveSession(): Boolean {
    val session = this?.sessionInfo ?: return false
    return session.currentLap > 0 || session.topDrivers.isNotEmpty() || session.sessionTime != "00:00:00"
}

internal fun com.georacing.georacing.domain.model.CircuitState?.isEventDayActive(): Boolean {
    if (this == null) return false
    return hasLiveSession() || mode != com.georacing.georacing.domain.model.CircuitMode.UNKNOWN
}

// DashboardBottomBar Removed from here


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    GeoRacingTheme {
        HomeScreen(
            navController = rememberNavController(),
            circuitStateRepository = object : CircuitStateRepository {
                override fun getCircuitState() = kotlinx.coroutines.flow.flowOf(
                    com.georacing.georacing.domain.model.CircuitState(
                         com.georacing.georacing.domain.model.CircuitMode.NORMAL, 
                         "Preview", 
                         "24Â°C",
                         updatedAt = "Now"
                    )
                )
                override fun setCircuitState(mode: com.georacing.georacing.domain.model.CircuitMode, message: String?) {}
                override val appMode = kotlinx.coroutines.flow.flowOf(com.georacing.georacing.domain.model.AppMode.ONLINE)
                override val debugInfo = kotlinx.coroutines.flow.flowOf("Debug Info")
            },
            userPreferences = com.georacing.georacing.data.local.UserPreferencesDataStore(androidx.compose.ui.platform.LocalContext.current) // Mock for preview
        )
    }
}

// Data class
data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val route: String,
    val description: String = "",
    val preserveTitle: Boolean = false
)

