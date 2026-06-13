package com.georacing.georacing.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class GeoRacingCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GeoRacingCarScreen(carContext)
    }
}
