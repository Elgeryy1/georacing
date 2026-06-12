package com.georacing.georacing.data.remote.dto

import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.google.gson.annotations.SerializedName

data class CircuitStateDto(
    @SerializedName("id") val id: String,
    @SerializedName("global_mode") val global_mode: String?,
    @SerializedName("mode") val mode: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("temperature") val temperature: String?,
    @SerializedName("humidity") val humidity: String? = null,
    @SerializedName("wind") val wind: String? = null,
    @SerializedName("forecast") val forecast: String? = null,
    @SerializedName("last_updated") val last_updated: String?
)

private fun String.toCircuitMode(): CircuitMode =
    when (this.uppercase()) {
        "NORMAL" -> CircuitMode.NORMAL
        "GREEN_FLAG", "GREEN" -> CircuitMode.GREEN_FLAG
        "YELLOW_FLAG", "YELLOW" -> CircuitMode.YELLOW_FLAG
        "SAFETY_CAR", "SC" -> CircuitMode.SAFETY_CAR
        "VSC" -> CircuitMode.VSC
        "RED_FLAG", "RED" -> CircuitMode.RED_FLAG
        "EVACUATION" -> CircuitMode.EVACUATION 
        else -> CircuitMode.UNKNOWN
    }

fun CircuitStateDto.toDomain(): CircuitState {
    return CircuitState(
        mode = (global_mode ?: mode)?.toCircuitMode() ?: CircuitMode.UNKNOWN,
        message = message ?: "",
        temperature = temperature,
        updatedAt = last_updated ?: "",
        humidity = humidity,
        wind = wind,
        forecast = forecast,
        sessionInfo = null
    )
}
