package com.georacing.georacing.ui.screens.alerts

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.model.NewsCategory
import com.georacing.georacing.domain.model.NewsPriority
import com.georacing.georacing.domain.model.RaceNews
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AlertsScreen — Premium News & Alerts HUD
 * Noticias, incidencias y emergencias desde el backend Metropolis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {
    val backdrop = LocalBackdrop.current
    var selectedCategory by remember { mutableStateOf<NewsCategory?>(null) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    // Noticias + Incidencias + Emergencias desde el backend
    var allNews by remember { mutableStateOf<List<RaceNews>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Carga news + incidents (Panel Metropolis) + emergencies
    suspend fun loadNews() {
        isLoading = true
        loadError = null
        try {
            val combined = mutableListOf<RaceNews>()

            // 1) Tabla "news" — noticias editoriales
            try {
                val newsResponse = FirestoreLikeClient.api.read("news")
                combined += newsResponse.mapNotNull { map ->
                    try {
                        RaceNews(
                            id = map["id"]?.toString() ?: return@mapNotNull null,
                            title = map["title"]?.toString() ?: "",
                            content = map["content"]?.toString() ?: "",
                            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            category = try { NewsCategory.valueOf(map["category"]?.toString() ?: "RACE_UPDATE") } catch (_: Exception) { NewsCategory.RACE_UPDATE },
                            priority = try { NewsPriority.valueOf(map["priority"]?.toString() ?: "LOW") } catch (_: Exception) { NewsPriority.LOW }
                        )
                    } catch (_: Exception) { null }
                }
            } catch (e: Exception) {
                Log.w("AlertsScreen", "No se pudieron cargar news: ${e.message}")
            }

            // 2) Tabla "incidents" — incidencias gestionadas desde Panel Metropolis
            try {
                val incidentsResponse = FirestoreLikeClient.api.read("incidents")
                combined += incidentsResponse.mapNotNull { map ->
                    try {
                        val status = map["status"]?.toString()?.uppercase() ?: "ACTIVE"
                        if (status == "RESOLVED") return@mapNotNull null

                        val level = map["level"]?.toString()?.uppercase() ?: "INFO"
                        val priority = when (level) {
                            "CRITICAL" -> NewsPriority.HIGH
                            "WARNING" -> NewsPriority.MEDIUM
                            else -> NewsPriority.LOW
                        }

                        RaceNews(
                            id = "incident_${map["id"]}",
                            title = "⚠️ ${map["category"]?.toString() ?: "Incidencia"}",
                            content = map["description"]?.toString() ?: "",
                            timestamp = parseTimestamp(map["created_at"]),
                            category = NewsCategory.SAFETY,
                            priority = priority
                        )
                    } catch (_: Exception) { null }
                }
            } catch (e: Exception) {
                Log.w("AlertsScreen", "No se pudieron cargar incidents: ${e.message}")
            }

            // 3) Tabla "emergencies" — emergencias activas del Panel
            try {
                val emergResponse = FirestoreLikeClient.api.read("emergencies")
                combined += emergResponse.mapNotNull { map ->
                    try {
                        val status = map["status"]?.toString()?.uppercase() ?: "ACTIVE"
                        if (status == "RESOLVED") return@mapNotNull null

                        val level = map["level"]?.toString()?.uppercase() ?: "WARNING"
                        val priority = when (level) {
                            "CRITICAL" -> NewsPriority.HIGH
                            "WARNING" -> NewsPriority.HIGH
                            "ADVISORY" -> NewsPriority.MEDIUM
                            else -> NewsPriority.LOW
                        }

                        RaceNews(
                            id = "emergency_${map["id"]}",
                            title = "🚨 ${map["title"]?.toString() ?: "Emergencia"}",
                            content = map["description"]?.toString() ?: "",
                            timestamp = parseTimestamp(map["startedAt"] ?: map["created_at"]),
                            category = NewsCategory.SAFETY,
                            priority = priority
                        )
                    } catch (_: Exception) { null }
                }
            } catch (e: Exception) {
                Log.w("AlertsScreen", "No se pudieron cargar emergencies: ${e.message}")
            }

            allNews = combined.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.w("AlertsScreen", "Error general cargando alertas: ${e.message}")
            loadError = e.message
            allNews = emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadNews() }

    val filteredNews = if (selectedCategory == null) allNews else allNews.filter { it.category == selectedCategory }

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(RacingRed))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.newsletter_title).uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(stringResource(R.string.newsletter_subtitle), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
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

            // ── Filter Chips Row ──
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400, 100)) + slideInVertically(tween(400, 100)) { -15 }
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PremiumFilterChip(
                            selected = selectedCategory == null,
                            label = stringResource(R.string.news_category_all),
                            accentColor = RacingRed,
                            onClick = { selectedCategory = null }
                        )
                    }
                    items(NewsCategory.values()) { category ->
                        PremiumFilterChip(
                            selected = selectedCategory == category,
                            label = category.displayName,
                            accentColor = RacingRed,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            // Separator line
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, MetalGrey.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            // ── News Content ──
            if (filteredNews.isEmpty() && !isLoading) {
                // Empty state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 200))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(RacingRed.copy(alpha = 0.12f))
                                    .drawBehind {
                                        drawCircle(RacingRed.copy(alpha = 0.06f), radius = size.minDimension * 0.9f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(40.dp), tint = RacingRed)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                stringResource(R.string.news_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.news_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                var isRefreshing by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        scope.launch { loadNews(); isRefreshing = false }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        itemsIndexed(filteredNews) { index, news ->
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showContent,
                                enter = fadeIn(tween(300, 150 + index * 50)) + slideInVertically(tween(300, 150 + index * 50)) { 25 }
                            ) {
                                PremiumNewsCard(news = news)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════
// ── Premium Composables ──
// ══════════════════════════════════════════════

@Composable
fun PremiumNewsCard(news: RaceNews) {
    val priorityColor = when (news.priority) {
        NewsPriority.HIGH -> RacingRed
        NewsPriority.MEDIUM -> StatusAmber
        NewsPriority.LOW -> StatusGreen
    }

    val timeText = getTimeAgo(news.timestamp)

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = priorityColor)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Priority indicator with glow
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(priorityColor.copy(alpha = 0.4f), radius = size.minDimension)
                    }
                    .clip(CircleShape)
                    .background(priorityColor)
                    .align(Alignment.Top)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category badge
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            news.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    news.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    news.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun PremiumFilterChip(
    selected: Boolean,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.2f) else MetalGrey.copy(alpha = 0.25f)
    val textColor = if (selected) TextPrimary else TextSecondary

    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                if (selected) Modifier.border(0.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .graphicsLayer { clip = false }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)

    return when {
        minutes < 1 -> stringResource(R.string.news_time_now)
        minutes < 60 -> stringResource(R.string.news_time_minutes, minutes.toInt())
        else -> stringResource(R.string.news_time_hours, hours.toInt())
    }
}

/**
 * Parsea timestamps del backend: puede ser Long (epoch millis), String ISO ("2024-01-15 10:30:00")
 */
private fun parseTimestamp(value: Any?): Long {
    return when (value) {
        is Number -> value.toLong().let { if (it < 1_000_000_000_000L) it * 1000 else it }
        is String -> try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            sdf.parse(value)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                sdf.parse(value)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) { System.currentTimeMillis() }
        }
        else -> System.currentTimeMillis()
    }
}
