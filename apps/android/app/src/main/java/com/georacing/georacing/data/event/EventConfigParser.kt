package com.georacing.georacing.data.event

import com.google.gson.Gson

internal object EventConfigParser {
    private val gson = Gson()

    val candidateTables = listOf(
        "active_event_config",
        "event_runtime_config",
        "event_config",
        "events"
    )

    fun parseActiveEvent(
        rows: List<Map<String, Any?>>,
        preferredEventId: String? = null
    ): ActiveEventConfig? {
        val selectedRow = selectActiveRow(rows, preferredEventId) ?: return null
        return parseRow(selectedRow)
    }

    fun parseRow(row: Map<String, Any?>): ActiveEventConfig {
        val brandingMap = row.nestedMap("branding", "theme", "palette")
        val featuresMap = row.nestedMap("feature_flags", "features")
        val navigationMap = row.nestedMap("navigation", "runtime", "app_config")

        val eventId = row.string("event_id", "id", "slug", "code")
            ?: ActiveEventConfig.fallback().eventId
        val slug = row.string("slug", "event_slug", "event_code", "id") ?: eventId
        val displayName = row.string("display_name", "name", "title")
            ?: ActiveEventConfig.fallback().displayName
        val shortName = row.string("short_name", "shortLabel", "short_title") ?: displayName
        val venueName = row.string("venue_name", "venue", "circuit_name", "circuit")
            ?: ActiveEventConfig.fallback().venueName
        val city = row.string("city", "location_city")
        val country = row.string("country", "location_country")
        val locationLabel = row.string("location_label", "location", "location_name")
            ?: listOfNotNull(city, country).joinToString(", ").ifBlank {
                ActiveEventConfig.fallback().locationLabel
            }
        val timezone = row.string("timezone", "tz") ?: ActiveEventConfig.fallback().timezone
        val isActive = row.bool("is_active", "active", "enabled")
            ?: row.string("status", "event_status")?.equals("active", ignoreCase = true)
            ?: true
        val heroTitle = row.string("hero_title", "headline", "title", "display_name") ?: displayName
        val heroSubtitle = row.string("hero_subtitle", "tagline", "subtitle", "description")
            ?: ActiveEventConfig.fallback().heroSubtitle
        val infoBanner = row.string("info_banner", "banner_text", "banner", "info_message")
            ?: ActiveEventConfig.fallback().infoBanner
        val ctaLabel = row.string("cta_label", "cta", "primary_cta")
            ?: ActiveEventConfig.fallback().ctaLabel
        val themeVariant = row.string("theme_variant", "themeVariant")
            ?: brandingMap.string("theme_variant", "themeVariant")
            ?: ActiveEventConfig.fallback().themeVariant

        fun brandingValue(defaultValue: String, vararg keys: String): String {
            return brandingMap.string(*keys)
                ?: row.string(*keys)
                ?: defaultValue
        }

        fun featureValue(defaultValue: Boolean, vararg keys: String): Boolean {
            return featuresMap.bool(*keys)
                ?: row.bool(*keys)
                ?: defaultValue
        }

        val defaultConfig = ActiveEventConfig.fallback()
        val defaultNavigation = defaultConfig.navigation
        val widgetKeys = parseStringList(
            navigationMap?.findValue("default_widget_keys", "widgets", "widgets_csv", "home_widgets")
                ?: row.findValue("default_widget_keys", "widgets", "widgets_csv", "home_widgets")
        ).ifEmpty { defaultNavigation.defaultWidgetKeys }
        val screens = parseScreenConfigs(
            navigationMap?.findValue("screens", "screens_json")
                ?: row.findValue("screens", "screens_json")
        ).ifEmpty { defaultNavigation.screens }
        val mapLayers = parseMapLayers(
            navigationMap?.findValue("map_layers", "map_layers_json", "map_categories", "map_categories_json")
                ?: row.findValue("map_layers", "map_layers_json", "map_categories", "map_categories_json")
        ).ifEmpty { defaultNavigation.mapLayers }
        val allowUserLayoutOverride = navigationMap.bool(
            "allow_user_layout_override",
            "allow_layout_override"
        ) ?: row.bool(
            "allow_user_layout_override",
            "allow_layout_override"
        ) ?: defaultNavigation.allowUserLayoutOverride

        return ActiveEventConfig(
            eventId = eventId,
            slug = slug,
            displayName = displayName,
            shortName = shortName,
            venueName = venueName,
            locationLabel = locationLabel,
            timezone = timezone,
            isActive = isActive,
            heroTitle = heroTitle,
            heroSubtitle = heroSubtitle,
            infoBanner = infoBanner,
            ctaLabel = ctaLabel,
            themeVariant = themeVariant,
            branding = EventBranding(
                primaryHex = brandingValue(
                    defaultConfig.branding.primaryHex,
                    "primary_color",
                    "primaryHex",
                    "branding_primary"
                ),
                secondaryHex = brandingValue(
                    defaultConfig.branding.secondaryHex,
                    "secondary_color",
                    "secondaryHex",
                    "branding_secondary"
                ),
                tertiaryHex = brandingValue(
                    defaultConfig.branding.tertiaryHex,
                    "accent_color",
                    "accentHex",
                    "tertiary_color",
                    "tertiaryHex",
                    "branding_tertiary"
                ),
                backgroundHex = brandingValue(
                    defaultConfig.branding.backgroundHex,
                    "background_color",
                    "backgroundHex",
                    "branding_background"
                ),
                surfaceHex = brandingValue(
                    defaultConfig.branding.surfaceHex,
                    "surface_color",
                    "surfaceHex",
                    "branding_surface"
                ),
                onPrimaryHex = brandingValue(
                    defaultConfig.branding.onPrimaryHex,
                    "on_primary_color",
                    "onPrimaryHex"
                ),
                onBackgroundHex = brandingValue(
                    defaultConfig.branding.onBackgroundHex,
                    "on_background_color",
                    "onBackgroundHex"
                ),
                onSurfaceHex = brandingValue(
                    defaultConfig.branding.onSurfaceHex,
                    "on_surface_color",
                    "onSurfaceHex"
                ),
                outlineHex = brandingValue(
                    defaultConfig.branding.outlineHex,
                    "outline_color",
                    "outlineHex"
                ),
                outlineVariantHex = brandingValue(
                    defaultConfig.branding.outlineVariantHex,
                    "outline_variant_color",
                    "outlineVariantHex"
                ),
                logoUrl = brandingMap.string("logo_url", "logoUrl") ?: row.string("logo_url", "logoUrl")
            ),
            featureFlags = EventFeatureFlags(
                enableOrders = featureValue(true, "enable_orders", "orders_enabled", "show_orders"),
                enableGroup = featureValue(true, "enable_group", "group_enabled", "show_group"),
                enableParking = featureValue(true, "enable_parking", "parking_enabled", "show_parking"),
                enableMoments = featureValue(true, "enable_moments", "moments_enabled", "show_moments"),
                enableAlerts = featureValue(true, "enable_alerts", "alerts_enabled", "show_alerts"),
                enableRoadmap = featureValue(true, "enable_roadmap", "roadmap_enabled", "show_roadmap"),
                enableWeather = featureValue(true, "enable_weather", "weather_enabled", "show_weather"),
                enableTraffic = featureValue(true, "enable_traffic", "traffic_enabled", "show_traffic")
            ),
            navigation = EventNavigationConfig(
                allowUserLayoutOverride = allowUserLayoutOverride,
                defaultWidgetKeys = widgetKeys,
                screens = screens,
                mapLayers = mapLayers
            )
        )
    }

