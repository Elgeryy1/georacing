package com.georacing.georacing.car

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.georacing.georacing.R
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.expressions.Expression

/**
 * Manages map styles (Day/Night) and ensures custom layers (Route, Car, POIs)
 * are correctly re-applied when styles change.
 */
class MapStyleManager(private val context: Context) {

    // Helper to construct map style JSONs - estilo similar a Google Maps sin POIs comerciales
    private fun buildGoogleMapStyleJson(isDark: Boolean): String {
        // Usamos CARTO Positron/Dark Matter - estilo muy similar a Google Maps pero sin POIs comerciales
        val tileUrl = if (isDark) {
            "https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
        } else {
            "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"
        }
        
        return """
        {
          "version": 8,
          "sources": {
            "carto-tiles": {
              "type": "raster",
              "tiles": ["$tileUrl"],
              "tileSize": 256,
              "attribution": "© CARTO, © OpenStreetMap"
            }
          },
          "layers": [
            {
              "id": "carto-tiles",
              "type": "raster",
              "source": "carto-tiles",
              "minzoom": 0,
              "maxzoom": 20
            }
          ]
        }
        """.trimIndent()
    }

    // Map Styles - estilo Google Maps sin POIs comerciales
    val STYLE_DAY_JSON = buildGoogleMapStyleJson(false) // Positron claro (similar Google Maps)
    val STYLE_NIGHT_JSON = buildGoogleMapStyleJson(true) // Dark Matter oscuro

    // Esri World Imagery (Satellite)
    val STYLE_SATELLITE_JSON = """
    {
      "version": 8,
      "sources": {
        "esri-satellite": {
          "type": "raster",
          "tiles": [
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
          ],
          "tileSize": 256,
          "attribution": "© Esri, Maxar, Earthstar Geographics, and the GIS User Community"
        }
      },
      "layers": [
        {
          "id": "esri-satellite",
          "type": "raster",
          "source": "esri-satellite",
          "minzoom": 0,
          "maxzoom": 20
        }
      ]
    }
    """.trimIndent()
    
    // Properties to access styles
    val STYLE_DAY = ""   // Placeholder for logic usage (External references should use JSON)
    val STYLE_NIGHT = "" // Placeholder

    // Source & Layer IDs
    companion object {
        const val SOURCE_ROUTE = "route-source"
        const val LAYER_ROUTE = "route-layer"
        const val SOURCE_CAR = "car-source"
        const val LAYER_CAR = "car-layer"
        const val SOURCE_POIS = "poi-source"
        const val LAYER_POIS = "poi-layer"
        
        const val IMAGE_CAR = "img-car-f1"
        const val IMAGE_PARKING = "img-parking"
        const val IMAGE_GATE = "img-gate"
        
        // Hazard Icons for Waze-style
        const val IMAGE_HAZARD_POLICE = "img-hazard-police"
        const val IMAGE_HAZARD_CONSTRUCTION = "img-hazard-construction"
        const val IMAGE_HAZARD_TRAFFIC = "img-hazard-traffic"
        const val IMAGE_RACER = "img-racer"
        
        // Hazard Source/Layer
        const val SOURCE_HAZARDS = "hazards-source"
        const val LAYER_HAZARDS = "hazards-layer"
        const val SOURCE_RACERS = "racers-source"
        const val LAYER_RACERS = "racers-layer"
    }
    
    // =============================================
    // WAZE CARTOON STYLE (Purple Roads, Wide Lines)
    // =============================================
    fun buildWazeStyleJson(): String {
        // Custom cartoon style with saturated colors
        return """
        {
          "version": 8,
          "name": "GeoRacing Waze Style",
          "sources": {
            "carto-tiles": {
              "type": "raster",
              "tiles": ["https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png"],
              "tileSize": 256,
              "attribution": "© CARTO, © OpenStreetMap"
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "#F0F4F8"
              }
            },
            {
              "id": "carto-tiles",
              "type": "raster",
              "source": "carto-tiles",
              "minzoom": 0,
              "maxzoom": 20,
              "paint": {
                "raster-saturation": 0.3,
                "raster-brightness-max": 1.0
              }
            }
          ]
        }
        """.trimIndent()
    }
    
