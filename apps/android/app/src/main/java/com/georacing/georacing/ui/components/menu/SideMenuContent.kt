package com.georacing.georacing.ui.components.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.georacing.georacing.domain.features.FeatureCategory
import com.georacing.georacing.domain.features.FeatureRegistry
import com.georacing.georacing.domain.features.FeatureStatus
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*

/**
 * SideMenuContent — iOS parity for SideMenuView.swift
 *
 * Shows:
 * - Quick navigation links (tabs)
 * - FeatureRegistry categories with expandable sections
 * - Completion counters per category
 */
@Composable
fun SideMenuContent(
    navController: NavController,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Logo + Title ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Flag, null, tint = TextPrimary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "GeoRacing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    "Driver Menu",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MetalGrey.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))

        // ── Quick Nav (tabs) ──
        val quickLinks = listOf(
            Triple("Inicio", Icons.Default.Home, Screen.Home.route),
            Triple("Mapa", Icons.Default.Map, Screen.Map.route),
            Triple("Alertas", Icons.Default.Notifications, Screen.Alerts.route),
            Triple("Tienda", Icons.Default.ShoppingCart, Screen.Orders.route),
            Triple("Ajustes", Icons.Default.Settings, Screen.Settings.route)
        )

        quickLinks.forEach { (text, icon, route) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onClose()
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = MetalGrey, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MetalGrey.copy(alpha = 0.3f))
        Spacer(Modifier.height(12.dp))

        // ── Feature Categories ──
        Text(
            "FUNCIONES GEORACING",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(Modifier.height(8.dp))

        FeatureCategory.entries.forEach { category ->
            val features = FeatureRegistry.features(category)
            if (features.isNotEmpty()) {
                FeatureCategorySection(
                    category = category,
                    onFeatureSelected = { feature ->
                        onClose()
                        feature.route?.let { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Version ──
        Text(
            "v1.0.0 (Parity Build)",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FeatureCategorySection(
    category: FeatureCategory,
    onFeatureSelected: (com.georacing.georacing.domain.features.Feature) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chevron"
    )

    val features = FeatureRegistry.features(category)
    val completedCount = FeatureRegistry.completedCount(category)
    val totalCount = FeatureRegistry.totalCount(category)

    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        // ── Category Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = category.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                category.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            // Completion counter
            Text(
                "$completedCount/$totalCount",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = TextTertiary,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }

        // ── Expandable Features ──
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(start = 20.dp)) {
                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = feature.route != null) {
                                onFeatureSelected(feature)
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            feature.icon,
                            null,
                            tint = if (feature.route != null) TextSecondary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            feature.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (feature.route != null) TextPrimary else TextTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        // Status dot (only if not complete)
                        if (feature.status != FeatureStatus.COMPLETE) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(feature.status.color)
                            )
                        }
                    }
                }
            }
        }
    }
}
