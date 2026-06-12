package com.georacing.georacing.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class GeoRacingCarAppService : CarAppService() {
    override fun onCreateSession(): Session = GeoRacingCarSession()

    override fun createHostValidator(): HostValidator {
        // Allowing all hosts for now to bypass 'p1' error and BuildConfig issues
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}