    /**
     * Applies Waze-style cartoon layers with purple routes
     */
    fun applyWazeLayers(style: Style) {
        addImages(style)
        addHazardImages(style)

        // Route Line (Purple/Magenta - Waze style, WIDER)
        if (style.getSource(SOURCE_ROUTE) == null) {
            style.addSource(GeoJsonSource(SOURCE_ROUTE))
        }
        if (style.getLayer(LAYER_ROUTE) == null) {
            val routeLayer = LineLayer(LAYER_ROUTE, SOURCE_ROUTE).withProperties(
                PropertyFactory.lineColor("#C07AF0"), // Purple/Magenta
                PropertyFactory.lineWidth(12f), // MUCH WIDER for car legibility
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(0.9f)
            )
            style.addLayer(routeLayer)
        }

        // Hazards Layer
        if (style.getSource(SOURCE_HAZARDS) == null) {
            style.addSource(GeoJsonSource(SOURCE_HAZARDS))
        }
        if (style.getLayer(LAYER_HAZARDS) == null) {
            val hazardLayer = SymbolLayer(LAYER_HAZARDS, SOURCE_HAZARDS).withProperties(
                PropertyFactory.iconImage(Expression.get("icon_image")),
                PropertyFactory.iconSize(1.5f), // LARGE for visibility
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textOffset(arrayOf(0f, 2.0f)),
                PropertyFactory.textSize(16f), // LARGE text
                PropertyFactory.textColor("#FFFFFF"),
                PropertyFactory.textHaloColor("#000000"),
                PropertyFactory.textHaloWidth(2f)
            )
            style.addLayer(hazardLayer)
        }
        
        // Other Racers Layer
        if (style.getSource(SOURCE_RACERS) == null) {
            style.addSource(GeoJsonSource(SOURCE_RACERS))
        }
        if (style.getLayer(LAYER_RACERS) == null) {
            val racerLayer = SymbolLayer(LAYER_RACERS, SOURCE_RACERS).withProperties(
                PropertyFactory.iconImage(IMAGE_RACER),
                PropertyFactory.iconSize(0.8f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconRotate(Expression.get("bearing")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
            )
            style.addLayer(racerLayer)
        }
        
        // Car Layer (on top)
        if (style.getSource(SOURCE_CAR) == null) {
            style.addSource(GeoJsonSource(SOURCE_CAR))
        }
        if (style.getLayer(LAYER_CAR) == null) {
            val carLayer = SymbolLayer(LAYER_CAR, SOURCE_CAR).withProperties(
                PropertyFactory.iconImage(IMAGE_CAR),
                PropertyFactory.iconSize(0.8f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconRotate(Expression.get("bearing")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
            )
            style.addLayer(carLayer)
        }
    }
    
    private fun addHazardImages(style: Style) {
        fun drawableToBitmap(id: Int): android.graphics.Bitmap {
            val drawable = ContextCompat.getDrawable(context, id)!!
            val bitmap = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        
        try {
            // Reusing F1 car as placeholder for all icons
            style.addImage(IMAGE_HAZARD_POLICE, drawableToBitmap(R.drawable.ic_f1_car_scaled))
            style.addImage(IMAGE_HAZARD_CONSTRUCTION, drawableToBitmap(R.drawable.ic_f1_car_scaled))
            style.addImage(IMAGE_HAZARD_TRAFFIC, drawableToBitmap(R.drawable.ic_f1_car_scaled))
            style.addImage(IMAGE_RACER, drawableToBitmap(R.drawable.ic_f1_car_scaled))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Applies the standard GeoRacing rendering layers to a loaded Style.
     * Call this inside style loaded callback.
     */
    fun applyLayers(style: Style) {
        // 1. Add Images
        addImages(style)

        // 2. Route Line (Neon Cyan)
        if (style.getSource(SOURCE_ROUTE) == null) {
            style.addSource(GeoJsonSource(SOURCE_ROUTE))
        }
        if (style.getLayer(LAYER_ROUTE) == null) {
            val routeLayer = LineLayer(LAYER_ROUTE, SOURCE_ROUTE).withProperties(
                PropertyFactory.lineColor("#00E5FF"), // Neon Cyan
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(1.0f)
            )
            style.addLayer(routeLayer)
        }

        // 3. POIs (Parkings & Gates)
        // We assume the source is populated by the screen
        if (style.getSource(SOURCE_POIS) == null) {
            style.addSource(GeoJsonSource(SOURCE_POIS))
        }
        if (style.getLayer(LAYER_POIS) == null) {
            val poiLayer = SymbolLayer(LAYER_POIS, SOURCE_POIS).withProperties(
                PropertyFactory.iconImage(Expression.get("icon_image")), // Data driven property
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor("#FFFFFF"),
                PropertyFactory.textHaloColor("#000000"),
                PropertyFactory.textHaloWidth(1f)
            )
            style.addLayer(poiLayer)
        }

        // 4. Update Car Layer (The F1 Marker)
        if (style.getSource(SOURCE_CAR) == null) {
            style.addSource(GeoJsonSource(SOURCE_CAR))
        }
        if (style.getLayer(LAYER_CAR) == null) {
            val carLayer = SymbolLayer(LAYER_CAR, SOURCE_CAR).withProperties(
                PropertyFactory.iconImage(IMAGE_CAR), // Or data-driven if using sprites
                PropertyFactory.iconSize(0.6f),       // Adjust scale
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                // We use iconRotate to spin the single asset if we don't have 36 sprites
                PropertyFactory.iconRotate(Expression.get("bearing")), 
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP)
            )
            style.addLayer(carLayer)
        }
    }

    private fun addImages(style: Style) {
        // Helper to convert drawable to bitmap
        fun drawableToBitmap(id: Int): Bitmap {
            val drawable = ContextCompat.getDrawable(context, id)!!
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }

        // Add assets - Ensure these drawables exist or use placeholders
        // Using built-in or existing resources for safety
        try {
            style.addImage(IMAGE_CAR, drawableToBitmap(R.drawable.ic_f1_car_scaled))
            // TODO: Add real Parking/Gate icons. Using CarCar as placeholder for now if missing.
             style.addImage(IMAGE_PARKING, drawableToBitmap(R.drawable.ic_f1_car_scaled))
             style.addImage(IMAGE_GATE, drawableToBitmap(R.drawable.ic_f1_car_scaled))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
