package com.georacing.georacing.ui.screens.traffic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.repository.NetworkTrafficRepository
import com.georacing.georacing.domain.model.RouteTraffic
import com.georacing.georacing.domain.model.RouteTrafficStatus
import com.georacing.georacing.domain.model.ZoneOccupancy
import com.georacing.georacing.domain.model.ZoneOccupancyStatus
import kotlinx.coroutines.delay

// Theme Colors
private val DeepBg = Color(0xFF080810)
private val SurfaceDark = Color(0xFF14141C)
private val OffWhite = Color(0xFFF8FAFC)
private val NeonCyan = Color(0xFF06B6D4)
private val NeonGreen = Color(0xFF22C55E)
private val NeonYellow = Color(0xFFF59E0B)
private val NeonRed = Color(0xFFEF4444)
private val NeonPurple = Color(0xFF8B5CF6)
private val SlateLabel = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteTrafficScreen(
    navController: NavController
) {
    val repository = remember { NetworkTrafficRepository() }

    var routes by remember { mutableStateOf<List<RouteTraffic>>(emptyList()) }
    var zones by remember { mutableStateOf<List<ZoneOccupancy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Polling cada 15 segundos
    LaunchedEffect(Unit) {
        while (true) {
            try {
                isLoading = true
                routes = repository.getRoutes()
                zones = repository.getZoneTraffic()
            } catch (_: Exception) { }
            isLoading = false
            delay(15000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Traffic, contentDescription = null, tint = NeonCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Tráfico del Circuito", color = OffWhite, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DeepBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: Rutas / Zonas
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = NeonCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Rutas", fontWeight = FontWeight.Medium)
                    }
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Zonas", fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (isLoading && routes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                when (selectedTab) {
                    0 -> RoutesTab(routes)
                    1 -> ZonesTab(zones)
                }
            }
        }
    }
}

@Composable
private fun RoutesTab(routes: List<RouteTraffic>) {
    if (routes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay datos de rutas", color = SlateLabel)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(routes, key = { it.id }) { route ->
            RouteCard(route)
        }
    }
}

@Composable
private fun RouteCard(route: RouteTraffic) {
    val statusColor = when (route.status) {
        RouteTrafficStatus.OPERATIVA -> NeonGreen
        RouteTrafficStatus.SATURADA -> NeonYellow
        RouteTrafficStatus.CERRADA -> NeonRed
        RouteTrafficStatus.MANTENIMIENTO -> NeonPurple
    }

    val capacityPct = route.capacityPercentage.coerceIn(0, 100)
    val barColor by animateColorAsState(
        when {
            capacityPct >= 90 -> NeonRed
            capacityPct >= 70 -> NeonYellow
            else -> NeonGreen
        },
        label = "barColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: nombre + status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.name, color = OffWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(route.origin, color = SlateLabel, fontSize = 12.sp)
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateLabel, modifier = Modifier.size(14.dp).padding(horizontal = 2.dp))
                        Text(route.destination, color = SlateLabel, fontSize = 12.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        route.status.name,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Usuarios activos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Usuarios activos", color = SlateLabel, fontSize = 13.sp)
                Text(
                    "${route.activeUsers}",
                    color = if (route.capacityPercentage >= 80) NeonRed else OffWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Barra de capacidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Capacidad", color = SlateLabel, fontSize = 12.sp)
                Text("$capacityPct%", color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E1E2A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = capacityPct / 100f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricChip("🚶", "${route.velocity} m/s")
                MetricChip("📏", "${route.distance}m")
                MetricChip("⏱", if (route.estimatedTime > 0) "${route.estimatedTime} min" else "–")
            }
        }
    }
}

@Composable
private fun ZonesTab(zones: List<ZoneOccupancy>) {
    if (zones.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay datos de zonas", color = SlateLabel)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(zones, key = { it.id }) { zone ->
            ZoneCard(zone)
        }
    }
}

@Composable
private fun ZoneCard(zone: ZoneOccupancy) {
    val statusColor = when (zone.status) {
        ZoneOccupancyStatus.ABIERTA, ZoneOccupancyStatus.OPERATIVA -> NeonGreen
        ZoneOccupancyStatus.SATURADA -> NeonYellow
        ZoneOccupancyStatus.CERRADA -> NeonRed
        ZoneOccupancyStatus.MANTENIMIENTO -> NeonPurple
    }

    val pct = zone.occupancyPercentage.coerceIn(0, 100)
    val barColor by animateColorAsState(
        when {
            pct >= 85 -> NeonRed
            pct >= 60 -> NeonYellow
            else -> NeonGreen
        },
        label = "zoneBarColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(zone.name, color = OffWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Tipo: ${zone.type}", color = SlateLabel, fontSize = 12.sp)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        zone.status.name,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Ocupación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ocupación", color = SlateLabel, fontSize = 12.sp)
                Text(
                    "${zone.currentOccupancy} / ${zone.capacity}",
                    color = OffWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1E1E2A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = pct / 100f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barColor)
                )
            }
            Text("$pct%", color = barColor, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))

            Spacer(Modifier.height(10.dp))

            // Métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricChip("🌡️", "${zone.temperature}°C")
                MetricChip("⏳", "${zone.waitTime} min")
                MetricChip("⬆️", "${zone.entryRate}/min")
                MetricChip("⬇️", "${zone.exitRate}/min")
            }
        }
    }
}

@Composable
private fun MetricChip(emoji: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1E1E2A)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Text(value, color = OffWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
