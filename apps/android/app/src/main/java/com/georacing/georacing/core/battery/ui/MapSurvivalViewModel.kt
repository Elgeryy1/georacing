package com.georacing.georacing.core.battery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.core.battery.domain.SurvivalMode
import com.georacing.georacing.core.battery.domain.SurvivalModeManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ejemplo de ViewModel para demostrar el consumo del modo de supervivencia
 * desde la UI (Compose o Fragment). En tu proyecto de Dagger/Hilt pondrías @HiltViewModel
 */
// @HiltViewModel
class MapSurvivalViewModel(
    private val survivalModeManager: SurvivalModeManager
) : ViewModel() {

    // Exponemos el estilo del mapa a la UI de MapLibre reactivamente.
    // Compose hará la recomposición de UI de inmediato cuando la batería baje del 30%.
    val mapStyleUrl: StateFlow<String> = survivalModeManager.survivalMode
        .map { mode ->
            when (mode) {
                SurvivalMode.SURVIVAL -> "asset://style_oled_black.json" // Minimalista sin texturas, color puro RGB 0,0,0
                SurvivalMode.WARNING -> "asset://style_dark.json"
                SurvivalMode.NORMAL -> "asset://style_day_3d.json"       // Renderizado completo
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "asset://style_day_3d.json"
        )
    
    // Si necesitas apagar módulos específicos desde la presentación
    fun onUserOpenedARFeature() {
        if (survivalModeManager.survivalMode.value == SurvivalMode.SURVIVAL) {
            // Mostrar Toast: "Operación denegada. Nivel de energía crítico para emergencias."
            return
        }
        // Iniciar flujo de Realidad Aumentada...
    }
}
