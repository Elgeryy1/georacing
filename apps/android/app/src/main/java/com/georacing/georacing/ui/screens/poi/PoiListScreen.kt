package com.georacing.georacing.ui.screens.poi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.domain.repository.PoiRepository
import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType

import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LiquidPill
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.kyant.backdrop.Backdrop

// Racing Dark Theme Colors
private val RacingAccent = Color(0xFF06B6D4)    // NeonCyan
private val SearchBarBg = Color(0xFF14141C)       // Dark surface
private val SearchBarText = Color(0xFFF8FAFC)     // Light text
private val ChipBg = Color(0xFF14141C)            // Dark surface
private val ChipBgSelected = Color(0xFF14141C).copy(alpha = 0.8f)
private val ChipText = Color(0xFFF8FAFC)          // Light text
private val SubtleGray = Color(0xFF64748B)        // Slate

// Category data with emojis
data class PoiCategoryChip(
    val emoji: String,
    val label: String,
    val type: PoiType?
)

private val poiCategoryChips = listOf(
    PoiCategoryChip("🚻", "Baños", PoiType.WC),
    PoiCategoryChip("🍔", "Comida", PoiType.FOOD),
    PoiCategoryChip("👕", "Merch", PoiType.MERCH),
    PoiCategoryChip("🎟️", "Accesos", PoiType.GATE),
    PoiCategoryChip("🅿️", "Parking", PoiType.PARKING),
    PoiCategoryChip("🎊", "Fan Zone", PoiType.FANZONE)
)

// Helper function to get emoji for POI type
private fun getEmojiForType(type: PoiType): String {
    return when (type) {
        PoiType.WC -> "🚻"
        PoiType.FOOD -> "🍔"
        PoiType.MERCH -> "👕"
        PoiType.GATE -> "🎟️"
        PoiType.ACCESS -> "🎟️"
        PoiType.PARKING -> "🅿️"
        PoiType.FANZONE -> "🎊"
        PoiType.SERVICE -> "🔧"
        PoiType.EXIT -> "🚪"
        PoiType.OTHER -> "📍"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(
    navController: NavController,
    poiRepository: PoiRepository
) {
    val viewModel: PoiViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                PoiViewModel(poiRepository)
            }
        }
    )

    val pois by viewModel.visiblePois.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val backdrop = LocalBackdrop.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080810))
        ) {
            // Add top padding for status bar + floating pill
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(72.dp))
            
            // Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    LiquidPill(
                        backdrop = backdrop,
                        modifier = Modifier.clickable { viewModel.filterByType(null) },
                        surfaceColor = if (selectedType == null) RacingAccent.copy(alpha = 0.2f) else ChipBg.copy(alpha = 0.5f),
                        tint = if (selectedType == null) RacingAccent else Color.Unspecified
                    ) {
                        Text(
                            "Todos",
                            color = if (selectedType == null) Color.White else ChipText,
                            fontWeight = if (selectedType == null) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                items(poiCategoryChips) { chip ->
                    val isSelected = selectedType == chip.type
                    LiquidPill(
                        backdrop = backdrop,
                        modifier = Modifier.clickable {
                            viewModel.filterByType(if (isSelected) null else chip.type)
                        },
                        surfaceColor = if (isSelected) RacingAccent.copy(alpha = 0.2f) else ChipBg.copy(alpha = 0.5f),
                        tint = if (isSelected) RacingAccent else Color.Unspecified
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(chip.emoji, fontSize = 14.sp)
                            Text(
                                chip.label,
                                color = if (isSelected) Color.White else ChipText,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // POI List with pull-to-refresh
            var isRefreshing by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    scope.launch { delay(1500); isRefreshing = false }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pois) { poi ->
                        PoiCard(
                            poi = poi,
                            backdrop = backdrop,
                            onNavigateClick = { /* TODO: Navigate to POI */ }
                        )
                    }
                }
            }
        }
        
        // Floating Search Pill (Top)
        LiquidPill(
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            surfaceColor = SearchBarBg.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { /* TODO: Open search */ }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Icon
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = ChipText
                    )
                }
                
                // Search Text
                Text(
                    text = "Buscar puntos de interés...",
                    color = SubtleGray,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                
                // Home Icon
                IconButton(onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                        popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                    }
                }) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Inicio",
                        tint = ChipText
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun PoiCard(
    poi: Poi,
    backdrop: Backdrop,
    onNavigateClick: () -> Unit
) {
    LiquidCard(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        surfaceColor = com.georacing.georacing.ui.theme.AsphaltGrey.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Emoji Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E0E18)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getEmojiForType(poi.type),
                    fontSize = 24.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SearchBarText
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description
                Text(
                    text = poi.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SubtleGray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Zone/Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SubtleGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (poi.zone.isNotEmpty()) poi.zone else "Circuit",
                        style = MaterialTheme.typography.labelMedium,
                        color = SubtleGray
                    )
                }
                
                // Wait time (simulated)
                if (poi.type == PoiType.WC || poi.type == PoiType.FOOD) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = RacingAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tiempo de espera: ~5 min",
                            style = MaterialTheme.typography.labelMedium,
                            color = RacingAccent
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action Button
                Button(
                    onClick = onNavigateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RacingAccent
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "CÓMO LLEGAR",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF080810)
                    )
                }
            }
        }
    }
}
