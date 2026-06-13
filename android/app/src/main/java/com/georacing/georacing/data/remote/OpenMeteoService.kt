package com.georacing.georacing.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo API — Free weather API, no API key required.
 * https://open-meteo.com/
 * 
 * Used for ClimaSmartScreen with real weather data at Circuit de Barcelona-Catalunya.
 */
interface OpenMeteoService {

    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,wind_direction_10m,weather_code,surface_pressure",
        @Query("hourly") hourly: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") daily: String = "sunrise,sunset,uv_index_max",
        @Query("timezone") timezone: String = "Europe/Madrid",
        @Query("forecast_days") forecastDays: Int = 1
    ): OpenMeteoResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        val instance: OpenMeteoService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoService::class.java)
        }
    }
}

data class OpenMeteoResponse(
    val current: CurrentWeather?,
    val hourly: HourlyWeather?,
    val daily: DailyWeather?
)

data class CurrentWeather(
    val temperature_2m: Double?,
    val relative_humidity_2m: Int?,
    val apparent_temperature: Double?,
    val wind_speed_10m: Double?,
    val wind_direction_10m: Int?,
    val weather_code: Int?,
    val surface_pressure: Double?
)

data class HourlyWeather(
    val time: List<String>?,
    val temperature_2m: List<Double>?,
    val precipitation_probability: List<Int>?,
    val weather_code: List<Int>?
)

data class DailyWeather(
    val sunrise: List<String>?,
    val sunset: List<String>?,
    val uv_index_max: List<Double>?
)
