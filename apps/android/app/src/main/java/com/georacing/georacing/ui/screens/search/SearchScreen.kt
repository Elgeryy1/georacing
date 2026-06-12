package com.georacing.georacing.ui.screens.search

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.car.PoiRepository as CarPoiRepository
import com.georacing.georacing.car.PoiType
import com.georacing.georacing.ui.navigation.Screen

private data class SearchResult(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
    val category: String
)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Searchable items: combinación de POIs reales + funcionalidades de la app
    val allItems = remember {
        // POIs reales del Circuit de Barcelona-Catalunya (coordenadas GPS verificadas)
        val poiItems = CarPoiRepository.getAllPois().map { poi ->
            val (icon, color, route, category) = when (poi.type) {
                PoiType.GATE -> Quad(Icons.Default.MeetingRoom, Color(0xFF64748B), Screen.Map.route, "Accesos")
                PoiType.PARKING -> Quad(Icons.Default.LocalParking, Color(0xFF8B8B97), Screen.Parking.route, "Parking")
                PoiType.FANZONE -> Quad(Icons.Default.Attractions, Color(0xFF22C55E), Screen.Map.route, "Experiencia")
                PoiType.SERVICE -> Quad(Icons.Default.Wc, Color(0xFFA855F7), Screen.PoiList.route, "Servicios")
                PoiType.MEDICAL -> Quad(Icons.Default.LocalHospital, Color(0xFFEF4444), Screen.PoiList.route, "Servicios")
                PoiType.OTHER -> Quad(Icons.Default.Place, Color(0xFF3B82F6), Screen.Map.route, "Otros")
            }
            SearchResult(poi.name, poi.description, icon, color, route, category)
        }

        // Funcionalidades de la app (índice estático de navegación)
        val appFeatures = listOf(
            SearchResult("Mapa del Circuito", "Plano interactivo completo", Icons.Default.Map, Color(0xFF3B82F6), Screen.Map.route, "Navegación"),
            SearchResult("Transporte", "Trenes, shuttles y a pie", Icons.Default.DirectionsBus, Color(0xFF22C55E), Screen.Transport.route, "Navegación"),
            SearchResult("F1 Live Telemetría", "Datos en tiempo real", Icons.Default.Speed, Color(0xFFE8253A), Screen.FanImmersive.route, "Experiencia"),
            SearchResult("Momentos", "Galería de fotos del evento", Icons.Default.CameraAlt, Color(0xFFEC4899), Screen.Moments.route, "Experiencia"),
            SearchResult("ClimaSmart", "Tiempo y recomendaciones", Icons.Default.WbSunny, Color(0xFFFFA726), Screen.ClimaSmart.route, "Experiencia"),
            SearchResult("Mi Grupo", "Compartir ubicación con amigos", Icons.Default.Groups, Color(0xFF6366F1), Screen.Group.route, "Social"),
            SearchResult("Alertas", "Noticias y avisos del circuito", Icons.Default.Notifications, Color(0xFFEF4444), Screen.Alerts.route, "Social"),
            SearchResult("Restaurantes", "Comida y bebida del circuito", Icons.Default.Restaurant, Color(0xFFFF6B2C), Screen.Orders.route, "Comida"),
        )

        poiItems + appFeatures
    }

    val filteredItems = remember(query) {
        if (query.isBlank()) emptyList()
        else allItems.filter {
            it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
        }
    }

    val recentSearches = remember { listOf("Parking", "Puerta", "Mapa", "Grupo") }
    val quickAccess = remember {
        listOf(
            SearchResult("Emergencia", "Punto médico más cercano", Icons.Default.LocalHospital, Color(0xFFEF4444), Screen.PoiList.route, "Rápido"),
            SearchResult("Mapa", "Plano interactivo", Icons.Default.Map, Color(0xFF3B82F6), Screen.Map.route, "Rápido"),
            SearchResult("Mi Parking", "Dónde aparqué", Icons.Default.LocalParking, Color(0xFF8B8B97), Screen.Parking.route, "Rápido"),
            SearchResult("Comida", "Restaurantes y bares", Icons.Default.Restaurant, Color(0xFFFF6B2C), Screen.Orders.route, "Rápido")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Buscar en el circuito...", color = Color(0xFF64748B)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White, cursorColor = Color(0xFFE8253A),
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } },
                actions = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", tint = Color(0xFF64748B)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14141C))
            )
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (query.isBlank()) {
                // Recent searches
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("BÚSQUEDAS RECIENTES", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(recentSearches) { recent ->
                    Row(
                        Modifier.fillMaxWidth().clickable { query = recent }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.History, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(recent, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8))
                    }
                }

                // Quick access
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("ACCESO RÁPIDO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(quickAccess) { item ->
                    SearchResultRow(item) { navController.navigate(item.route) }
                }
            } else {
                // Search results
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("${filteredItems.size} resultados", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                    Spacer(Modifier.height(8.dp))
                }

                if (filteredItems.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.SearchOff, null, tint = Color(0xFF64748B), modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("No se encontraron resultados", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF64748B))
                                Text("Prueba con otra búsqueda", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                            }
                        }
                    }
                } else {
                    val grouped = filteredItems.groupBy { it.category }
                    grouped.forEach { (category, items) ->
                        item {
                            Text(category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                        items(items) { item ->
                            SearchResultRow(item) { navController.navigate(item.route) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(result.color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(result.icon, contentDescription = result.title, tint = result.color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                Text(result.subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Ir a ${result.title}", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
        }
    }
}
