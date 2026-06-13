package com.georacing.georacing.car

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.CarText
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.CarToast

class PoiListScreen(
    carContext: CarContext
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val pois = PoiRepository.getAllPois()

        val itemListBuilder = ItemList.Builder()

        pois.forEachIndexed { index, poi ->
            val place = Place.Builder(
                CarLocation.create(poi.latitude, poi.longitude)
            )
                .build()

            val metadata = Metadata.Builder()
                .setPlace(place)
                .build()

            val subtitle = "${poi.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${poi.description}"

            val row = Row.Builder()
                .setTitle(poi.name)
                .addText(CarText.create(subtitle))
                .setMetadata(metadata)
                // Lo marcamos como navegable para que no exija DistanceSpan
                .setBrowsable(true)
                .setOnClickListener {
                    startNavigationToPoi(poi)
                }
                .build()

            itemListBuilder.addItem(row)
        }

        return PlaceListMapTemplate.Builder()
            .setTitle("GeoRacing · Circuit de Barcelona–Catalunya")
            .setHeaderAction(Action.APP_ICON)
            .setItemList(itemListBuilder.build())
            .build()
    }

    private fun startNavigationToPoi(poi: PoiModel) {
        val uriString = "geo:${poi.latitude},${poi.longitude}?q=${Uri.encode(poi.name)}"
        val navIntent = Intent(
            CarContext.ACTION_NAVIGATE,
            Uri.parse(uriString)
        )

        try {
            carContext.startCarApp(navIntent)
        } catch (e: Exception) {
            Log.e("GeoRacingCar", "Error starting navigation", e)
            CarToast.makeText(
                carContext,
                "No se ha podido iniciar la navegación",
                CarToast.LENGTH_SHORT
            ).show()
        }
    }
}
