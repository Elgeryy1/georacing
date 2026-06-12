package com.georacing.georacing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("role") val role: String = "user"
)
