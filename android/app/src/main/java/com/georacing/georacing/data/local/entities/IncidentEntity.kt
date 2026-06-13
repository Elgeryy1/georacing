package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.georacing.georacing.domain.model.IncidentCategory

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey
    val id: String, // UUID generated
    val category: String, // Stored as name
    val description: String,
    val beaconId: String?,
    val zone: String?,
    val timestamp: Long,
    val isSynced: Boolean = false // Sync flag
)
