package com.georacing.georacing.domain.usecase

import com.georacing.georacing.infrastructure.ble.StaffBeaconAdvertiser
import kotlinx.coroutines.flow.Flow

/**
 * Caso de Uso que permite a un perfil Staff emitir una alerta de emergencia usando
 * su dispositivo móvil como "Baliza Humana" BLE.
 */
class BroadcastEmergencyUseCase(
    private val staffBeaconAdvertiser: StaffBeaconAdvertiser
) {

    /**
     * Inicia la emisión de la baliza de emergencia con el estado proporcionado.
     * @param isEvacuation Si es true emite "EVACUATE", sino "DANGER".
     * @return Flow con el estado de la emisión (true = éxito, false = error/apagado)
     */
    operator fun invoke(isEvacuation: Boolean): Flow<Boolean> {
        val payload = if (isEvacuation) "STATE: EVACUATE" else "STATE: DANGER"
        return staffBeaconAdvertiser.startAdvertising(payload)
    }
}
