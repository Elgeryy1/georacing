package com.georacing.georacing.ui.screens.clima

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.data.remote.OpenMeteoService
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private data class HourlyForecast(val hour: String, val tempC: Int, val icon: String, val rainProb: Int)
private data class WeatherRecommendation(val icon: ImageVector, val title: String, val description: String, val color: Color, val priority: Int)

/**
 * ClimaSmartScreen — Premium Weather HUD
 * Datos meteorológicos REALES via Open-Meteo API.
 * Circuit de Barcelona-Catalunya: 41.57°N, 2.26°E
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimaSmartScreen(navController: NavController) {
    val backdrop = LocalBackdrop.current

    // Coordenadas reales del Circuit de Barcelona-Catalunya
    val circuitLat = 41.57
    val circuitLon = 2.26

    var currentTemp by remember { mutableIntStateOf(0) }
    var feelsLike by remember { mutableIntStateOf(0) }
    var humidity by remember { mutableIntStateOf(0) }
    var windSpeed by remember { mutableIntStateOf(0) }
    var windDirection by remember { mutableStateOf("") }
    var uvIndex by remember { mutableIntStateOf(0) }
    var pressure by remember { mutableIntStateOf(0) }
    var sunrise by remember { mutableStateOf("--:--") }
    var sunset by remember { mutableStateOf("--:--") }
    var weatherCode by remember { mutableIntStateOf(0) }
    var hourlyForecast by remember { mutableStateOf<List<HourlyForecast>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Staggered entrance
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

    // Fetch real weather data
    LaunchedEffect(Unit) {
        try {
            val response = OpenMeteoService.instance.getForecast(
                latitude = circuitLat,
                longitude = circuitLon
            )

            response.current?.let { c ->
                currentTemp = c.temperature_2m?.toInt() ?: 0
                feelsLike = c.apparent_temperature?.toInt() ?: 0
                humidity = c.relative_humidity_2m ?: 0
                windSpeed = c.wind_speed_10m?.toInt() ?: 0
                windDirection = degreesToCardinal(c.wind_direction_10m ?: 0)
                weatherCode = c.weather_code ?: 0
                pressure = c.surface_pressure?.toInt() ?: 0
            }

            response.daily?.let { d ->
                sunrise = d.sunrise?.firstOrNull()?.takeLast(5) ?: "--:--"
                sunset = d.sunset?.firstOrNull()?.takeLast(5) ?: "--:--"
                uvIndex = d.uv_index_max?.firstOrNull()?.toInt() ?: 0
            }

            response.hourly?.let { h ->
                val times = h.time ?: emptyList()
                val temps = h.temperature_2m ?: emptyList()
                val rain = h.precipitation_probability ?: emptyList()
                val codes = h.weather_code ?: emptyList()

                val currentHour = LocalTime.now().hour
                hourlyForecast = times.indices
                    .filter { i ->
                        val hour = try { times[i].takeLast(5).take(2).toInt() } catch (_: Exception) { -1 }
                        hour >= currentHour
                    }
                    .take(12)
                    .map { i ->
                        HourlyForecast(
                            hour = times.getOrNull(i)?.takeLast(5) ?: "",
                            tempC = temps.getOrNull(i)?.toInt() ?: 0,
                            icon = weatherCodeToEmoji(codes.getOrNull(i) ?: 0),
                            rainProb = rain.getOrNull(i) ?: 0
                        )
                    }
            }

            isLoading = false
        } catch (e: Exception) {
            Log.e("ClimaSmartScreen", "Error fetching weather", e)
            errorMsg = "Error al obtener datos meteorológicos: ${e.message}"
            isLoading = false
        }
    }

    val recommendations = remember(currentTemp, uvIndex, humidity, windSpeed) {
        buildList {
            if (uvIndex >= 6) add(WeatherRecommendation(Icons.Default.WbSunny, "Protección Solar", "UV $uvIndex (Muy Alto). Usa protector solar FPS 50+ y gorra.", Color(0xFFFF9800), 1))
            if (currentTemp >= 28) add(WeatherRecommendation(Icons.Default.WaterDrop, "Hidratación", "Temp. ${currentTemp}°C. Bebe mínimo 500ml/hora. Hay fuentes en el circuito.", ElectricBlue, 1))
            if (windSpeed >= 15) add(WeatherRecommendation(Icons.Default.Air, "Viento Fuerte", "Ráfagas de ${windSpeed} km/h dirección $windDirection. Sujeta bien gorras y pancartas.", NeonCyan, 2))
            if (currentTemp <= 10) add(WeatherRecommendation(Icons.Default.AcUnit, "Frío", "Temperatura baja: ${currentTemp}°C. Lleva ropa de abrigo.", ElectricBlue, 1))
            if (weatherCode in 51..67 || weatherCode in 80..82) add(WeatherRecommendation(Icons.Default.Umbrella, "Lluvia", "Se espera lluvia. Lleva chubasquero o paraguas.", NeonPurple, 1))
            add(WeatherRecommendation(Icons.Default.Visibility, "Visibilidad", "Condiciones actuales: ${weatherCodeToText(weatherCode)}.", StatusGreen, 3))
        }.sortedBy { it.priority }
    }

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("CLIMA", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Tiempo real · Open-Meteo", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                },
                actions = {
                    HomeIconButton { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                }
            )

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Pulsing loading indicator
                            val pulseAnim = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by pulseAnim.animateFloat(
                                initialValue = 0.8f, targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "s"
                            )
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                                    .drawBehind {
                                        drawCircle(NeonCyan.copy(alpha = 0.2f), radius = size.minDimension / 2)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Sincronizando datos reales...", color = TextTertiary, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                    }
                }
                errorMsg != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(StatusRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Warning, null, tint = StatusRed, modifier = Modifier.size(36.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text("Error Meteorológico", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(errorMsg ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(8.dp))

                        // ── Location pill ──
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -20 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(RacingRed.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = RacingRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Circuit de Barcelona-Catalunya · Montmeló", style = MaterialTheme.typography.labelSmall, color = TextSecondary, letterSpacing = 0.5.sp)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Hero Weather Card ── (glass with gradient glow)
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 40 }
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .liquidGlass(RoundedCornerShape(24.dp), GlassLevel.L3, accentGlow = NeonCyan)
                            ) {
                                // Subtle sky gradient overlay
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    ElectricBlue.copy(alpha = 0.08f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        .padding(28.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Animated emoji
                                        val floatAnim = rememberInfiniteTransition(label = "float")
                                        val yOffset by floatAnim.animateFloat(
                                            initialValue = 0f, targetValue = -8f,
                                            animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse), label = "y"
                                        )
                                        Text(
                                            weatherCodeToEmoji(weatherCode), fontSize = 72.sp,
                                            modifier = Modifier.graphicsLayer { translationY = yOffset }
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        // Temperature - large hero text
                                        Text(
                                            "${currentTemp}°",
                                            style = MaterialTheme.typography.displayLarge.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 80.sp,
                                                letterSpacing = (-2).sp
                                            ),
                                            color = TextPrimary
                                        )
                                        Text(
                                            "Sensación térmica ${feelsLike}°C",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )

                                        Spacer(Modifier.height(24.dp))

                                        // Stats row - glass sub-cards
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            PremiumWeatherStat(Icons.Default.WaterDrop, "$humidity%", "Humedad", NeonCyan)
                                            PremiumWeatherStat(Icons.Default.Air, "$windSpeed km/h", "Viento $windDirection", NeonCyan)
                                            PremiumWeatherStat(Icons.Default.WbSunny, "UV $uvIndex", "Índice UV", ChampagneGold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // ── Hourly Forecast Section ──
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(500, 250)) + slideInVertically(tween(500, 250)) { 30 }
                        ) {
                            Column {
                                Text(
                                    "PREVISIÓN HORARIA",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary,
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Datos del circuito en tiempo real",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextTertiary.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (hourlyForecast.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(hourlyForecast) { index, forecast ->
                                    AnimatedVisibility(
                                        visible = showContent,
                                        enter = fadeIn(tween(300, 300 + index * 50)) + slideInVertically(tween(300, 300 + index * 50)) { 20 }
                                    ) {
                                        PremiumHourlyCard(forecast)
                                    }
                                }
                            }
                        } else {
                            Text("Sin datos horarios disponibles", color = TextTertiary, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(28.dp))

                        // ── Recommendations Section ──
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(500, 400)) + slideInVertically(tween(500, 400)) { 30 }
                        ) {
                            Text(
                                "RECOMENDACIONES INTELIGENTES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        recommendations.forEachIndexed { index, rec ->
                            AnimatedVisibility(
                                visible = showContent,
                                enter = fadeIn(tween(400, 500 + index * 80)) + slideInVertically(tween(400, 500 + index * 80)) { 30 }
                            ) {
                                PremiumRecommendationCard(rec)
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Spacer(Modifier.height(24.dp))

                        // ── Bottom Stats Strip ──
                        AnimatedVisibility(
                            visible = showContent,
                            enter = fadeIn(tween(500, 700)) + slideInVertically(tween(500, 700)) { 20 }
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .liquidGlass(RoundedCornerShape(16.dp), GlassLevel.L1)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    PremiumMiniStat("Presión", "${pressure}hPa", NeonCyan)
                                    PremiumMiniStat("UV máx", "$uvIndex", ChampagneGold)
                                    PremiumMiniStat("Amanecer", sunrise, StatusAmber)
                                    PremiumMiniStat("Anochecer", sunset, NeonPurple)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Fuente: Open-Meteo API (datos reales)",
                            fontSize = 10.sp,
                            color = TextTertiary.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(100.dp))
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
private fun PremiumWeatherStat(icon: ImageVector, value: String, label: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary, fontSize = 10.sp)
    }
}

@Composable
private fun PremiumHourlyCard(forecast: HourlyForecast) {
    val isCurrent = try {
        LocalTime.now().hour == LocalTime.parse(forecast.hour, DateTimeFormatter.ofPattern("HH:mm")).hour
    } catch (_: Exception) { false }

    val accentColor = if (isCurrent) ElectricBlue else Color.Transparent

    Box(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L1, accentGlow = accentColor)
    ) {
        // Current hour highlight
        if (isCurrent) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(ElectricBlue.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )
        }
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                if (isCurrent) "AHORA" else forecast.hour,
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrent) ElectricBlue else TextTertiary,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Normal,
                fontSize = if (isCurrent) 9.sp else 11.sp,
                letterSpacing = if (isCurrent) 1.sp else 0.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(forecast.icon, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "${forecast.tempC}°",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            if (forecast.rainProb > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WaterDrop, null, tint = ElectricBlue, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${forecast.rainProb}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumRecommendationCard(rec: WeatherRecommendation) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .liquidGlass(RoundedCornerShape(18.dp), GlassLevel.L2, accentGlow = rec.color)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Icon box with colored glow
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(rec.color.copy(alpha = 0.12f))
                    .drawBehind {
                        drawCircle(rec.color.copy(alpha = 0.08f), radius = size.minDimension * 0.8f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(rec.icon, rec.title, tint = rec.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rec.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (rec.priority == 1) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(rec.color.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "IMPORTANTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = rec.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    rec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun PremiumMiniStat(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            fontSize = 10.sp
        )
    }
}

// ══════════════════════════════════════════════
// ── Helpers ──
// ══════════════════════════════════════════════

private fun degreesToCardinal(degrees: Int): String = when {
    degrees < 23 -> "N"; degrees < 68 -> "NE"; degrees < 113 -> "E"; degrees < 158 -> "SE"
    degrees < 203 -> "S"; degrees < 248 -> "SO"; degrees < 293 -> "O"; degrees < 338 -> "NO"
    else -> "N"
}

private fun weatherCodeToEmoji(code: Int): String = when (code) {
    0 -> "☀️"; 1 -> "🌤️"; 2 -> "⛅"; 3 -> "☁️"
    in 45..48 -> "🌫️"
    in 51..55 -> "🌦️"; in 56..57 -> "🌧️❄️"
    in 61..65 -> "🌧️"; in 66..67 -> "🌧️❄️"
    in 71..77 -> "🌨️"
    in 80..82 -> "🌧️"
    in 85..86 -> "🌨️"
    in 95..99 -> "⛈️"
    else -> "🌡️"
}

private fun weatherCodeToText(code: Int): String = when (code) {
    0 -> "Cielo despejado"; 1 -> "Mayormente despejado"; 2 -> "Parcialmente nublado"; 3 -> "Nublado"
    in 45..48 -> "Niebla"
    in 51..55 -> "Llovizna"; in 56..57 -> "Llovizna helada"
    in 61..65 -> "Lluvia"; in 66..67 -> "Lluvia helada"
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 85..86 -> "Nieve"
    in 95..99 -> "Tormenta"
    else -> "Desconocido"
}
