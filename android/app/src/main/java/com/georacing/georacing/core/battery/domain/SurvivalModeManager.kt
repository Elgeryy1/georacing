package com.georacing.georacing.core.battery.domain

import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Gestor principal de dominio que evalúa el nivel de batería
 * de forma reactiva y determina en qué modo operativo está la App.
 * Este es el core del "Energy Shedding Protocol".
 */
class SurvivalModeManager(
    private val batteryMonitor: BatteryMonitor
) {
    // Definimos el umbral crítico para la carrera.
    companion object {
        const val SURVIVAL_THRESHOLD = 30
        const val WARNING_THRESHOLD = 50
    }

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    /**
     * Estado global que reacciona a los cambios en la batería.
     * Toda la aplicación (ViewModels, Servicios) puede observarlo.
     */
    val survivalMode: StateFlow<SurvivalMode> = batteryMonitor.batteryState
        .map { state ->
            when {
                state.percentage < 0 -> SurvivalMode.NORMAL // Desconocido, por defecto normal
                state.isCharging -> SurvivalMode.NORMAL // Si está cargando, sin restricciones
                state.percentage <= SURVIVAL_THRESHOLD -> SurvivalMode.SURVIVAL
                state.percentage <= WARNING_THRESHOLD -> SurvivalMode.WARNING
                else -> SurvivalMode.NORMAL
            }
        }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.Eagerly, // Mantenemos el modo vivo desde inicio
            initialValue = SurvivalMode.NORMAL
        )

    /**
     * Si necesitas acciones imperativas que maten procesos inmediatamente,
     * este es el lugar para implementarlas (Load Shedding).
     * Por ahora mostramos los intent/eventos requeridos.
     */
    fun enforceSurvivalRestrictions() {
        if (survivalMode.value == SurvivalMode.SURVIVAL) {
            killGamificationAndAR()
            forceOfflineTelemetry()
        }
    }

    private fun killGamificationAndAR() {
        // Aquí puedes emitir un evento con EventBus/SharedFlow
        // que tus servicios de AR o Gamification escuchen para detenerse (Ej. ARCore session).
    }

    private fun forceOfflineTelemetry() {
        // Aquí cancelaríamos los jobs de WorkManager periódicos
        // Ej: WorkManager.getInstance(context).cancelUniqueWork("telemetry_sync")
    }
}
