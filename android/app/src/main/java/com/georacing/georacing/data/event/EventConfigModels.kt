package com.georacing.georacing.data.event

import com.georacing.georacing.domain.model.DashboardLayout
import com.georacing.georacing.domain.model.WidgetType

enum class EventConfigSource {
    DEFAULT,
    CACHE,
    REMOTE
}

enum class EventThemeVariant {
    RACING_RED,
    SUNSET,
    NIGHT,
    ELECTRIC,
    CLASSIC;

    companion object {
        fun from(rawValue: String?): EventThemeVariant {
            return entries.firstOrNull { candidate ->
                candidate.name.equals(rawValue.orEmpty().trim(), ignoreCase = true)
            } ?: RACING_RED
        }
    }
}

data class EventBranding(
    val primaryHex: String = "#E8253A",
    val secondaryHex: String = "#16161E",
    val tertiaryHex: String = "#22C55E",
    val backgroundHex: String = "#080810",
    val surfaceHex: String = "#16161E",
    val onPrimaryHex: String = "#FFFFFF",
    val onBackgroundHex: String = "#F8FAFC",
    val onSurfaceHex: String = "#F8FAFC",
    val outlineHex: String = "#1FFFFFFF",
    val outlineVariantHex: String = "#14FFFFFF",
    val logoUrl: String? = null
)

data class EventFeatureFlags(
    val enableOrders: Boolean = true,
    val enableGroup: Boolean = true,
    val enableParking: Boolean = true,
    val enableMoments: Boolean = true,
    val enableAlerts: Boolean = true,
    val enableRoadmap: Boolean = true,
    val enableWeather: Boolean = true,
    val enableTraffic: Boolean = true
)

data class EventScreenConfig(
    val route: String,
    val label: String? = null,
    val enabled: Boolean = true
)

data class EventMapLayerConfig(
    val key: String,
    val label: String? = null,
    val enabled: Boolean = true
)

data class EventNavigationConfig(
    val allowUserLayoutOverride: Boolean = true,
    val defaultWidgetKeys: List<String> = emptyList(),
    val screens: List<EventScreenConfig> = emptyList(),
    val mapLayers: List<EventMapLayerConfig> = emptyList()
)

data class ActiveEventConfig(
    val eventId: String,
    val slug: String,
    val displayName: String,
    val shortName: String,
    val venueName: String,
    val locationLabel: String,
    val timezone: String,
    val isActive: Boolean,
    val heroTitle: String,
    val heroSubtitle: String,
    val infoBanner: String = "Dynamic event configuration enabled",
    val ctaLabel: String = "Explorar recinto",
    val themeVariant: String? = EventThemeVariant.RACING_RED.name,
    val branding: EventBranding = EventBranding(),
    val featureFlags: EventFeatureFlags = EventFeatureFlags(),
    val navigation: EventNavigationConfig = EventNavigationConfig()
) {
    companion object {
        fun fallback(): ActiveEventConfig = ActiveEventConfig(
            eventId = "default_event",
            slug = "georacing-default",
            displayName = "GeoRacing Live Event",
            shortName = "GeoRacing",
            venueName = "Circuit de Barcelona-Catalunya",
            locationLabel = "Montmelo, Spain",
            timezone = "Europe/Madrid",
            isActive = true,
            heroTitle = "Live event",
            heroSubtitle = "Dynamic event configuration enabled",
            infoBanner = "Mapa, servicios y alertas listos para moverte por el recinto.",
            ctaLabel = "Explorar circuito",
            themeVariant = EventThemeVariant.RACING_RED.name,
            branding = EventBranding(),
            featureFlags = EventFeatureFlags(),
            navigation = EventNavigationConfig(
                allowUserLayoutOverride = true,
                defaultWidgetKeys = DashboardLayout.DEFAULT.widgets.map { it.name },
                screens = defaultScreens(),
                mapLayers = defaultMapLayers()
            )
        )

        private fun defaultScreens(): List<EventScreenConfig> = listOf(
            EventScreenConfig(route = "home", label = "Inicio"),
            EventScreenConfig(route = "map", label = "Mapa"),
            EventScreenConfig(route = "alerts", label = "Alertas"),
            EventScreenConfig(route = "orders", label = "Tienda"),
            EventScreenConfig(route = "settings", label = "Ajustes"),
            EventScreenConfig(route = "parking", label = "Tu Coche"),
            EventScreenConfig(route = "roadmap", label = "Agenda"),
            EventScreenConfig(route = "group", label = "Grupo"),
            EventScreenConfig(route = "incident_report", label = "Avisar"),
            EventScreenConfig(route = "circuit_destinations", label = "A mi puerta"),
            EventScreenConfig(route = "fan_immersive", label = "Live"),
            EventScreenConfig(route = "moments", label = "Momentos")
        )

        private fun defaultMapLayers(): List<EventMapLayerConfig> = listOf(
            EventMapLayerConfig(key = "FOOD", label = "Comida"),
            EventMapLayerConfig(key = "RESTROOM", label = "Banos"),
            EventMapLayerConfig(key = "MERCHANDISE", label = "Merch"),
            EventMapLayerConfig(key = "GATE", label = "Mi Puerta")
        )
    }
}

fun ActiveEventConfig.themeVariantEnum(): EventThemeVariant = EventThemeVariant.from(themeVariant)

data class EventConfigSyncState(
    val isRefreshing: Boolean = false,
    val source: EventConfigSource = EventConfigSource.DEFAULT,
    val lastSuccessfulSyncMs: Long? = null,
    val errorMessage: String? = null
)

fun ActiveEventConfig.screenConfig(route: String): EventScreenConfig? =
    navigation.screens.firstOrNull { it.route.equals(route, ignoreCase = true) }

fun ActiveEventConfig.isRouteEnabled(route: String, default: Boolean = true): Boolean =
    screenConfig(route)?.enabled ?: default

fun ActiveEventConfig.labelForRoute(route: String, fallback: String): String =
    screenConfig(route)?.label?.takeIf { it.isNotBlank() } ?: fallback

fun ActiveEventConfig.mapLayerConfig(key: String): EventMapLayerConfig? =
    navigation.mapLayers.firstOrNull { it.key.equals(key, ignoreCase = true) }

fun ActiveEventConfig.isMapLayerEnabled(key: String, default: Boolean = true): Boolean =
    mapLayerConfig(key)?.enabled ?: default

fun ActiveEventConfig.labelForMapLayer(key: String, fallback: String): String =
    mapLayerConfig(key)?.label?.takeIf { it.isNotBlank() } ?: fallback

fun ActiveEventConfig.resolvedHomeWidgets(
    fallback: List<WidgetType> = DashboardLayout.DEFAULT.widgets
): List<WidgetType> {
    val remoteWidgets = navigation.defaultWidgetKeys
        .mapNotNull { widgetKey ->
            runCatching { WidgetType.valueOf(widgetKey) }.getOrNull()
        }
        .distinct()

    return if (remoteWidgets.isNotEmpty()) remoteWidgets else fallback
}
