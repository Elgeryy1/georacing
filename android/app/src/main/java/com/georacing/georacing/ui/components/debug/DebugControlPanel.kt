package com.georacing.georacing.ui.components.debug

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.domain.model.CircuitMode

// ═══════════════════════════════════════════════════════════
// Color Palette
// ═══════════════════════════════════════════════════════════
private val BgDark = Color(0xFF0A0A14)
private val BgCard = Color(0xFF12121E)
private val BgCardLight = Color(0xFF1A1A2E)
private val AccentCyan = Color(0xFF06B6D4)
private val AccentGreen = Color(0xFF22C55E)
private val AccentRed = Color(0xFFEF4444)
private val AccentOrange = Color(0xFFF97316)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentPink = Color(0xFFEC4899)
private val AccentGold = Color(0xFFEAB308)
private val AccentBlue = Color(0xFF3B82F6)
private val TextDim = Color(0xFF64748B)
private val TextLight = Color(0xFFE2E8F0)

// ═══════════════════════════════════════════════════════════
// Tab definitions
// ═══════════════════════════════════════════════════════════
private enum class DebugTab(val icon: ImageVector, val label: String, val color: Color) {
    PRESETS(Icons.Filled.AutoAwesome, "Presets", AccentPink),
    GAMIFICATION(Icons.Filled.EmojiEvents, "Logros", AccentGold),
    CIRCUIT(Icons.Filled.Flag, "Circuito", AccentRed),
    SOCIAL(Icons.Filled.People, "Social", AccentBlue),
    SYSTEM(Icons.Filled.Settings, "Sistema", AccentCyan)
}

// ═══════════════════════════════════════════════════════════
// MAIN PANEL
// ═══════════════════════════════════════════════════════════
@Composable
fun DebugControlPanel(
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(DebugTab.PRESETS) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val showToast: (String) -> Unit = { msg -> toastMessage = msg }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark.copy(alpha = 0.92f))
            .clickable { onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .clickable(enabled = false) {}
        ) {
            // ── Header ──
            DebugHeader(onDismiss)

            // ── Toast feedback ──
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                toastMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            msg,
                            color = AccentGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(2000)
                        toastMessage = null
                    }
                }
            }

            // ── Tab Bar ──
            DebugTabBar(selectedTab) { selectedTab = it }

            // ── Tab Content ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (selectedTab) {
                    DebugTab.PRESETS -> presetsTab(showToast)
                    DebugTab.GAMIFICATION -> gamificationTab(showToast)
                    DebugTab.CIRCUIT -> circuitTab(showToast)
                    DebugTab.SOCIAL -> socialTab(showToast)
                    DebugTab.SYSTEM -> systemTab(showToast)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// HEADER
// ═══════════════════════════════════════════════════════════
@Composable
private fun DebugHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated indicator
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "pulse"
        )

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = pulseAlpha))
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Debug Console",
                color = TextLight,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                "Simula toda la app sin estar en el circuito",
                color = TextDim,
                fontSize = 11.sp
            )
        }

        // Simulation active indicator
        val isActive = ScenarioSimulator.isSimulationActive
        if (isActive) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentOrange.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = AccentOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
        }

        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, "Cerrar", tint = TextDim)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TAB BAR
