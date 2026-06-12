package com.georacing.georacing.car

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.widget.ImageView
import com.georacing.georacing.R
import org.maplibre.android.maps.MapView

class CarMapPresentation(context: Context, display: Display) : Presentation(context, display) {

    lateinit var mapView: MapView
    lateinit var hudOverlay: ImageView  // Vista para el HUD visual
    lateinit var speedometer: SpeedometerView // Waze-style speedometer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.car_navigation_map)
        mapView = findViewById(R.id.mapView)
        hudOverlay = findViewById(R.id.hudOverlay)
        speedometer = findViewById(R.id.speedometer)
    }
}
