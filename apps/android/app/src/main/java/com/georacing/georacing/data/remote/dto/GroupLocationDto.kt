package com.georacing.georacing.data.remote.dto

import com.georacing.georacing.data.model.GroupMemberLocation
import com.google.gson.annotations.SerializedName
import java.util.Date

data class GroupLocationRequest(
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("displayName") val displayName: String
)

data class GroupMemberDto(
    @SerializedName("user_uuid") val userUuid: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("displayName") val displayName: String?
)

fun GroupMemberDto.toDomain(): GroupMemberLocation {
    // Fix for timestamp: API might return 0 or missing field.
    // If timestamp is 0, we assume it's "now" to avoid showing "50 years ago".
    // Also check if it's in seconds (small number) or milliseconds.
    val safeTimestamp = if (timestamp <= 0) {
        System.currentTimeMillis()
    } else if (timestamp < 10000000000L) {
        // If less than 10^10, it's likely seconds (valid until year 2286)
        timestamp * 1000
    } else {
        timestamp
    }

    return GroupMemberLocation(
        userId = userUuid,
        displayName = displayName ?: "User $userUuid", // Use API name or fallback to ID
        photoUrl = "",
        latitude = lat,
        longitude = lon,
        lastUpdated = com.google.firebase.Timestamp(Date(safeTimestamp)),
        sharing = true // If they are in the list, they are sharing
    )
}
