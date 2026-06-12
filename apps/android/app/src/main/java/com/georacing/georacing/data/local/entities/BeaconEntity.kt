package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity para Configuración de Beacons (BLE).
 * Cachea los datos de las balizas físicas instaladas en el circuito.
 * Alineado con el esquema del Panel Metropolis.
 */
@Entity(tableName = "beacons")
data class BeaconEntity(
    @PrimaryKey
    val id: String,
    val beaconUid: String = "",
    val name: String,
    val zone: String,
    val mapX: Float = 0f,
    val mapY: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val messageNormal: String = "",
    val messageEmergency: String = "",
    val message: String = "",
    val arrowDirection: String = "NONE",
    val mode: String = "NORMAL",
    val color: String = "#00FF00",
    val brightness: Int = 100,
    val batteryLevel: Int = 100,
    val isOnline: Boolean = true,
    val hasScreen: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