// ═══════════════════════════════════════════════════════════
@Composable
private fun DebugTabBar(selected: DebugTab, onSelect: (DebugTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DebugTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val bgColor by animateColorAsState(
                if (isSelected) tab.color.copy(alpha = 0.15f) else Color.Transparent,
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) tab.color else TextDim,
                label = "tabContent"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) }
                    .background(bgColor)
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(tab.icon, tab.label, tint = contentColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    tab.label, color = contentColor, fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TAB: PRESETS — Full scenario simulations
// ═══════════════════════════════════════════════════════════
private fun androidx.compose.foundation.lazy.LazyListScope.presetsTab(toast: (String) -> Unit) {
    item {
        SectionTitle("Escenarios completos", "Un botón = experiencia simulada completa")
    }

    item {
        PresetCard(
            emoji = "🆕", title = "Fan Nuevo",
            subtitle = "Primera visita al circuito. Reset total + primer logro y cromo.",
            gradient = listOf(Color(0xFF1E3A5F), Color(0xFF0F2030)),
            onClick = { ScenarioSimulator.simulateNewFan(); toast("✅ Fan nuevo — reset + primer logro") }
        )
    }
    item {
        PresetCard(
            emoji = "🏎️", title = "Fan Activo — Día Completo",
            subtitle = "5 logros, 5 cromos, 12K pasos, exploración básica.",
            gradient = listOf(Color(0xFF1A3D2E), Color(0xFF0F2018)),
            onClick = { ScenarioSimulator.simulateActiveFanDay(); toast("✅ Día activo simulado") }
        )
    }
    item {
        PresetCard(
            emoji = "⭐", title = "VIP Experience",
            subtitle = "Todo desbloqueado. Todos los logros + épicos + 2000 XP.",
            gradient = listOf(Color(0xFF3D3010), Color(0xFF201A08)),
            onClick = { ScenarioSimulator.simulateVIPExperience(); toast("✅ VIP — todo desbloqueado") }
        )
    }
    item {
        PresetCard(
            emoji = "🚨", title = "Emergencia Total",
            subtitle = "Evacuación BLE + Crowd surge + Logros safety desbloqueados.",
            gradient = listOf(Color(0xFF3D1010), Color(0xFF200808)),
            onClick = { ScenarioSimulator.simulateEmergencyScenario(); toast("✅ Emergencia activada") }
        )
    }
    item {
        PresetCard(
            emoji = "🏁", title = "Race Day",
            subtitle = "Safety Car + hazards + crowd + pedidos + grupo con 3 miembros.",
            gradient = listOf(Color(0xFF2D1040), Color(0xFF180820)),
            onClick = {
                ScenarioSimulator.simulateCircuitMode(CircuitMode.SAFETY_CAR)
                ScenarioSimulator.addHazard(ScenarioSimulator.HazardType.TRAFFIC)
                ScenarioSimulator.triggerCrowdSurge()
                ScenarioSimulator.createOrdersAllStatuses()
                ScenarioSimulator.createFakeGroup()
                ScenarioSimulator.addFakeSteps(8000)
                ScenarioSimulator.spawnRacers(5)
                toast("✅ Race Day simulado")
            }
        )
    }
    item {
        PresetCard(
            emoji = "🌧️", title = "Día Lluvioso",
            subtitle = "Lluvia, 14°C, tráfico denso, bandera amarilla.",
            gradient = listOf(Color(0xFF10253D), Color(0xFF081520)),
            onClick = {
                ScenarioSimulator.simulateWeather(14f, "Lluvia")
                ScenarioSimulator.simulateCircuitMode(CircuitMode.YELLOW_FLAG)
                ScenarioSimulator.addHazard(ScenarioSimulator.HazardType.TRAFFIC)
                ScenarioSimulator.unlockCollectible("c20") // Bajo la lluvia
                toast("✅ Día lluvioso simulado")
            }
        )
    }

    item { Spacer(Modifier.height(4.dp)) }

    item {
        DangerButton(
            text = "🔄 RESET COMPLETO (todo)",
            onClick = { ScenarioSimulator.resetEverything(); toast("✅ Todo reseteado") }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// TAB: GAMIFICATION — Achievements + Collectibles + XP
// ═══════════════════════════════════════════════════════════
private fun androidx.compose.foundation.lazy.LazyListScope.gamificationTab(toast: (String) -> Unit) {
    // ── Achievements section ──
    item { SectionTitle("🏆 Logros", "20 logros en 6 categorías") }

    item {
        ActionRow(
            left = ActionItem("Desbloquear TODOS", AccentGreen) {
                ScenarioSimulator.unlockAllAchievements(); toast("✅ 20 logros desbloqueados")
            },
            right = ActionItem("Resetear TODOS", AccentRed) {
                ScenarioSimulator.resetAllAchievements(); toast("✅ Logros reseteados")
            }
        )
    }

    // Logros individuales agrupados
    item {
        var expanded by remember { mutableStateOf(false) }
        ExpandableSection("Logros individuales (20)", expanded) { expanded = !expanded }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                achievementList.forEach { (category, items) ->
                    Text(category, color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                    items.forEach { (id, label, emoji) ->
                        MiniButton("$emoji $label") {
                            ScenarioSimulator.unlockAchievement(id); toast("✅ $label")
                        }
                    }
                }
            }
        }
    }

    // ── XP ──
    item { SectionTitle("⚡ Experiencia", "Añade XP para subir de nivel") }

    item {
        ActionRow(
            left = ActionItem("+500 XP", AccentPurple) {
                ScenarioSimulator.addBonusXP(500); toast("✅ +500 XP")
            },
            right = ActionItem("+2000 XP", Color(0xFF6D28D9)) {
                ScenarioSimulator.addBonusXP(2000); toast("✅ +2000 XP")
            }
        )
    }

    // ── Collectibles section ──
    item { SectionTitle("🎴 Coleccionables", "24 cromos digitales (4 rarezas)") }

    item {
        ActionRow(
            left = ActionItem("Todos (24)", AccentGreen) {
                ScenarioSimulator.unlockAllCollectibles(); toast("✅ 24 cromos desbloqueados")
            },
            right = ActionItem("Resetear", AccentRed) {
                ScenarioSimulator.resetAllCollectibles(); toast("✅ Cromos reseteados")
            }
        )
    }

    item {
        ActionRow(
            left = ActionItem("🎲 Random 5", AccentPurple) {
                ScenarioSimulator.unlockRandomCollectibles(5); toast("✅ 5 aleatorios")
            },
            right = ActionItem("🎲 Random 12", Color(0xFF7C3AED)) {
                ScenarioSimulator.unlockRandomCollectibles(12); toast("✅ 12 aleatorios")
            }
        )
    }

    item {
        var expanded by remember { mutableStateOf(false) }
        ExpandableSection("Cromos individuales (24)", expanded) { expanded = !expanded }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                // Generamos la lista de 24 cromos a partir de los datos en CollectiblesScreen
                // Como no tenemos acceso directo a la lista estática aquí sin refactorizar,
                // usamos los IDs numéricos y un título genérico (el usuario en debug ya sabe qué es)
                val collectibleNames = mapOf(
                    1 to "Fernando Alonso", 2 to "Lewis Hamilton", 3 to "Max Verstappen",
                    4 to "Marc Márquez", 5 to "Pecco Bagnaia", 6 to "Marchador",
                    7 to "Corredor", 25 to "Maratonista", 8 to "Primera Foto",
                    9 to "Fotógrafo", 10 to "Paparazzi", 11 to "Primer Pedido",
                    12 to "Gourmet", 13 to "Master Chef", 14 to "VIP Access",
                    15 to "Pit Lane", 16 to "Eco Warrior", 17 to "Planeta Verde",
                    18 to "Nocturno", 19 to "Madrugador", 20 to "Bajo la Lluvia",
                    21 to "Leyenda GeoRacing", 22 to "Fiel al Circuito", 23 to "El Primero",
                    24 to "Grupo Legendario"
                )
                
                collectibleNames.forEach { (num, name) ->
                    val id = "c${num.toString().padStart(2, '0')}"
                    MiniButton("🎴 $id - $name") {
                        ScenarioSimulator.unlockCollectible(id)
                        toast("✅ Desbloqueado: $name")
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// TAB: CIRCUIT — Flags, Weather, Traffic, Zones, Hazards
// ═══════════════════════════════════════════════════════════
private fun androidx.compose.foundation.lazy.LazyListScope.circuitTab(toast: (String) -> Unit) {
    // ── Circuit Flags ──
    item { SectionTitle("🏁 Banderas del Circuito", "Simula el estado de la carrera") }

    item {
        FlagGrid(toast)
    }

    // ── Weather ──
    item { SectionTitle("🌤️ Meteorología", "Inyecta datos de clima") }

    item {
        ActionRow(
            left = ActionItem("☀️ Sol 32°C", AccentOrange) {
                ScenarioSimulator.simulateWeather(32f, "Soleado"); toast("✅ Sol 32°C")
            },
            right = ActionItem("🌧️ Lluvia 14°C", AccentBlue) {
                ScenarioSimulator.simulateWeather(14f, "Lluvia"); toast("✅ Lluvia 14°C")
            }
        )
    }
    item {
        ActionRow(
            left = ActionItem("❄️ Frío 3°C", Color(0xFF7DD3FC)) {
                ScenarioSimulator.simulateWeather(3f, "Frío"); toast("✅ Frío 3°C")
            },
            right = ActionItem("🔄 Real", TextDim) {
                ScenarioSimulator.clearWeather(); toast("✅ Clima real")
            }
        )
    }

    // ── Crowd / Traffic ──
    item { SectionTitle("👥 Multitudes y Tráfico", "Aglomeración + hazards Waze") }

    item {
        ActionRow(
            left = ActionItem("Crowd 90%", AccentRed) {
                ScenarioSimulator.triggerCrowdSurge(); toast("✅ Surge activado")
            },
            right = ActionItem("Clear Crowd", AccentGreen) {
                ScenarioSimulator.resetCrowd(); toast("✅ Crowd limpio")
            }
        )
    }
    item {
        ActionRow(
            left = ActionItem("🏎️ Spawn 5 Racers", AccentCyan) {
                ScenarioSimulator.spawnRacers(5); toast("✅ 5 racers spawned")
            },
            right = ActionItem("Clear Racers", TextDim) {
                ScenarioSimulator.clearRacers(); toast("✅ Racers eliminados")
            }
        )
    }

    // ── Hazards ──
    item { SectionTitle("⚠️ Hazards (estilo Waze)", "Peligros en el mapa") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScenarioSimulator.HazardType.entries.forEach { hazard ->
                SmallChipButton(
                    text = "${hazard.emoji} ${hazard.label.take(10)}",
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                ) {
                    ScenarioSimulator.addHazard(hazard); toast("✅ ${hazard.label}")
                }
            }
        }
    }
    item {
        MiniButton("🗑️ Limpiar todos los hazards") {
            ScenarioSimulator.clearHazards(); toast("✅ Hazards eliminados")
        }
    }

    // ── Zone simulation ──
    item { SectionTitle("📍 Zonas del Circuito", "Simula estar en una zona") }

    item {
        var expanded by remember { mutableStateOf(false) }
        ExpandableSection("8 zonas disponibles", expanded) { expanded = !expanded }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(
                    "Tribuna Principal", "Paddock VIP", "Fan Zone",
                    "Curva 1-3", "Recta Principal", "Pit Lane",
                    "Parking Norte", "Hospitality"
                ).forEach { zone ->
                    MiniButton("📍 $zone") {
                        ScenarioSimulator.simulateZoneVisit(zone); toast("✅ Zona: $zone")
                    }
                }
            }
        }
    }

    item {
        ActionRow(
            left = ActionItem("🗺️ Todas las zonas", AccentCyan) {
                ScenarioSimulator.simulateAllZonesVisited(); toast("✅ Todas visitadas")
            },
            right = ActionItem("🔄 Reset zona", TextDim) {
                ScenarioSimulator.resetSimulatedZone(); toast("✅ Zona reseteada")
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// TAB: SOCIAL — Orders, Groups, Alerts, Incidents
// ═══════════════════════════════════════════════════════════
private fun androidx.compose.foundation.lazy.LazyListScope.socialTab(toast: (String) -> Unit) {
    // ── Orders ──
    item { SectionTitle("🛒 Pedidos Click & Collect", "Crea pedidos falsos en todas las fases") }

    item {
        ActionRow(
            left = ActionItem("Pedido Pendiente", AccentOrange) {
                ScenarioSimulator.createFakeOrder("pending"); toast("✅ Pedido pending")
            },
            right = ActionItem("Preparando", Color(0xFFD97706)) {
                ScenarioSimulator.createFakeOrder("preparing"); toast("✅ Pedido preparing")
            }
        )
    }
    item {
        ActionRow(
            left = ActionItem("🔔 Listo!", AccentGreen) {
                ScenarioSimulator.createFakeOrder("ready"); toast("✅ Pedido listo!")
            },
            right = ActionItem("📦 Todos (4)", AccentBlue) {
                ScenarioSimulator.createOrdersAllStatuses(); toast("✅ 4 pedidos creados")
            }
        )
    }

    // ── Groups ──
    item { SectionTitle("👥 Grupos", "Crea un grupo con miembros fake en el mapa") }

    item {
        GlowButton(
            text = "Crear grupo + 3 miembros GPS",
            color = AccentBlue,
            onClick = { ScenarioSimulator.createFakeGroup(); toast("✅ Grupo con 3 miembros") }
        )
    }

    // ── Alerts / News ──
    item { SectionTitle("📰 Alertas / Noticias", "Publica alertas que se ven en la app") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("⚠️ Safety", "SAFETY", AccentRed),
                Triple("🌤️ Meteo", "WEATHER", AccentBlue),
                Triple("🚗 Tráfico", "TRAFFIC", AccentOrange)
            ).forEach { (label, cat, color) ->
                SmallChipButton(label, color, Modifier.weight(1f)) {
                    ScenarioSimulator.createFakeAlert(cat); toast("✅ Alerta $cat")
                }
            }
        }
    }
    item {
        ActionRow(
            left = ActionItem("📅 Horario", AccentPurple) {
                ScenarioSimulator.createFakeAlert("SCHEDULE_CHANGE"); toast("✅ Alerta horario")
            },
            right = ActionItem("📢 General", TextDim) {
                ScenarioSimulator.createFakeAlert("GENERAL"); toast("✅ Alerta general")
            }
        )
    }

    // ── Incidents ──
    item { SectionTitle("🚨 Incidencias", "Simula reportes de incidentes") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("🏥 Médica", "medical", AccentRed),
                Triple("🔥 Fuego", "fire", AccentOrange),
                Triple("👥 Crowd", "crowd", AccentPurple)
            ).forEach { (label, type, color) ->
                SmallChipButton(label, color, Modifier.weight(1f)) {
                    ScenarioSimulator.createFakeIncident(type); toast("✅ Incidente $type")
                }
            }
        }
    }

    // ── Eco ──
    item { SectionTitle("🌱 Eco / Sostenibilidad", "CO₂ ahorrado") }

    item {
        ActionRow(
            left = ActionItem("+1 kg CO₂", AccentGreen) {
                ScenarioSimulator.addCO2Saved(1f); toast("✅ +1 kg CO₂")
            },
            right = ActionItem("+5 kg CO₂", Color(0xFF16A34A)) {
                ScenarioSimulator.addCO2Saved(5f); toast("✅ +5 kg CO₂")
            }
        )
    }

    // ── Parking ──
    item { SectionTitle("🅿️ Parking", "Guardar/borrar posición del coche") }

    item {
        ActionRow(
            left = ActionItem("📍 Guardar coche", AccentBlue) {
                ScenarioSimulator.saveCarLocation(); toast("✅ Coche guardado")
            },
            right = ActionItem("🗑️ Borrar", AccentRed) {
                ScenarioSimulator.clearCarLocation(); toast("✅ Coche borrado")
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// TAB: SYSTEM — Battery, Network, Car, Steps, Speed, BLE
// ═══════════════════════════════════════════════════════════
private fun androidx.compose.foundation.lazy.LazyListScope.systemTab(toast: (String) -> Unit) {
    // ── Battery ──
    item { SectionTitle("🔋 Batería / Energía", "Fuerza nivel de batería") }

    item {
        ActionRow(
            left = ActionItem("Survival 20%", AccentOrange) {
                ScenarioSimulator.simulateSurvivalMode(); toast("✅ Batería 20%")
            },
            right = ActionItem("🔄 Real", TextDim) {
                ScenarioSimulator.resetAll(); toast("✅ Batería real")
            }
        )
    }

    // ── Network ──
    item { SectionTitle("📡 Red / Conectividad", "Simula caída de red") }

    item {
        ActionRow(
            left = ActionItem("💀 Kill Network", Color(0xFF374151)) {
                ScenarioSimulator.killNetwork(); toast("✅ Red muerta (503)")
            },
            right = ActionItem("📡 Restaurar", AccentCyan) {
                ScenarioSimulator.restoreNetwork(); toast("✅ Red ok")
            }
        )
    }

    // ── Car ──
    item { SectionTitle("🚗 Android Auto / Coche", "Simula conexión/desconexión") }

    item {
        ActionRow(
            left = ActionItem("Conectar", AccentCyan) {
                ScenarioSimulator.simulateCarConnect(); toast("✅ Coche conectado")
            },
            right = ActionItem("Desconectar", TextDim) {
                ScenarioSimulator.simulateCarDisconnect(); toast("✅ Coche desconectado")
            }
        )
    }

    // ── Health ──
    item { SectionTitle("🏃 Salud / Pasos", "Inyecta pasos en Health Connect") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("+1K", 1000, AccentBlue),
                Triple("+5K", 5000, AccentCyan),
                Triple("+10K", 10000, AccentPurple),
                Triple("+25K", 25000, Color(0xFF6D28D9))
            ).forEach { (label, steps, color) ->
                SmallChipButton(label, color, Modifier.weight(1f)) {
                    ScenarioSimulator.addFakeSteps(steps); toast("✅ +$steps pasos")
                }
            }
        }
    }

    // ── Speed ──
    item { SectionTitle("⚡ Velocidad GPS", "Simula velocidad de desplazamiento") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("5 km/h", 5f, AccentGreen),
                Triple("30 km/h", 30f, AccentBlue),
                Triple("80 km/h", 80f, AccentOrange),
                Triple("200 km/h", 200f, AccentRed)
            ).forEach { (label, speed, color) ->
                SmallChipButton(label, color, Modifier.weight(1f)) {
                    ScenarioSimulator.setSimulatedSpeed(speed); toast("✅ $label")
                }
            }
        }
    }

    // ── Transport mode ──
    item { SectionTitle("🚶 Modo Transporte", "Fuerza detección de transporte") }

    item {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Triple("🚶 Andando", "walking", AccentGreen),
                Triple("🚗 Coche", "driving", AccentBlue),
                Triple("🚆 Tren", "transit", AccentPurple),
                Triple("🔄 Auto", null, TextDim)
            ).forEach { (label, mode, color) ->
                SmallChipButton(label, color, Modifier.weight(1f)) {
                    ScenarioSimulator.setTransportMode(mode); toast("✅ $label")
                }
            }
        }
    }

    // ── Smart Ticket ──
    item { SectionTitle("🎫 Smart Ticket", "Proximidad a puertas de acceso") }

    item {
        ActionRow(
            left = ActionItem("📍 En puerta", AccentGold) {
                ScenarioSimulator.arriveAtGate(); toast("✅ En puerta de acceso")
            },
            right = ActionItem("🔄 Reset", TextDim) {
                ScenarioSimulator.resetGate(); toast("✅ Puerta reseteada")
            }
        )
    }

    // ── Master Reset ──
    item { Spacer(Modifier.height(8.dp)) }

    item {
        DangerButton("💣 RESET ABSOLUTO — Todo a cero") {
            ScenarioSimulator.resetEverything(); toast("✅ Reset absoluto completado")
        }
    }
}


// ═══════════════════════════════════════════════════════════
// UI COMPONENTS — Premium Design System
// ═══════════════════════════════════════════════════════════

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        Text(title, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextDim, fontSize = 10.sp)
    }
}

