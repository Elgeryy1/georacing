package com.georacing.georacing.data.remote.dto

import com.georacing.georacing.domain.model.ArrowDirection
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.model.BeaconMode
import com.google.gson.annotations.SerializedName

/**
 * DTO para deserializar beacons del backend.
 * Compatible con el esquema del Panel Metropolis y el legacy.
 */
data class BeaconConfigDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("beacon_uid") val beaconUid: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("zone") val zone: String? = null,
    @SerializedName("zone_id") val zoneId: Int? = null,
    @SerializedName("map_x") val mapX: Float? = null,
    @SerializedName("map_y") val mapY: Float? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("message_normal") val messageNormal: String? = null,
    @SerializedName("message_emergency") val messageEmergency: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("arrow_direction") val arrowDirection: String? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("brightness") val brightness: Int? = null,
    @SerializedName("battery_level") val batteryLevel: Int? = null,
    @SerializedName("is_online") val isOnline: Boolean? = null,
    @SerializedName("has_screen") val hasScreen: Boolean? = null
)

fun BeaconConfigDto.toDomain(): BeaconConfig {
    return BeaconConfig(
        id = id ?: beaconUid ?: "",
        beaconUid = beaconUid ?: id ?: "",
        name = name ?: "",
        zone = zone ?: (zoneId?.toString() ?: ""),
        mapX = mapX ?: latitude?.toFloat() ?: 0f,
        mapY = mapY ?: longitude?.toFloat() ?: 0f,
        latitude = latitude ?: mapX?.toDouble() ?: 0.0,
        longitude = longitude ?: mapY?.toDouble() ?: 0.0,
        messageNormal = messageNormal ?: message ?: "",
        messageEmergency = messageEmergency ?: message ?: "",
        message = message ?: messageNormal ?: "",
        arrowDirection = try {
            ArrowDirection.valueOf((arrowDirection ?: "NONE").uppercase())
        } catch (e: Exception) {
            ArrowDirection.NONE
        },
        mode = try {
            BeaconMode.valueOf((mode ?: "NORMAL").uppercase())
        } catch (e: Exception) {
            BeaconMode.NORMAL
        },
        color = color ?: "#00FF00",
        brightness = brightness ?: 100,
        batteryLevel = batteryLevel ?: 100,
        isOnline = isOnline ?: true,
        hasScreen = hasScreen ?: false
    )
}
