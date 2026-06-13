package com.georacing.georacing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupCreateRequest(
    @SerializedName("id") val id: String,
    @SerializedName("owner_user_id") val ownerUserId: String,
    @SerializedName("name") val name: String,
    @SerializedName("created_at") val createdAt: Long = System.currentTimeMillis()
)
