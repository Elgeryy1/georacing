package com.georacing.georacing.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template

class DestinationSearchScreen(carContext: CarContext) : Screen(carContext) {

    private val allPois = PoiRepository.getAllPois()
    private var filteredPois = allPois

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        filteredPois.forEach { poi ->
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(poi.name)
                    .addText(poi.description)
                    .setOnClickListener {
                        screenManager.push(
                            GeoRacingNavigationScreen(
                                carContext = carContext,
                                destTitle = poi.name,
                                destLat = poi.latitude,
                                destLon = poi.longitude
                            )
                        )
                    }
                    .build()
            )
        }

        return SearchTemplate.Builder(
            object : SearchTemplate.SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    filteredPois = if (searchText.isEmpty()) {
                        allPois
                    } else {
                        allPois.filter {
                            it.name.contains(searchText, ignoreCase = true) ||
                            it.description.contains(searchText, ignoreCase = true)
                        }
                    }
                    invalidate()
                }

                override fun onSearchSubmitted(searchText: String) {
                    onSearchTextChanged(searchText)
                }
            }
        )
        .setHeaderAction(Action.BACK)
        .setShowKeyboardByDefault(true)
        .setItemList(listBuilder.build())
        .build()
    }
}
