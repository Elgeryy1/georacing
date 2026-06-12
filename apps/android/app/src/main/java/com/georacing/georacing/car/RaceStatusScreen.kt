package com.georacing.georacing.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.georacing.georacing.R
import com.georacing.georacing.debug.ScenarioSimulator
import com.georacing.georacing.domain.model.CircuitMode

/**
 * Race Status Screen - GridTemplate showing track status info.
 * Accessible via status button in navigation ActionStrip.
 */
class RaceStatusScreen(
    carContext: CarContext,
    private val circuitMode: CircuitMode = CircuitMode.GREEN_FLAG,
    private val assignedParking: String = "C"
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val gridItemBuilder = ItemList.Builder()
        
        // 1. Track Status (Green/Red/Yellow)
        gridItemBuilder.addItem(
            GridItem.Builder()
                .setTitle(getTrackStatusTitle())
                .setText(getTrackStatusDescription())
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, getTrackStatusIcon())
                    ).setTint(getTrackStatusColor()).build()
                )
                .setOnClickListener { /* Show details */ }
                .build()
        )
        
        // 2. Track Temperature
        val temperature = ScenarioSimulator.crowdIntensity.value * 30 + 25 // 25-55°C simulated
        gridItemBuilder.addItem(
            GridItem.Builder()
                .setTitle("${temperature.toInt()}°C")
                .setText("Temp. Pista")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.mipmap.ic_launcher_round)
                    ).build()
                )
                .build()
        )
        
        // 3. Assigned Parking
        gridItemBuilder.addItem(
            GridItem.Builder()
                .setTitle("Parking $assignedParking")
                .setText("Tu ubicación asignada")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.mipmap.ic_launcher_round)
                    ).setTint(CarColor.BLUE).build()
                )
                .setOnClickListener {
                    // Could navigate to parking
                }
                .build()
        )
        
        // 4. Active Hazards
        val hazardCount = ScenarioSimulator.activeHazards.value.size
        gridItemBuilder.addItem(
            GridItem.Builder()
                .setTitle(if (hazardCount > 0) "$hazardCount Alertas" else "Sin Alertas")
                .setText("Peligros en ruta")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.mipmap.ic_launcher_round)
                    ).setTint(if (hazardCount > 0) CarColor.RED else CarColor.GREEN).build()
                )
                .build()
        )
        
        return GridTemplate.Builder()
            .setTitle("🏁 Estado del Circuito")
            .setHeaderAction(Action.BACK)
            .setSingleList(gridItemBuilder.build())
            .build()
    }
    
    private fun getTrackStatusTitle(): String {
        return when (circuitMode) {
            CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> "🟢 VERDE"
            CircuitMode.YELLOW_FLAG -> "🟡 AMARILLA"
            CircuitMode.SAFETY_CAR -> "🚗 SAFETY CAR"
            CircuitMode.VSC -> "🚦 VSC"
            CircuitMode.RED_FLAG -> "🔴 BANDERA ROJA"
            CircuitMode.EVACUATION -> "🆘 EVACUACIÓN"
            CircuitMode.UNKNOWN -> "⚪ DESCONOCIDO"
        }
    }
    
    private fun getTrackStatusDescription(): String {
        return when (circuitMode) {
            CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> "Pista abierta"
            CircuitMode.YELLOW_FLAG -> "Precaución"
            CircuitMode.SAFETY_CAR -> "Detrás del SC"
            CircuitMode.VSC -> "Velocidad reducida"
            CircuitMode.RED_FLAG -> "Carrera detenida"
            CircuitMode.EVACUATION -> "¡Salga ahora!"
            CircuitMode.UNKNOWN -> "Sin datos"
        }
    }
    
    private fun getTrackStatusColor(): CarColor {
        return when (circuitMode) {
            CircuitMode.GREEN_FLAG, CircuitMode.NORMAL -> CarColor.GREEN
            CircuitMode.YELLOW_FLAG, CircuitMode.SAFETY_CAR, CircuitMode.VSC -> CarColor.YELLOW
            CircuitMode.RED_FLAG, CircuitMode.EVACUATION -> CarColor.RED
            else -> CarColor.DEFAULT
        }
    }
    
    private fun getTrackStatusIcon(): Int {
        // Use launcher icon as placeholder - would use custom flag icons
        return R.mipmap.ic_launcher_round
    }
}
