package com.georacing.georacing.data.remote.dto

import com.georacing.georacing.domain.model.IncidentReport
import com.google.gson.annotations.SerializedName

data class IncidentReportDto(
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("beacon_id") val beaconId: String?,
    @SerializedName("zone") val zone: String?,
    @SerializedName("timestamp") val timestamp: Long
)

fun IncidentReport.toDto(): IncidentReportDto {
    return IncidentReportDto(
        category = category.name,
        description = description,
        beaconId = beaconId,
        zone = zone,
        timestamp = timestamp
    )
}