@Composable
private fun PresetCard(
    emoji: String,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp)
            }
            Icon(Icons.Filled.PlayArrow, null, tint = TextDim.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

data class ActionItem(val label: String, val color: Color, val onClick: () -> Unit)

@Composable
private fun ActionRow(left: ActionItem, right: ActionItem) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ActionButton(left.label, left.color, Modifier.weight(1f), left.onClick)
        ActionButton(right.label, right.color, Modifier.weight(1f), right.onClick)
    }
}

@Composable
private fun ActionButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmallChipButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(text, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GlowButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.2f),
            contentColor = color
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun MiniButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(34.dp),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, color = TextLight.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DangerButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentRed.copy(alpha = 0.15f),
            contentColor = AccentRed
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun ExpandableSection(title: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCardLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextLight, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            null, tint = TextDim, modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FlagGrid(toast: (String) -> Unit) {
    val flags = listOf(
        Triple("🟢 Green", CircuitMode.GREEN_FLAG, Color(0xFF22C55E)),
        Triple("🟡 Yellow", CircuitMode.YELLOW_FLAG, Color(0xFFEAB308)),
        Triple("🔴 Red", CircuitMode.RED_FLAG, Color(0xFFEF4444)),
        Triple("🏎️ Safety Car", CircuitMode.SAFETY_CAR, Color(0xFFF97316)),
        Triple("💻 VSC", CircuitMode.VSC, Color(0xFF8B5CF6)),
        Triple("🚨 Evacuación", CircuitMode.EVACUATION, Color(0xFFDC2626))
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        flags.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (label, mode, color) ->
                    SmallChipButton(label, color, Modifier.weight(1f)) {
                        ScenarioSimulator.simulateCircuitMode(mode); toast("✅ $label")
                    }
                }
            }
        }
        MiniButton("🔄 Quitar bandera (dato real)") {
            ScenarioSimulator.clearCircuitMode(); toast("✅ Bandera quitada")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// DATA: Achievement list for individual unlock
// ═══════════════════════════════════════════════════════════
private val achievementList = listOf(
    "🗺️ Explorer" to listOf(
        Triple("exp_first_visit", "Primera Visita", "🏁"),
        Triple("exp_all_zones", "Explorador Total", "🗺️"),
        Triple("exp_5km", "Maratoniano", "🏃"),
        Triple("exp_paddock", "Acceso VIP", "⭐")
    ),
    "👥 Social" to listOf(
        Triple("soc_first_group", "En Equipo", "👥"),
        Triple("soc_5_friends", "Escudería Completa", "🏎️"),
        Triple("soc_share_qr", "Conexión Rápida", "📱"),
        Triple("soc_moment_shared", "Fotógrafo", "📸")
    ),
    "⚡ Speed" to listOf(
        Triple("spd_first_nav", "GPS Activado", "🛰️"),
        Triple("spd_arrive_fast", "Pole Position", "⏱️"),
        Triple("spd_find_car", "Memoria de Elefante", "🚗")
    ),
    "🎪 Fan" to listOf(
        Triple("fan_first_order", "Primera Compra", "🛒"),
        Triple("fan_merch", "Coleccionista", "👕"),
        Triple("fan_weather_check", "Meteorólogo", "🌤️"),
        Triple("fan_telemetry", "Ingeniero de Datos", "📊")
    ),
    "🌱 Eco" to listOf(
        Triple("eco_transport", "Movilidad Verde", "🚆"),
        Triple("eco_fountain", "Hidratación Sostenible", "💧")
    ),
    "🛡️ Safety" to listOf(
        Triple("saf_report", "Ciudadano Responsable", "🛡️"),
        Triple("saf_emergency", "Preparado", "🏥"),
        Triple("saf_medical", "Prevención", "🆘")
    )
)

// ── Public DebugButton (used by SettingsScreen) ──────────────────────
@Composable
fun DebugButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFFFF2D55), Color(0xFFBF5AF2)))
        )
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
