package com.georacing.georacing.car

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmService {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = true,
        @Query("annotations") annotations: String? = null  // "speed,maxspeed,duration,distance"
    ): OsrmResponse
}

interface GraphHopperService {
    @GET("route")
    suspend fun getRoute(
        @Query("point") points: List<String>, // ["lat,lon", "lat,lon"]
        @Query("vehicle") vehicle: String = "car",
        @Query("locale") locale: String = "es",
        @Query("instructions") instructions: Boolean = true,
        @Query("calc_points") calcPoints: Boolean = true,
        @Query("points_encoded") pointsEncoded: Boolean = false,
        @Query("ch.disable") chDisable: Boolean = true, // Permite usar tráfico
        @Query("key") apiKey: String = "YOUR_GRAPHHOPPER_API_KEY"
    ): GraphHopperResponse
}
