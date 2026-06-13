package com.georacing.georacing.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class WidgetType(
    val displayName: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    CONTEXTUAL_CARD(
        displayName = "Circuito Live",
        icon = Icons.Default.Info,
        accentColor = Color(0xFF22C55E)
    ),
    STATUS_CARD(
        displayName = "Estado",
        icon = Icons.Default.Dashboard,
        accentColor = Color(0xFF3B82F6)
    ),
    ACTIONS_GRID(
        displayName = "Acciones Rápidas",
        icon = Icons.Default.Apps,
        accentColor = Color(0xFFE8253A)
    ),
    ECO_METER(
        displayName = "Eco Meter",
        icon = Icons.Default.Eco,
        accentColor = Color(0xFF22C55E)
    ),
    NEWS_FEED(
        displayName = "Noticias",
        icon = Icons.Default.Newspaper,
        accentColor = Color(0xFF3B82F6)
    ),
    STAFF_ACTIONS(
        displayName = "Acciones Staff",
        icon = Icons.Default.AdminPanelSettings,
        accentColor = Color(0xFFA855F7)
    ),
    PARKING_INFO(
        displayName = "Parking",
        icon = Icons.Default.LocalParking,
        accentColor = Color(0xFF8B8B97)
    ),
    METEOROLOGY(
        displayName = "Meteorología",
        icon = Icons.Default.Cloud,
        accentColor = Color(0xFF00E5FF)
    ),
    AR_ACCESS(
        displayName = "Realidad Aumentada",
        icon = Icons.Default.ViewInAr,
        accentColor = Color(0xFFA855F7)
    ),
    FIND_RESTROOMS(
        displayName = "Aseos",
        icon = Icons.Default.Wc,
        accentColor = Color(0xFF64748B)
    ),
    FOOD_OFFERS(
        displayName = "Ofertas de Comida",
        icon = Icons.Default.Fastfood,
        accentColor = Color(0xFFFF9F0A)
    ),
    ACHIEVEMENTS(
        displayName = "Logros",
        icon = Icons.Default.EmojiEvents,
        accentColor = Color(0xFFD4A855)
    ),
    SEARCH_ACCESS(
        displayName = "Buscar",
        icon = Icons.Default.Search,
        accentColor = Color(0xFFCBD5E1)
    ),
    CLICK_COLLECT(
        displayName = "Click & Collect",
        icon = Icons.Default.ShoppingCart,
        accentColor = Color(0xFFFF6B2C)
    ),
    WRAPPED(
        displayName = "Resumen",
        icon = Icons.Default.Assessment,
        accentColor = Color(0xFFEC4899)
    ),
    COLLECTIBLES(
        displayName = "Coleccionables",
        icon = Icons.Default.Collections,
        accentColor = Color(0xFFA855F7)
    ),
    PROXIMITY_CHAT(
        displayName = "Chat Cercano",
        icon = Icons.Default.ChatBubble,
        accentColor = Color(0xFF00E5FF)
    )
}

data class DashboardLayout(
    val widgets: List<WidgetType>
) {
    companion object {
        val DEFAULT = DashboardLayout(
            listOf(
                WidgetType.METEOROLOGY,
                WidgetType.STATUS_CARD,
                WidgetType.ACTIONS_GRID,
                WidgetType.ACHIEVEMENTS,
                WidgetType.CLICK_COLLECT,
                WidgetType.NEWS_FEED,
                WidgetType.PROXIMITY_CHAT,
                WidgetType.PARKING_INFO
            )
        )
    }
}
