package com.georacing.georacing.ui.screens.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.georacing.georacing.car.PoiModel
import com.georacing.georacing.car.PoiRepository
import com.georacing.georacing.car.PoiType
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.navigation.Screen

/**
 * Pantalla de selección de destino del circuito.
 * 
 * Permite al usuario elegir un punto del Circuit de Barcelona Catalunya
 * para iniciar navegación hacia ese destino.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircuitDestinationSelector(
    navController: NavController
) {
    var selectedCategory by remember { mutableStateOf<PoiType?>(null) }
    
    val allPois = remember { PoiRepository.getAllPois() }
    
    val filteredPois = if (selectedCategory != null) {
        allPois.filter { it.type == selectedCategory }
    } else {
        allPois
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ir al Circuit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filtros de categoría
            CategoryFilterRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            
            // Lista de POIs
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Entrada principal destacada (si no hay filtro)
                if (selectedCategory == null) {
                    item {
                        FeaturedDestinationCard(
                            title = "Entrada Principal",
                            description = "Acceso Principal del Circuit de Barcelona-Catalunya",
                            icon = Icons.Default.Stadium,
                            onClick = {
                                navController.navigate("circuit_navigation/main_gate")
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Otros destinos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                
                // Lista de POIs
                items(filteredPois) { poi ->
                    DestinationCard(
                        poi = poi,
                        onClick = {
                            navController.navigate("circuit_navigation/${poi.id}")
                        }
                    )
                }
                
                if (filteredPois.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay destinos en esta categoría",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila de filtros de categoría.
 */
@Composable
fun CategoryFilterRow(
    selectedCategory: PoiType?,
    onCategorySelected: (PoiType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filtro "Todos"
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text("Todos") },
            leadingIcon = {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        
        // Filtro "Parkings"
        FilterChip(
            selected = selectedCategory == PoiType.PARKING,
            onClick = { onCategorySelected(PoiType.PARKING) },
            label = { Text("Parkings") },
            leadingIcon = {
                Icon(
                    Icons.Default.LocalParking,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        
        // Filtro "Accesos"
        FilterChip(
            selected = selectedCategory == PoiType.GATE,
            onClick = { onCategorySelected(PoiType.GATE) },
            label = { Text("Accesos") },
            leadingIcon = {
                Icon(
                    Icons.Default.Login,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
    }
}

/**
 * Tarjeta destacada para destino principal.
 */
@Composable
fun FeaturedDestinationCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            
            Icon(
                Icons.Default.Navigation,
                contentDescription = "Navegar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Tarjeta de destino individual.
 */
@Composable
fun DestinationCard(
    poi: PoiModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono según tipo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getPoiColor(poi.type).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getPoiIcon(poi.type),
                    contentDescription = null,
                    tint = getPoiColor(poi.type),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Información
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = poi.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navegar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Obtiene el icono correspondiente al tipo de POI.
 */
fun getPoiIcon(type: PoiType): ImageVector {
    return when (type) {
        PoiType.PARKING -> Icons.Default.LocalParking
        PoiType.GATE -> Icons.Default.Login
        PoiType.FANZONE -> Icons.Default.Stadium
        PoiType.SERVICE -> Icons.Default.HomeRepairService
        else -> Icons.Default.Place
    }
}

/**
 * Obtiene el color correspondiente al tipo de POI.
 */
@Composable
fun getPoiColor(type: PoiType): androidx.compose.ui.graphics.Color {
    return when (type) {
        PoiType.PARKING -> MaterialTheme.colorScheme.primary
        PoiType.GATE -> MaterialTheme.colorScheme.tertiary
        PoiType.FANZONE -> MaterialTheme.colorScheme.secondary
        PoiType.SERVICE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
}
