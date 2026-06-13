package com.georacing.georacing.ui.screens.fan

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import kotlinx.coroutines.delay

// ── Team data model ──
private data class RacingTeam(
    val id: String,
    val name: String,
    val series: String, // "F1" or "MotoGP"
    val primaryColor: Color,
    val secondaryColor: Color,
    val icon: String
)

// ── Trivia question model ──
private data class TriviaQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val category: String
)

private val allTeams = listOf(
    // F1
    RacingTeam("ferrari", "Ferrari", "F1", Color(0xFFDC0000), Color(0xFFFFF200), "🏎️"),
    RacingTeam("redbull", "Red Bull Racing", "F1", Color(0xFF1E41FF), Color(0xFFFFD700), "🐂"),
    RacingTeam("mercedes", "Mercedes-AMG", "F1", Color(0xFF00D2BE), Color(0xFFC0C0C0), "⭐"),
    RacingTeam("mclaren", "McLaren", "F1", Color(0xFFFF8700), Color(0xFF47C7FC), "🧡"),
    RacingTeam("astonmartin", "Aston Martin", "F1", Color(0xFF006F62), Color(0xFFC6FF00), "💚"),
    RacingTeam("alpine", "Alpine", "F1", Color(0xFF0090FF), Color(0xFFFF69B4), "🔵"),
    RacingTeam("williams", "Williams", "F1", Color(0xFF005AFF), Color(0xFF00A0DE), "🏁"),
    RacingTeam("haas", "Haas F1 Team", "F1", Color(0xFFB6BABD), Color(0xFFE10600), "🇺🇸"),
    RacingTeam("sauber", "Kick Sauber", "F1", Color(0xFF52E252), Color(0xFF000000), "🟢"),
    RacingTeam("racingbulls", "RB", "F1", Color(0xFF6692FF), Color(0xFFED1A3B), "🔷"),
    // MotoGP
    RacingTeam("ducati", "Ducati Lenovo", "MotoGP", Color(0xFFCC0000), Color(0xFFFFFFFF), "🏍️"),
    RacingTeam("yamaha", "Monster Yamaha", "MotoGP", Color(0xFF0C4DA2), Color(0xFF000000), "💙"),
    RacingTeam("honda", "Repsol Honda", "MotoGP", Color(0xFFFF6A13), Color(0xFFCC0000), "🔶"),
    RacingTeam("ktm", "Red Bull KTM", "MotoGP", Color(0xFFFF6900), Color(0xFF1E1E2E), "🧡"),
    RacingTeam("aprilia", "Aprilia Racing", "MotoGP", Color(0xFF9B0000), Color(0xFF000000), "🏴")
)

private val triviaQuestions = listOf(
    TriviaQuestion("¿Cuántas curvas tiene el Circuit de Barcelona-Catalunya?", listOf("14", "16", "12", "18"), 1, "Circuito"),
    TriviaQuestion("¿En qué año se inauguró el Circuito de Montmeló?", listOf("1989", "1991", "1985", "1993"), 1, "Historia"),
    TriviaQuestion("¿Cuál es la velocidad máxima estimada en la recta principal?", listOf("280 km/h", "310 km/h", "340 km/h", "360 km/h"), 2, "Datos"),
    TriviaQuestion("¿Qué piloto tiene más victorias en Montmeló?", listOf("Hamilton", "Schumacher", "Alonso", "Vettel"), 1, "Pilotos"),
    TriviaQuestion("¿Cuál es la longitud del circuito de Montmeló?", listOf("3.9 km", "4.2 km", "4.6 km", "5.1 km"), 2, "Circuito"),
    TriviaQuestion("¿Qué significa DRS en F1?", listOf("Drag Reduction System", "Drive Recovery System", "Dynamic Racing Speed", "Double Rate Shift"), 0, "Técnica"),
    TriviaQuestion("¿Cuántos GP de España se han celebrado en Montmeló?", listOf("Más de 20", "Más de 30", "Más de 15", "Más de 25"), 1, "Historia"),
    TriviaQuestion("¿Qué tipo de neumático es el más blando en F1?", listOf("Duro", "Medio", "Blando", "Intermedio"), 2, "Técnica")
)