    private fun selectActiveRow(
        rows: List<Map<String, Any?>>,
        preferredEventId: String?
    ): Map<String, Any?>? {
        if (rows.isEmpty()) {
            return null
        }

        val activeRow = rows.firstOrNull { row ->
            row.bool("is_active", "active", "enabled") == true ||
                row.string("status", "event_status")?.equals("active", ignoreCase = true) == true
        }
        if (activeRow != null) {
            return activeRow
        }

        if (!preferredEventId.isNullOrBlank()) {
            val preferredRow = rows.firstOrNull { row ->
                row.string("event_id", "id", "slug", "code")
                    ?.equals(preferredEventId, ignoreCase = true) == true
            }
            if (preferredRow != null) {
                return preferredRow
            }
        }

        return rows.firstOrNull()
    }

    private fun Map<String, Any?>.findValue(vararg keys: String): Any? {
        for (candidateKey in keys) {
            val entry = entries.firstOrNull { it.key.equals(candidateKey, ignoreCase = true) } ?: continue
            return entry.value
        }
        return null
    }

    private fun Map<String, Any?>?.string(vararg keys: String): String? {
        val rawValue = this?.findValue(*keys) ?: return null
        val normalized = rawValue.toString().trim()
        return normalized.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    }

    private fun Map<String, Any?>?.bool(vararg keys: String): Boolean? {
        return when (val rawValue = this?.findValue(*keys)) {
            is Boolean -> rawValue
            is Number -> rawValue.toInt() != 0
            is String -> when (rawValue.trim().lowercase()) {
                "1", "true", "yes", "active", "enabled", "on" -> true
                "0", "false", "no", "inactive", "disabled", "off" -> false
                else -> null
            }

            else -> null
        }
    }

