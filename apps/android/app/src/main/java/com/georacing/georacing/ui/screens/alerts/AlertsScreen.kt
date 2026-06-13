package com.georacing.georacing.ui.screens.alerts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.domain.model.NewsCategory
import com.georacing.georacing.domain.model.NewsPriority
import com.georacing.georacing.domain.model.RaceNews
import com.georacing.georacing.ui.theme.RacingRed
import com.georacing.georacing.ui.theme.CarbonBlack
import com.georacing.georacing.ui.theme.AsphaltGrey
import com.georacing.georacing.ui.theme.TextSecondary
import com.georacing.georacing.ui.theme.StatusGreen
import com.georacing.georacing.ui.theme.StatusAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf<NewsCategory?>(null) }
    
    // Noticias fake para demo
    val allNews = remember {
        listOf(
            RaceNews(
                id = "1",
                title = "¡La carrera comienza en 30 minutos!",
                content = "Los pilotos se están preparando en la parrilla de salida. Se espera una salida emocionante.",
                timestamp = System.currentTimeMillis() - 5 * 60 * 1000,
                category = NewsCategory.RACE_UPDATE,
                priority = NewsPriority.HIGH
            ),
            RaceNews(
                id = "2",
                title = "Cambio en la programación",
                content = "La sesión de calificación se ha adelantado 15 minutos debido a las condiciones meteorológicas.",
                timestamp = System.currentTimeMillis() - 30 * 60 * 1000,
                category = NewsCategory.SCHEDULE_CHANGE,
                priority = NewsPriority.MEDIUM
            ),
            RaceNews(
                id = "3",
                title = "Condiciones meteorológicas ideales",
                content = "Cielo despejado y temperatura de 24°C. Condiciones perfectas para la carrera de hoy.",
                timestamp = System.currentTimeMillis() - 60 * 60 * 1000,
                category = NewsCategory.WEATHER,
                priority = NewsPriority.LOW
            ),
            RaceNews(
                id = "4",
                title = "Lewis Hamilton habla antes de la carrera",
                content = "El piloto británico se muestra confiado: 'Hemos trabajado mucho en la estrategia y el coche se siente bien'.",
                timestamp = System.currentTimeMillis() - 90 * 60 * 1000,
                category = NewsCategory.DRIVER_NEWS,
                priority = NewsPriority.LOW
            ),
            RaceNews(
                id = "5",
                title = "Acceso recomendado por zona norte",
                content = "Debido al alto tráfico, recomendamos acceder al circuito por las puertas 3 y 4 en la zona norte.",
                timestamp = System.currentTimeMillis() - 120 * 60 * 1000,
                category = NewsCategory.TRAFFIC,
                priority = NewsPriority.MEDIUM
            ),
            RaceNews(
                id = "6",
                title = "Meet & Greet con los pilotos después de la carrera",
                content = "No te pierdas la oportunidad de conocer a tus pilotos favoritos en la zona VIP a las 18:00.",
                timestamp = System.currentTimeMillis() - 180 * 60 * 1000,
                category = NewsCategory.EVENT,
                priority = NewsPriority.LOW
            )
        )
    }
    
    val filteredNews = if (selectedCategory == null) {
        allNews
    } else {
        allNews.filter { it.category == selectedCategory }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))
                )
            )
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            stringResource(R.string.newsletter_title).uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            stringResource(R.string.newsletter_subtitle),
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    com.georacing.georacing.ui.components.HomeIconButton {
                        navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                            popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFFF8FAFC),
                    navigationIconContentColor = Color(0xFFF8FAFC)
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filtros de categoría
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { 
                            Text(
                                stringResource(R.string.news_category_all),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8253A),
                            selectedLabelColor = Color(0xFFF8FAFC),
                            containerColor = Color(0xFF14141C),
                            labelColor = Color(0xFFF8FAFC)
                        )
                    )
                }
                
                items(NewsCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { 
                            Text(
                                category.displayName,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8253A),
                            selectedLabelColor = Color(0xFFF8FAFC),
                            containerColor = Color(0xFF14141C),
                            labelColor = Color(0xFFF8FAFC)
                        )
                    )
                }
            }
            
            HorizontalDivider(color = Color(0xFF14141C), thickness = 1.dp)
            
            // Lista de noticias
            if (filteredNews.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFE8253A).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFFE8253A)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.news_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFF8FAFC),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.news_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(filteredNews) { news ->
                        NewsCard(news = news)
                    }
                }
            }
        }
    }
    } // Close Box
}

@Composable
fun NewsCard(news: RaceNews) {
    val priorityColor = when (news.priority) {
        NewsPriority.HIGH -> RacingRed
        NewsPriority.MEDIUM -> StatusAmber
        NewsPriority.LOW -> StatusGreen
    }
    
    val timeText = getTimeAgo(news.timestamp)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14141C)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            // Indicador de prioridad
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
                    .align(Alignment.Top)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Categoría y tiempo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = news.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Título
                Text(
                    text = news.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Contenido
                Text(
                    text = news.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 20.sp
                )
            }
        }
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
