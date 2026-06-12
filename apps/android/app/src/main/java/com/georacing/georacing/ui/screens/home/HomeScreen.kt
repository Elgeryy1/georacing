package com.georacing.georacing.ui.screens.home

import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.georacing.georacing.domain.repository.CircuitStateRepository
import com.georacing.georacing.ui.components.*
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.georacing.georacing.ui.glass.LiquidPill
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    circuitStateRepository: CircuitStateRepository,
    beaconScanner: com.georacing.georacing.data.ble.BeaconScanner? = null,
    isOnline: Boolean = true,
    bleBeaconsCount: Int = 0,
    userPreferences: com.georacing.georacing.data.local.UserPreferencesDataStore,
    appContainer: com.georacing.georacing.di.AppContainer? = null // Nullable for Preview/Compat
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
    
    // Load Dashboard Layout
    val dashboardLayout by userPreferences.dashboardLayout.collectAsState(
        initial = com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
    )

    // Dashboard Layout with Offline Indicator
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Extra top spacing for offline indicator
            // item { Spacer(modifier = Modifier.height(if (!isOnline) 60.dp else 20.dp)) }
            
            item {
                Spacer(modifier = Modifier.height(if (!isOnline) 60.dp else 20.dp))
                // Smart Ticket (Shows always, expands when at gate)
                com.georacing.georacing.ui.components.ticket.SmartTicketCard()
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dynamic Widgets
            items(dashboardLayout.size) { index ->
                val widgetType = dashboardLayout[index]
                 
                 // Special animation wrapper could go here using animateItemPlacement() if key provided
                 
                 RenderWidget(
                     type = widgetType,
                     navController = navController,
                     circuitState = circuitState,
                     temperature = circuitState?.temperature ?: "22°",
                     onNavigateToEdit = { navController.navigate("edit_dashboard") },
                     appContainer = appContainer,
                     newsItems = newsItems,
                     isOnline = isOnline
                 )
            }

            item { Spacer(modifier = Modifier.height(100.dp)) } // Bottom bar spacer
        }
        
        // Edit Button (Floating Top Right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
        ) {
             androidx.compose.material3.IconButton(
                 onClick = { navController.navigate("edit_dashboard") }
             ) {
                 androidx.compose.material3.Icon(
                     Icons.Default.Edit, 
                     contentDescription = "Edit Dashboard",
                     tint = Color.White.copy(alpha = 0.5f)
                 )
             }
        }
        
        // 📡 Offline Indicator
        OfflineIndicator(
            isOnline = isOnline,
            bleBeaconsDetected = bleBeaconsCount
        )
    }
}

@Composable
fun GreetingsHeader(temperature: String) {
    val currentDate = java.time.LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))
    val dateString = currentDate.format(formatter)
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "BIENVENIDO",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 2.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateString,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
        }
        
        // Weather Pill
        val backdrop = LocalBackdrop.current
        LiquidPill(
            backdrop = backdrop,
            modifier = Modifier,
            surfaceColor = Color(0xFF14141C).copy(alpha = 0.7f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Clima actual",
                    tint = Color(0xFFFF5E00), // NeonOrange
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = temperature,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DashboardGrid(navController: NavController) {
    // 4 Columns is tight on mobile, reference showed 4 but maybe 3 is safer?
    // Reference image shows 4 items in a row (Map, Store, Food, Bath).
    // Let's use GridCells.Fixed(4) and adjust FeatureCard size.
    
    // 4 Columns is tight on mobile, reference showed 4 but maybe 3 is safer?
    // Reference image shows 4 items in a row (Map, Store, Food, Bath).
    // Let's use GridCells.Fixed(4) and adjust FeatureCard size.

    
    val features = listOf(
        FeatureItem("Mapa", Icons.Filled.AltRoute, Color(0xFF00F0FF), Screen.Map.route),
        FeatureItem("Shop", Icons.Filled.Flag, Color(0xFF06D6A0), Screen.Orders.route),
        FeatureItem("Comida", Icons.Filled.Restaurant, Color(0xFFFF5E00), Screen.Orders.route), 
        FeatureItem("Baños", Icons.Filled.Wc, Color(0xFF4361EE), Screen.PoiList.route), 
        
        FeatureItem("Parking", Icons.Filled.LocalParking, Color(0xFFA0AAB2), Screen.Parking.route),
        FeatureItem("Buscar", Icons.Filled.Search, Color(0xFF00F0FF), Screen.Search.route),
        FeatureItem("Logros", Icons.Filled.EmojiEvents, Color(0xFFFFD166), Screen.Achievements.route),
        FeatureItem("Alertas", Icons.Filled.Warning, Color(0xFFFF2A3C), Screen.IncidentReport.route),
        
        FeatureItem("Recoger", Icons.Filled.ShoppingCart, Color(0xFFF72585), Screen.ClickCollect.route),
        FeatureItem("Tráfico", Icons.Filled.Traffic, Color(0xFFFF2A3C), Screen.RouteTraffic.route),
        FeatureItem("Resumen", Icons.Filled.Star, Color(0xFFB5179E), Screen.Wrapped.route),
        FeatureItem("Cromos", Icons.Filled.AccountBox, Color(0xFFFFD166), Screen.Collectibles.route),
        FeatureItem("Chat", Icons.Filled.Forum, Color(0xFF06D6A0), Screen.ProximityChat.route),
        FeatureItem("Fan Zone", Icons.Filled.SportsMotorsports, Color(0xFFFF4D6D), Screen.FanZone.route)
    )


    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false,
        modifier = Modifier.height(400.dp) 
    ) {
        items(features.size) { index ->
            val feature = features[index]
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
fun LatestNewsCard() {
    val backdrop = LocalBackdrop.current
    LiquidCard(
        backdrop = backdrop,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        cornerRadius = 24.dp,
        surfaceColor = AsphaltGrey.copy(alpha = 0.6f),
        tint = RacingRedBright.copy(alpha = 0.15f)
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
                        .background(RacingRedBright)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LIVE EVENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    ),
                    color = RacingRedBright
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = "Welcome to GeoRacing",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Live updates from the circuit • Real-time telemetry",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
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
                         "24°C",
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
    val description: String = ""
)
