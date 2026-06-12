package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity para el Estado Global del Circuito.
 * Esta tabla tiene UNA SOLA FILA (singleton pattern).
 * ID fijo = 1 para garantizar actualización en lugar de inserción múltiple.
 */
@Entity(tableName = "circuit_state")
data class CircuitStateEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val mode: String,           // "NORMAL", "SAFETY_CAR", "RED_FLAG", "EVACUATION", "UNKNOWN"
    val message: String?,
    val temperature: String?,
    val humidity: String? = null,
    val wind: String? = null,
    val forecast: String? = null,
    val updatedAt: String,
    val lastSynced: Long = System.currentTimeMillis()
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