    private fun parseStringList(rawValue: Any?): List<String> {
        return when (rawValue) {
            is List<*> -> rawValue.mapNotNull { value ->
                value?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            }

            is String -> {
                val trimmed = rawValue.trim()
                when {
                    trimmed.isEmpty() -> emptyList()
                    trimmed.startsWith("[") -> {
                        runCatching {
                            gson.fromJson(trimmed, List::class.java)
                                ?.mapNotNull { value -> value?.toString()?.trim()?.takeIf(String::isNotEmpty) }
                                ?: emptyList()
                        }.getOrElse { emptyList() }
                    }

                    trimmed.contains(",") -> trimmed.split(",")
                        .mapNotNull { value -> value.trim().takeIf { it.isNotEmpty() } }

                    else -> listOf(trimmed)
                }
            }

            else -> emptyList()
        }
    }

    private fun parseScreenConfigs(rawValue: Any?): List<EventScreenConfig> {
        val mappedConfigs = parseObjectList(rawValue).mapNotNull { item ->
            val route = item.string("route", "screen_key", "id") ?: return@mapNotNull null
            EventScreenConfig(
                route = route,
                label = item.string("label", "title", "name"),
                enabled = item.bool("enabled", "is_enabled", "visible", "is_visible") ?: true
            )
        }
        if (mappedConfigs.isNotEmpty()) {
            return mappedConfigs
        }

        return parseStringList(rawValue).map { route ->
            EventScreenConfig(route = route)
        }
    }

    private fun parseMapLayers(rawValue: Any?): List<EventMapLayerConfig> {
        val mappedConfigs = parseObjectList(rawValue).mapNotNull { item ->
            val key = item.string("key", "type", "id") ?: return@mapNotNull null
            EventMapLayerConfig(
                key = key,
                label = item.string("label", "title", "name"),
                enabled = item.bool("enabled", "is_enabled", "visible", "is_visible") ?: true
            )
        }
        if (mappedConfigs.isNotEmpty()) {
            return mappedConfigs
        }

        return parseStringList(rawValue).map { key ->
            EventMapLayerConfig(key = key)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseObjectList(rawValue: Any?): List<Map<String, Any?>> {
        return when (rawValue) {
            is List<*> -> rawValue.mapNotNull { normalizeMap(it) }
            is String -> {
                val trimmed = rawValue.trim()
                if (!trimmed.startsWith("[")) {
                    emptyList()
                } else {
                    runCatching {
                        ((gson.fromJson(trimmed, List::class.java) as? List<*>)?.mapNotNull { normalizeMap(it) })
                            ?: emptyList()
                    }.getOrElse { emptyList() }
                }
            }

            else -> emptyList()
        }
    }

    private fun normalizeMap(rawValue: Any?): Map<String, Any?>? {
        return when (rawValue) {
            is Map<*, *> -> rawValue.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }

            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.nestedMap(vararg keys: String): Map<String, Any?>? {
        return when (val rawValue = findValue(*keys)) {
            is Map<*, *> -> rawValue.entries
                .filter { it.key is String }
                .associate { it.key as String to it.value }

            is String -> {
                val trimmed = rawValue.trim()
                if (!trimmed.startsWith("{")) {
                    null
                } else {
                    val decoded = gson.fromJson(trimmed, Map::class.java) as? Map<*, *> ?: return null
                    decoded.entries
                        .filter { it.key is String }
                        .associate { it.key as String to it.value }
                }
            }

            else -> null
        }
    }
}
