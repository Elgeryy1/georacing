package com.georacing.georacing.infrastructure.telemetry

import com.georacing.georacing.data.local.dao.TelemetryDao
import com.georacing.georacing.data.local.entities.TelemetryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * La Caja Negra Operativa.
 * Escucha eventos críticos en la aplicación y los persiste offline en Room.
 */
class BlackBoxLogger(
    private val telemetryDao: TelemetryDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Registra un evento en la "Caja Negra".
     * @param eventType Tipo de evento (ej: "NETWORK_DROP", "BLE_TIMEOUT").
     * @param metadata Información adicional del evento (JSON o texto plano).
     */
    fun logEvent(eventType: String, metadata: String) {
        scope.launch {
            val entity = TelemetryEntity(
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                metadata = metadata
            )
            telemetryDao.insertLog(entity)
        }
    }
}
