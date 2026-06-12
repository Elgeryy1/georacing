package com.georacing.georacing.data.remote

import com.georacing.georacing.data.remote.ApiClient
import com.georacing.georacing.data.remote.GeoRacingApi

object RetrofitClient {
    // Delegating to unified ApiClient
    val api: GeoRacingApi by lazy {
        ApiClient.retrofit.create(GeoRacingApi::class.java)
    }
}
