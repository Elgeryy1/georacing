package com.georacing.georacing.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un evento crítico en la "Caja Negra" (BlackBox).
 * Se almacenan localmente y se envían al servidor cuando hay red disponible.
 */
@Entity(tableName = "telemetry_logs")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val eventType: String, // e.g., "NETWORK_DROP", "BLE_TIMEOUT", "APP_CRASH_RECOVER"
    val metadata: String   // JSON or extra info
)
