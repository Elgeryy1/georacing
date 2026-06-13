package com.georacing.georacing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.georacing.georacing.data.event.isRouteEnabled
import com.georacing.georacing.data.event.labelForRoute
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.glass.LiquidBottomTabs
import com.georacing.georacing.ui.glass.LiquidBottomTab
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.theme.LocalActiveEventConfig
import com.georacing.georacing.ui.theme.LocalEventVisualStyle

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun DashboardBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current

    val items = buildList {
        add(NavItem(activeEvent.labelForRoute(Screen.Home.route, "Inicio"), Icons.Default.Home, Screen.Home.route))
        if (activeEvent.isRouteEnabled(Screen.Map.route)) {
            add(NavItem(activeEvent.labelForRoute(Screen.Map.route, "Mapa"), Icons.Default.Map, Screen.Map.route))
        }
        if (activeEvent.isRouteEnabled(Screen.Alerts.route, default = activeEvent.featureFlags.enableAlerts)) {
            add(NavItem(activeEvent.labelForRoute(Screen.Alerts.route, "Alertas"), Icons.Default.Notifications, Screen.Alerts.route))
        }
        if (activeEvent.isRouteEnabled(Screen.Orders.route, default = activeEvent.featureFlags.enableOrders)) {
            add(NavItem(activeEvent.labelForRoute(Screen.Orders.route, "Tienda"), Icons.Default.ShoppingCart, Screen.Orders.route))
        }
        add(NavItem(activeEvent.labelForRoute(Screen.Settings.route, "Ajustes"), Icons.Default.Settings, Screen.Settings.route))
    }
    
    // Find current selected index — keep last valid tab when navigating to non-tab screens
    val rawIndex = items.indexOfFirst { it.route == currentRoute }
    var rememberedIndex by remember { mutableIntStateOf(0) }
    if (rawIndex >= 0) rememberedIndex = rawIndex
    val selectedIndex = rememberedIndex
    
    // Get backdrop from CompositionLocal
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
                val color = if (isSelected) visuals.navSelected else visuals.navIdle
                val label = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.NIGHT) {
                    item.label
                } else {
                    item.label.uppercase()
                }
                
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
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = if (isSelected) 10.sp else 9.sp,
                            letterSpacing = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.NIGHT) {
                                0.1.sp
                            } else {
                                0.8.sp
                            }
                        ),
                        color = color
                    )
                }
            }
        }
    }
}
