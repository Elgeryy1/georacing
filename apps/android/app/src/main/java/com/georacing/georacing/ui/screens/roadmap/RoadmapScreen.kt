package com.georacing.georacing.ui.screens.roadmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    viewModel: RoadmapViewModel = viewModel()
) {
    val roadmapData by viewModel.roadmapData.collectAsState()

    // Map to keep track of expanded states for each category
    // Key: Category Title, Value: Expanded (Boolean)
    // By default, maybe we want the first one expanded? or all? 
    // "que las secciones sean colapsables... para no abrumar". Let's expand all by default or let user choose.
    // Let's default to expanded for better initial visibility.
    var expandedStates by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Initialize states if empty (only once)
    LaunchedEffect(roadmapData) {
        if (expandedStates.isEmpty() && roadmapData.isNotEmpty()) {
            expandedStates = roadmapData.associate { it.title to true }
        }
    }

    Scaffold(
        containerColor = Color(0xFF080810),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "GEO RACING ROADMAP",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF080810)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            roadmapData.forEach { category ->
                stickyHeader {
                    CategoryHeader(
                        category = category,
                        isExpanded = expandedStates[category.title] == true,
                        onToggle = {
                            expandedStates = expandedStates.toMutableMap().apply {
                                put(category.title, !(this[category.title] ?: true))
                            }
                        }
                    )
                }

                if (expandedStates[category.title] == true) {
                    items(category.features) { feature ->
                        FeatureRow(feature)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            // Bottom spacer
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun CategoryHeader(
    category: FeatureCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        color = Color(0xFF080810),
        modifier = Modifier
             // Small padding to separate headers slightly if sticky
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFE8253A).copy(alpha = 0.8f), Color(0xFFE8253A).copy(alpha = 0.2f))
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = category.title,
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun FeatureRow(feature: Feature) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF64748B).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = feature.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Status Indicator
            StatusIndicator(feature.status)
        }
    }
}

@Composable
fun StatusIndicator(status: FeatureStatus) {
    val (icon, color, tooltip) = when (status) {
        FeatureStatus.DONE -> Triple(Icons.Default.CheckCircle, Color(0xFF22C55E), "Hecho")
        FeatureStatus.WIP -> Triple(Icons.Default.Schedule, Color(0xFFFFA726), "En Progreso")
        FeatureStatus.BACKLOG -> Triple(Icons.Default.Lock, Color(0xFF64748B), "Planificado")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tooltip.uppercase().take(4), // Short text
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
