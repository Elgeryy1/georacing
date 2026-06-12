package com.georacing.georacing.data.remote.dto

import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType
import com.google.gson.annotations.SerializedName

data class PoiDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String,
    @SerializedName("zone") val zone: String,
    @SerializedName("map_x") val mapX: Float,
    @SerializedName("map_y") val mapY: Float
)

fun PoiDto.toDomain(): Poi {
    return Poi(
        id = id,
        name = name,
        type = try {
            PoiType.valueOf(type.uppercase())
        } catch (e: Exception) {
            PoiType.OTHER
        },
        description = description,
        zone = zone,
        mapX = mapX,
        mapY = mapY
    )
}
