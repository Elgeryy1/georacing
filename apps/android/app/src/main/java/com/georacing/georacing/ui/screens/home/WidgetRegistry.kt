package com.georacing.georacing.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.domain.model.WidgetType
import com.georacing.georacing.ui.components.CircuitStatusCard
import com.georacing.georacing.ui.components.contextual.ContextualCardWidget

@Composable
fun RenderWidget(
    type: WidgetType,
    navController: NavController,
    circuitState: com.georacing.georacing.domain.model.CircuitState?,
    temperature: String,
    onNavigateToEdit: () -> Unit,
    appContainer: com.georacing.georacing.di.AppContainer? = null,
    newsItems: List<com.georacing.georacing.ui.screens.home.NewsArticle> = emptyList(),
    isOnline: Boolean = true
) {
    when (type) {
        WidgetType.CONTEXTUAL_CARD -> {
            ContextualCardWidget(
                circuitState = circuitState,
                isOnline = isOnline,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.METEOROLOGY -> {
            // Reusing existing GreetingHeader, slightly modified or wrapped
            // Ideally we should refactor GreetingsHeader to be cleaner here, but we can pass params
             com.georacing.georacing.ui.screens.home.GreetingsHeader(temperature = temperature)
             Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.STATUS_CARD -> {
            val state = circuitState ?: com.georacing.georacing.domain.model.CircuitState(
                com.georacing.georacing.domain.model.CircuitMode.UNKNOWN,
                "Cargando...",
                "--",
                ""
            )
            com.georacing.georacing.ui.components.CircuitStatusCard(
                mode = state.mode,
                message = state.message,
                temperature = state.temperature ?: "--",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
        WidgetType.ACTIONS_GRID -> {
             com.georacing.georacing.ui.screens.home.DashboardGrid(navController)
             Spacer(modifier = Modifier.height(32.dp))
        }
        WidgetType.NEWS_FEED -> {
            // Real news from API (iOS parity: HomeView → newsSection)
            if (newsItems.isNotEmpty()) {
                newsItems.take(3).forEach { article ->
                    val backdrop = com.georacing.georacing.ui.glass.LocalBackdrop.current
                    com.georacing.georacing.ui.glass.LiquidCard(
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        cornerRadius = 18.dp,
                        surfaceColor = Color(0xFF14141C).copy(alpha = 0.75f),
                        tint = Color(0xFFE8253A).copy(alpha = 0.05f)
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(Color(0xFFE8253A))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.material3.Text(
                                    article.category.uppercase(),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFE8253A)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.Text(
                                article.title,
                                style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 2
                            )
                            if (article.subtitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.material3.Text(
                                    article.subtitle,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            } else {
                // Fallback static card
                com.georacing.georacing.ui.screens.home.LatestNewsCard()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.PARKING_INFO -> {
             // Parking Widget - Dynamic
             val parkingLocation = appContainer?.parkingRepository?.parkingLocation?.collectAsState(initial = null)?.value
             
             androidx.compose.material3.Card(
                 colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                 modifier = Modifier.fillMaxWidth().clickable {
                     navController.navigate(com.georacing.georacing.ui.navigation.Screen.Parking.route)
                 }
             ) {
                 androidx.compose.foundation.layout.Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                 ) {
                     androidx.compose.material3.Icon(
                         androidx.compose.material.icons.Icons.Default.LocalParking,
                         contentDescription = "Parking",
                         tint = if (parkingLocation != null) Color(0xFF22C55E) else Color(0xFF64748B)
                     )
                     Spacer(modifier = Modifier.width(16.dp))
                     androidx.compose.foundation.layout.Column {
                         androidx.compose.material3.Text("Tu Coche", color = Color(0xFFF8FAFC), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                         if (parkingLocation != null) {
                              androidx.compose.material3.Text("Ubicación guardada", color = Color(0xFF22C55E), fontSize = 12.sp)
                         } else {
                              androidx.compose.material3.Text("No hay ubicación guardada", color = Color(0xFF64748B), fontSize = 12.sp)
                         }
                     }
                 }
             }
             Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.STAFF_ACTIONS -> {
             // Only visible for staff - placeholder
        }
        WidgetType.ECO_METER -> {
            // Eco Meter (Mini)
             androidx.compose.material3.Card(
                 colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF0A2A1A)),
                 modifier = Modifier.fillMaxWidth().clickable {
                      navController.navigate(com.georacing.georacing.ui.navigation.Screen.EcoMeter.route)
                 }
             ) {
                 androidx.compose.foundation.layout.Column(modifier = Modifier.padding(16.dp)) {
                    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Eco, contentDescription=null, tint=Color(0xFF22C55E))
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.Text("EcoMeter", color = Color(0xFFF8FAFC), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = 0.7f,
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                        color = Color(0xFF22C55E),
                        trackColor = Color(0xFF64748B).copy(alpha=0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                         androidx.compose.material3.Text("CO2 Ahorrado: 12kg", color = Color(0xFFF8FAFC).copy(0.7f), fontSize = 12.sp)
                         androidx.compose.material3.Text("Nivel 5", color = Color(0xFFE8253A), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                 }
             }
             Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.AR_ACCESS -> {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF1A1040)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.AR.route)
                }
            ) {
                 androidx.compose.foundation.layout.Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                 ) {
                     androidx.compose.material3.Icon(
                         androidx.compose.material.icons.Icons.Default.ViewInAr,
                         contentDescription = "Experiencia AR",
                         tint = Color(0xFF06B6D4)
                     )
                     Spacer(modifier = Modifier.width(16.dp))
                     androidx.compose.material3.Text("Experiencia AR", color = Color(0xFFF8FAFC), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                 }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.FIND_RESTROOMS -> {
            // Placeholder
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                 androidx.compose.foundation.layout.Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                 ) {
                     androidx.compose.material3.Icon(
                         androidx.compose.material.icons.Icons.Default.Wc,
                         contentDescription = "Buscar aseos",
                         tint = Color(0xFF64748B)
                     )
                     Spacer(modifier = Modifier.width(16.dp))
                     androidx.compose.material3.Text("Buscar Aseos", color = Color(0xFFF8FAFC))
                 }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.FOOD_OFFERS -> {
             androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF2A1508)),
                modifier = Modifier.fillMaxWidth()
            ) {
                 androidx.compose.foundation.layout.Row(
                     modifier = Modifier.padding(16.dp),
                     verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                 ) {
                     androidx.compose.material3.Icon(
                         androidx.compose.material.icons.Icons.Default.Fastfood,
                         contentDescription = "Ofertas de comida",
                         tint = Color(0xFFF97316)
                     )
                     Spacer(modifier = Modifier.width(16.dp))
                     androidx.compose.material3.Text("Ofertas Gastronómicas", color = Color(0xFFF8FAFC), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                 }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.ACHIEVEMENTS -> {
            // Gamification widget - Fan level mini card
            val profile = appContainer?.gamificationRepository?.profile?.collectAsState()?.value
            if (profile != null) {
                val unlockedCount = profile.achievements.count { it.isUnlocked }
                val totalCount = profile.achievements.size
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                    modifier = Modifier.fillMaxWidth().clickable {
                        navController.navigate(com.georacing.georacing.ui.navigation.Screen.Achievements.route)
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE8253A).copy(alpha = 0.2f))
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        // Level badge
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        listOf(Color(0xFFE8253A).copy(alpha = 0.3f), Color.Transparent)
                                    )
                                )
                                .border(1.5.dp, Color(0xFFE8253A).copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                "${profile.level}",
                                color = Color.White,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                            androidx.compose.material3.Text(profile.levelName, color = Color(0xFFF8FAFC), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { profile.xpProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
                                color = Color(0xFFE8253A),
                                trackColor = Color(0xFF1E1E2A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.Text(
                                "🏆 $unlockedCount/$totalCount logros · ${profile.totalXP} XP",
                                color = Color(0xFF64748B), fontSize = 11.sp
                            )
                        }
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Default.ChevronRight,
                            contentDescription = "Ver logros",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        WidgetType.SEARCH_ACCESS -> {
            // Quick search bar
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Search.route)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.material3.Text(
                        "Buscar en el circuito...",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.CLICK_COLLECT -> {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.ClickCollect.route)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Click & Collect",
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        androidx.compose.material3.Text(
                            "Pide comida y recoge sin colas",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.WRAPPED -> {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Wrapped.route)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "GeoRacing Wrapped",
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        androidx.compose.material3.Text(
                            "Tu resumen post-evento",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.COLLECTIBLES -> {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.Collectibles.route)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.AccountBox,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Cromos Digitales",
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        androidx.compose.material3.Text(
                            "Colecciona 24 cromos exclusivos",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        WidgetType.PROXIMITY_CHAT -> {
            androidx.compose.material3.Card(
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(com.georacing.georacing.ui.navigation.Screen.ProximityChat.route)
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Forum,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text(
                            "Chat Cercano",
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        androidx.compose.material3.Text(
                            "Habla con fans cercanos via BLE",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
