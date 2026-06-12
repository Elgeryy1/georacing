package com.georacing.georacing.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EnsureTableRequest(
    @SerializedName("tableName") val tableName: String
)

data class EnsureColumnRequest(
    @SerializedName("tableName") val tableName: String,
    @SerializedName("columnName") val columnName: String,
    @SerializedName("columnType") val columnType: String
)

data class UpsertRequest(
    @SerializedName("table") val table: String,
    @SerializedName("data") val data: Map<String, Any?>
)
