package com.georacing.georacing.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.event.labelForMapLayer
import com.georacing.georacing.data.event.labelForRoute
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.WidgetType
import com.georacing.georacing.ui.components.CircuitStatusCard
import com.georacing.georacing.ui.components.contextual.ContextualCardWidget
import com.georacing.georacing.ui.components.racecontrol.RaceControlWidget
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.LocalActiveEventConfig
import com.georacing.georacing.ui.theme.LocalEventVisualStyle
import com.kyant.backdrop.Backdrop

@Composable
fun RenderWidget(
    type: WidgetType,
    navController: NavController,
    circuitState: com.georacing.georacing.domain.model.CircuitState?,
    temperature: String,
    onNavigateToEdit: () -> Unit,
    appContainer: com.georacing.georacing.di.AppContainer? = null,
    newsItems: List<com.georacing.georacing.ui.screens.home.NewsArticle> = emptyList(),
    isOnline: Boolean = true,
    isLoadingNews: Boolean = false
) {
    val backdrop = LocalBackdrop.current
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current
    val palette = visuals.featurePalette

    when (type) {
        WidgetType.CONTEXTUAL_CARD -> {
            ContextualCardWidget(
                circuitState = circuitState,
                isOnline = isOnline,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        WidgetType.METEOROLOGY -> {
            GreetingsHeader(
                circuitState = circuitState,
                isOnline = isOnline
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        WidgetType.STATUS_CARD -> {
            val state = circuitState ?: com.georacing.georacing.domain.model.CircuitState(
                com.georacing.georacing.domain.model.CircuitMode.UNKNOWN,
                "Cargando...",
                "--",
                ""
            )
            if (state.hasLiveSession()) {
                RaceControlWidget(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                CircuitStatusCard(
                    mode = state.mode,
                    message = state.message,
                    temperature = state.temperature ?: "--",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.ACTIONS_GRID -> {
            DashboardGrid(
                navController = navController,
                circuitState = circuitState
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.NEWS_FEED -> {
            if (newsItems.isNotEmpty()) {
                newsItems.take(3).forEachIndexed { index, article ->
                    val accent = palette[index % palette.size]
                    LiquidCard(
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        cornerRadius = 18.dp,
                        surfaceColor = visuals.panelSurface,
                        tint = accent.copy(alpha = 0.06f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    article.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = accent
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                article.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = visuals.heroHeadlineColor,
                                maxLines = 2
                            )
                            if (article.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    article.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = visuals.heroBodyColor,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            } else {
                LatestNewsCard(
                    circuitState = circuitState,
                    isLoading = isLoadingNews
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.PARKING_INFO -> {
            val parkingLocation = appContainer?.parkingRepository?.parkingLocation
                ?.collectAsState(initial = null)?.value
            val hasCar = parkingLocation != null
            val accentColor = if (hasCar) palette[2 % palette.size] else visuals.panelMuted

            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.LocalParking,
                accentColor = accentColor,
                title = activeEvent.labelForRoute(Screen.Parking.route, "Tu coche"),
                subtitle = if (hasCar) "Ubicacion guardada" else "No hay ubicacion guardada",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Parking.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.STAFF_ACTIONS -> { /* Only visible for staff */ }

        WidgetType.ECO_METER -> {
            val ecoAccent = palette[2 % palette.size]
            LiquidCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.EcoMeter.route)
                },
                cornerRadius = 18.dp,
                surfaceColor = visuals.panelSurface,
                tint = ecoAccent.copy(alpha = 0.08f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlowIcon(icon = Icons.Default.Eco, color = ecoAccent)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "ECOMETER",
                            color = visuals.heroHeadlineColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = visuals.panelMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { 0.7f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = ecoAccent,
                        trackColor = visuals.loaderTrack
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "CO₂ Ahorrado: 12kg",
                            color = visuals.heroBodyColor,
                            fontSize = 12.sp
                        )
                        Text(
                            "Nivel 5",
                            color = ecoAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.AR_ACCESS -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.ViewInAr,
                accentColor = palette[1 % palette.size],
                title = "Experiencia AR",
                subtitle = "Realidad aumentada en el circuito",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.AR.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.FIND_RESTROOMS -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.Wc,
                accentColor = palette[3 % palette.size],
                title = activeEvent.labelForMapLayer("RESTROOM", "Servicios"),
                subtitle = "Encuentra los mas cercanos",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.PoiList.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.FOOD_OFFERS -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.Fastfood,
                accentColor = palette[2 % palette.size],
                title = activeEvent.labelForMapLayer("FOOD", "Food"),
                subtitle = "Descubre opciones del recinto",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Orders.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.ACHIEVEMENTS -> {
            val profile = appContainer?.gamificationRepository?.profile?.collectAsState()?.value
            if (profile != null) {
                val unlockedCount = profile.achievements.count { it.isUnlocked }
                val totalCount = profile.achievements.size
                LiquidCard(
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth().clickable {
                        navController.navigate(com.georacing.georacing.ui.navigation.Screen.Achievements.route)
                    },
                    cornerRadius = 18.dp,
                    surfaceColor = visuals.panelSurface,
                    tint = visuals.navSelected.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Level badge with glow
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            visuals.navSelected.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    1.5.dp,
                                    visuals.navSelected.copy(alpha = 0.35f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${profile.level}",
                                color = visuals.heroHeadlineColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                profile.levelName,
                                color = visuals.heroHeadlineColor,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { profile.xpProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = visuals.navSelected,
                                trackColor = visuals.loaderTrack
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                "🏆 $unlockedCount/$totalCount logros · ${profile.totalXP} XP",
                                color = visuals.panelMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = visuals.panelMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.SEARCH_ACCESS -> {
            LiquidCard(
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Search.route)
                },
                cornerRadius = 14.dp,
                surfaceColor = visuals.searchSurface,
                tint = visuals.navSelected.copy(alpha = 0.04f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = visuals.searchHint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (circuitState.isEventDayActive()) "Buscar puerta, grada o fan zone..."
                        else "Buscar en el circuito...",
                        color = visuals.searchHint,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.CLICK_COLLECT -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.ShoppingCart,
                accentColor = palette[1 % palette.size],
                title = activeEvent.ctaLabel,
                subtitle = "Pide comida y recoge sin colas",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.ClickCollect.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.WRAPPED -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.Star,
                accentColor = palette[0 % palette.size],
                title = "${activeEvent.shortName} Wrapped",
                subtitle = "Tu resumen post-evento",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Wrapped.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.COLLECTIBLES -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.AccountBox,
                accentColor = palette[1 % palette.size],
                title = "Cromos Digitales",
                subtitle = "Colecciona 24 cromos exclusivos",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Collectibles.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        WidgetType.PROXIMITY_CHAT -> {
            GlassWidgetCard(
                backdrop = backdrop,
                icon = Icons.Default.Forum,
                accentColor = palette[2 % palette.size],
                title = "Chat Cercano",
                subtitle = "Habla con fans cercanos via BLE",
                onClick = {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.ProximityChat.route)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Reusable glassmorphism widget card
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun GlassWidgetCard(
    backdrop: Backdrop,
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val visuals = LocalEventVisualStyle.current

    LiquidCard(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        cornerRadius = 18.dp,
        surfaceColor = visuals.panelSurface,
        tint = accentColor.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlowIcon(icon = icon, color = accentColor)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = visuals.heroHeadlineColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = visuals.heroBodyColor,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = visuals.panelMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
// Glowing icon with soft radial background
// ─────────────────────────────────────────────────────────────────────

@Composable
private fun GlowIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    listOf(color.copy(alpha = 0.2f), Color.Transparent)
                )
            )
            .border(
                0.5.dp,
                color.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}
