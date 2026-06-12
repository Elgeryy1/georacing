package com.georacing.georacing.data.map

import org.maplibre.android.geometry.LatLng

/**
 * Configuración centralizada para MapLibre
 * Versión unificada (data + ui merged)
 */
object MapLibreConfig {
    
    /**
     * Estilo híbrido ortofoto de Catalunya (perfecto para el circuito).
     * Fuente gratuita del ICGC.
     */
    const val SATELLITE_STYLE_URL = "https://geoserveis.icgc.cat/contextmaps/icgc_orto_hibrida.json"
    const val DEFAULT_STYLE_URL = "https://demotiles.maplibre.org/style.json"
    const val FALLBACK_STYLE_URL = DEFAULT_STYLE_URL

    /**
     * Assets
     */
    const val OFFLINE_STYLE_ASSET = "circuit_catalunya_style.json"
    const val ROUTES_GEOJSON_ASSET = "circuit_routes.geojson"
    const val POIS_GEOJSON_ASSET = "circuit_pois.geojson"

    const val POI_LAYER_ID = "circuit_pois_layer"
    const val POI_SOURCE_ID = "circuit_pois_source"
    
    const val POI_COLOR_DEFAULT = "#6C5CE7"
    const val POI_COLOR_WC = "#1E90FF"
    const val POI_COLOR_FOOD = "#E17055"
    const val POI_COLOR_MERCH = "#FDCB6E"
    const val POI_COLOR_INFO = "#00B894"
    const val POI_RADIUS = 6f

    const val ROUTES_VEHICLE_COLOR = "#FF3B30"
    const val ROUTES_WALK_COLOR = "#4CAF50"
    const val ROUTES_WIDTH = 3.5f

    /**
     * Estilo base a usar.
     */
    const val MAP_STYLE_URL = SATELLITE_STYLE_URL
    
    /**
     * Coordenadas del Circuit de Barcelona-Catalunya
     */
    object CircuitBarcelona {
        const val LATITUDE = 41.5700
        const val LONGITUDE = 2.2611
        const val DEFAULT_ZOOM = 15.5
        
        fun getLatLng() = LatLng(LATITUDE, LONGITUDE)
        fun toLatLng() = LatLng(LATITUDE, LONGITUDE) // Alias for backward compatibility
    }

    /**
     * Configuración de marcadores
     */
    object Markers {
        const val DEFAULT_ICON_SIZE = 1.0f
        const val MEMBER_MARKER_COLOR = "#E63946" // Rojo racing
        const val MY_LOCATION_MARKER_COLOR = "#2A9D8F" // Verde azulado
        
        // Zoom mínimo para mostrar marcadores
        const val MIN_ZOOM_TO_SHOW = 12.0
    }
    
    /**
     * Configuración de actualización de ubicaciones
     */
    object Updates {
        const val LOCATION_UPDATE_INTERVAL_MS = 10_000L
        const val MIN_DISTANCE_FOR_UPDATE_METERS = 10f
    }
}