/**
 * FanZoneScreen — Premium Fan Experience.
 * Mirrors iOS: FanZoneView.swift
 *
 * Features:
 * - Series selector (F1 / MotoGP)
 * - Team selection with team-colored theming
 * - Interactive trivia
 * - Team news section
 * - Collectibles preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FanZoneScreen(navController: NavController) {
    val backdrop = LocalBackdrop.current

    // State
    var selectedSeries by remember { mutableStateOf("F1") }
    var selectedTeam by remember { mutableStateOf<RacingTeam?>(null) }
    var showTeamSelector by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }

    // Trivia state
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableIntStateOf(-1) }
    var triviaScore by remember { mutableIntStateOf(0) }
    var answeredCount by remember { mutableIntStateOf(0) }
    var showTriviaResult by remember { mutableStateOf(false) }

    val currentQuestion = triviaQuestions[currentQuestionIndex]
    val teamColor = selectedTeam?.primaryColor ?: RacingRed

    LaunchedEffect(Unit) {
        delay(150)
        showContent = true
    }

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                LiquidTopBar(
                    backdrop = backdrop,
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(teamColor))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("FAN ZONE", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Text(selectedTeam?.name ?: "Elige tu equipo", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }
                    },
                    actions = {
                        // Team selector button
                        IconButton(onClick = { showTeamSelector = true }) {
                            Icon(
                                Icons.Default.SportsMotorsports,
                                "Seleccionar equipo",
                                tint = teamColor
                            )
                        }
                        HomeIconButton {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
            ) {
                // ── Series Selector ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { -20 }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("F1", "MotoGP").forEach { series ->
                                val isSelected = selectedSeries == series
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) teamColor.copy(alpha = 0.2f)
                                            else AsphaltGrey.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) teamColor.copy(alpha = 0.5f) else Color.Transparent,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { selectedSeries = series }
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        series,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = if (isSelected) TextPrimary else TextTertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Team Selection (Horizontal scroll) ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, 100)) + slideInVertically(tween(400, 100)) { -20 }
                    ) {
                        Column {
                            Text(
                                "TU EQUIPO",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(allTeams.filter { it.series == selectedSeries }) { team ->
                                    val isSelected = selectedTeam?.id == team.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected)
                                                    Brush.horizontalGradient(listOf(team.primaryColor.copy(alpha = 0.3f), team.secondaryColor.copy(alpha = 0.15f)))
                                                else
                                                    Brush.horizontalGradient(listOf(AsphaltGrey.copy(alpha = 0.4f), AsphaltGrey.copy(alpha = 0.3f)))
                                            )
                                            .border(
                                                if (isSelected) 1.5.dp else 0.5.dp,
                                                if (isSelected) team.primaryColor.copy(alpha = 0.7f) else MetalGrey.copy(alpha = 0.3f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedTeam = team }
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(team.icon, fontSize = 18.sp)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                team.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) TextPrimary else TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Trivia Section ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 200)) + slideInVertically(tween(500, 200)) { -20 }
                    ) {
                        LiquidCard(
                            backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp,
                            surfaceColor = CarbonBlack.copy(alpha = 0.85f),
                            tint = teamColor.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.QuestionMark, null, tint = teamColor, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("TRIVIA DEL CIRCUITO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = TextPrimary, letterSpacing = 1.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "${currentQuestionIndex + 1}/${triviaQuestions.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
                                    )
                                }

                                Spacer(Modifier.height(16.dp))

                                // Category badge
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(teamColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(currentQuestion.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = teamColor, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    currentQuestion.question,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(Modifier.height(16.dp))

                                // 2x2 Answer grid (like iOS)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (row in 0..1) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            for (col in 0..1) {
                                                val idx = row * 2 + col
                                                if (idx < currentQuestion.options.size) {
                                                    val isCorrect = idx == currentQuestion.correctIndex
                                                    val isSelectedOption = selectedAnswer == idx
                                                    val bgColor = when {
                                                        selectedAnswer == -1 -> AsphaltGrey.copy(alpha = 0.6f)
                                                        isCorrect -> StatusGreen.copy(alpha = 0.25f)
                                                        isSelectedOption -> StatusRed.copy(alpha = 0.25f)
                                                        else -> AsphaltGrey.copy(alpha = 0.3f)
                                                    }
                                                    val borderColor = when {
                                                        selectedAnswer == -1 -> MetalGrey.copy(alpha = 0.3f)
                                                        isCorrect -> StatusGreen.copy(alpha = 0.6f)
                                                        isSelectedOption -> StatusRed.copy(alpha = 0.6f)
                                                        else -> Color.Transparent
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(bgColor)
                                                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                                            .clickable(enabled = selectedAnswer == -1) {
                                                                selectedAnswer = idx
                                                                answeredCount++
                                                                if (isCorrect) triviaScore++
                                                                showTriviaResult = true
                                                            }
                                                            .padding(12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            currentQuestion.options[idx],
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium,
                                                            color = TextPrimary,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Result & Next
                                AnimatedVisibility(visible = showTriviaResult) {
                                    Column {
                                        Spacer(Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                if (selectedAnswer == currentQuestion.correctIndex) "✅ ¡Correcto!" else "❌ Incorrecto",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedAnswer == currentQuestion.correctIndex) StatusGreen else StatusRed
                                            )
                                            TextButton(onClick = {
                                                currentQuestionIndex = (currentQuestionIndex + 1) % triviaQuestions.size
                                                selectedAnswer = -1
                                                showTriviaResult = false
                                            }) {
                                                Text("Siguiente →", color = teamColor, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        // Score
                                        Text(
                                            "Puntuación: $triviaScore/$answeredCount",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Team News Section ──
                item {
                    AnimatedVisibility(
                        visible = showContent && selectedTeam != null,
                        enter = fadeIn(tween(500, 300)) + slideInVertically(tween(500, 300)) { -20 }
                    ) {
                        Column {
                            Text(
                                "NOTICIAS ${selectedTeam?.name?.uppercase() ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(10.dp))

                            // Mock team news (like iOS)
                            val teamNews = listOf(
                                "Resultados del último GP: ${selectedTeam?.name} destaca en clasificación",
                                "Nuevas mejoras aerodinámicas confirmadas para Barcelona",
                                "Entrevista exclusiva con el equipo técnico"
                            )

                            teamNews.forEachIndexed { idx, title ->
                                LiquidCard(
                                    backdrop = backdrop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    cornerRadius = 14.dp,
                                    surfaceColor = AsphaltGrey.copy(alpha = 0.7f),
                                    tint = teamColor.copy(alpha = 0.06f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(teamColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                when (idx) {
                                                    0 -> Icons.Default.EmojiEvents
                                                    1 -> Icons.Default.Build
                                                    else -> Icons.Default.Mic
                                                },
                                                contentDescription = null,
                                                tint = teamColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary,
                                                maxLines = 2
                                            )
                                            Text(
                                                "Hace ${idx + 1}h",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextTertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Collectibles Preview ──
                item {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 400)) + slideInVertically(tween(500, 400)) { -20 }
                    ) {
                        Column {
                            Text(
                                "COLECCIONABLES",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextTertiary,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(3) { idx ->
                                    LiquidCard(
                                        backdrop = backdrop,
                                        modifier = Modifier.size(120.dp),
                                        cornerRadius = 16.dp,
                                        surfaceColor = AsphaltGrey.copy(alpha = 0.6f),
                                        tint = teamColor.copy(alpha = 0.1f)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = TextTertiary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    "Caja Secreta",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextTertiary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "#${idx + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = teamColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Team Selector Bottom Sheet ──
        if (showTeamSelector) {
            TeamSelectorSheet(
                selectedSeries = selectedSeries,
                selectedTeam = selectedTeam,
                onSeriesChange = { selectedSeries = it },
                onTeamSelected = { team ->
                    selectedTeam = team
                    showTeamSelector = false
                },
                onDismiss = { showTeamSelector = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamSelectorSheet(
    selectedSeries: String,
    selectedTeam: RacingTeam?,
    onSeriesChange: (String) -> Unit,
    onTeamSelected: (RacingTeam) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonBlack,
        contentColor = TextPrimary
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "SELECCIONA TU EQUIPO",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(16.dp))

            // Series toggle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("F1", "MotoGP").forEach { series ->
                    FilterChip(
                        selected = selectedSeries == series,
                        onClick = { onSeriesChange(series) },
                        label = { Text(series, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RacingRed.copy(alpha = 0.2f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            allTeams.filter { it.series == selectedSeries }.forEach { team ->
                val isSelected = selectedTeam?.id == team.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) team.primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onTeamSelected(team) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(team.icon, fontSize = 22.sp)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        team.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary
                    )
                    Spacer(Modifier.weight(1f))
                    if (isSelected) {
                        Icon(Icons.Default.CheckCircle, null, tint = team.primaryColor)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
