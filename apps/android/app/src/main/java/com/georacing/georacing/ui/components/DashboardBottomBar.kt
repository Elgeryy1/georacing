package com.georacing.georacing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.glass.GlassSupport
import com.georacing.georacing.ui.glass.LiquidBottomTabs
import com.georacing.georacing.ui.glass.LiquidBottomTab
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.glass.LiquidSurface

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun DashboardBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        NavItem("Inicio", Icons.Default.Home, Screen.Home.route),
        NavItem("Mapa", Icons.Default.Map, Screen.Map.route),
        NavItem("Alertas", Icons.Default.Notifications, Screen.Alerts.route),
        NavItem("Tienda", Icons.Default.ShoppingCart, Screen.Orders.route),
        NavItem("Ajustes", Icons.Default.Settings, Screen.Settings.route)
    )
    
    // Find current selected index
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    if (GlassSupport.isEmulator) {
        // Fallback: simple bottom bar without any RuntimeShader/drawBackdrop usage
        FallbackBottomBar(
            items = items,
            selectedIndex = selectedIndex,
            currentRoute = currentRoute,
            navController = navController
        )
    } else {
        // Full Liquid Glass bottom bar (real device only)
        LiquidGlassBottomBar(
            items = items,
            selectedIndex = selectedIndex,
            currentRoute = currentRoute,
            navController = navController
        )
    }
}

@Composable
private fun LiquidGlassBottomBar(
    items: List<NavItem>,
    selectedIndex: Int,
    currentRoute: String?,
    navController: NavController
) {
    val backdrop = LocalBackdrop.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiquidBottomTabs(
            selectedTabIndex = { selectedIndex },
            onTabSelected = { index ->
                val targetRoute = items[index].route
                if (currentRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Home.route) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            },
            backdrop = backdrop,
            tabsCount = items.size,
            modifier = Modifier.fillMaxWidth()
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF8E8E93)
                
                LiquidBottomTab(
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = if (isSelected) 10.sp else 9.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Simple fallback bottom bar for emulators where RuntimeShader crashes.
 * Uses standard Compose drawing without any backdrop/shader effects.
 */
@Composable
private fun FallbackBottomBar(
    items: List<NavItem>,
    selectedIndex: Int,
    currentRoute: String?,
    navController: NavController
) {
    val backdrop = LocalBackdrop.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
    ) {
        LiquidSurface(
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            cornerRadius = 32.dp,
            surfaceColor = com.georacing.georacing.ui.theme.AsphaltGrey.copy(alpha = 0.8f)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val color = if (isSelected) Color(0xFF00F0FF) else Color(0xFF8E8E93)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo(Screen.Home.route) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = color,
                    modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = if (isSelected) 10.sp else 9.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = color
                )
            }
        }
            }
        }
    }
}
