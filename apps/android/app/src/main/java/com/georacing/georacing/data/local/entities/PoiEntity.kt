package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity para Puntos de Interés (POIs).
 * Cachea la información de lugares importantes del circuito.
 */
@Entity(tableName = "pois")
data class PoiEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,           // Stored as String, converted to PoiType in domain
    val description: String,
    val zone: String,
    val mapX: Float,
    val mapY: Float,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)
