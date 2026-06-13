package com.georacing.georacing.car

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestiona la guía de carriles basándose en datos reales de OSRM.
 *
 * OSRM devuelve intersections[].lanes[] en cada step del route response.
 * Cada lane tiene:
 *  - valid: Boolean → true si ese carril es válido para la maniobra
 *  - indications: Array<String> → "straight", "left", "right", "slight left", etc.
 *
 * Este manager parsea esa información y genera un modelo visual para mostrar
 * en Android Auto y en la pantalla del móvil.
 */
object LaneGuidanceManager {

    private const val TAG = "LaneGuidanceManager"

    /**
     * Representación de un carril individual.
     */
    data class Lane(
        val directions: List<LaneDirection>,
        val isRecommended: Boolean // true si este carril es válido para la próxima maniobra
    )

    /**
     * Dirección posible de un carril.
     */
    enum class LaneDirection(val symbol: String) {
        STRAIGHT("↑"),
        LEFT("←"),
        RIGHT("→"),
        SLIGHT_LEFT("↖"),
        SLIGHT_RIGHT("↗"),
        SHARP_LEFT("⬉"),
        SHARP_RIGHT("⬈"),
        UTURN("↩"),
        MERGE_LEFT("⇽"),
        MERGE_RIGHT("⇾"),
        NONE("·");

        companion object {
            fun fromOsrm(indication: String): LaneDirection = when (indication.lowercase()) {
                "straight" -> STRAIGHT
                "left" -> LEFT
                "right" -> RIGHT
                "slight left" -> SLIGHT_LEFT
                "slight right" -> SLIGHT_RIGHT
                "sharp left" -> SHARP_LEFT
                "sharp right" -> SHARP_RIGHT
                "uturn" -> UTURN
                "merge left" -> MERGE_LEFT
                "merge right" -> MERGE_RIGHT
                "none" -> NONE
                else -> STRAIGHT
            }
        }
    }

    /**
     * Resultado de análisis de carriles para un step específico.
     */
    data class LaneGuidanceResult(
        val lanes: List<Lane>,
        val totalLanes: Int,
        val recommendedLaneIndices: List<Int>,
        val cueText: String, // "Usa el carril derecho", etc.
        val laneConfig: LaneConfig // Compatibilidad con el enum existente
    )

    /**
     * Parsea los datos de carriles desde la respuesta JSON de OSRM para un step.
     *
     * @param stepJson Un objeto JSON correspondiente a un step de OSRM
     * @return LaneGuidanceResult con la información de carriles, o null si no hay datos
     */
    fun parseLanesFromStep(stepJson: JSONObject): LaneGuidanceResult? {
        try {
            val intersections = stepJson.optJSONArray("intersections") ?: return null
            if (intersections.length() == 0) return null

            // Tomar la primera intersección (la más relevante para la maniobra)
            val intersection = intersections.getJSONObject(0)
            val lanesJson = intersection.optJSONArray("lanes") ?: return null
            if (lanesJson.length() == 0) return null

            val lanes = mutableListOf<Lane>()
            val recommendedIndices = mutableListOf<Int>()

            for (i in 0 until lanesJson.length()) {
                val laneJson = lanesJson.getJSONObject(i)
                val valid = laneJson.optBoolean("valid", false)
                val indicationsJson = laneJson.optJSONArray("indications") ?: JSONArray()

                val directions = mutableListOf<LaneDirection>()
                for (j in 0 until indicationsJson.length()) {
                    directions.add(LaneDirection.fromOsrm(indicationsJson.getString(j)))
                }
                if (directions.isEmpty()) directions.add(LaneDirection.STRAIGHT)

                lanes.add(Lane(directions = directions, isRecommended = valid))
                if (valid) recommendedIndices.add(i)
            }

            val totalLanes = lanes.size
            val cueText = generateCueText(lanes, recommendedIndices, totalLanes)
            val laneConfig = inferLaneConfig(recommendedIndices, totalLanes)

            Log.d(TAG, "📍 Lanes: $totalLanes total, recommended=${recommendedIndices}, cue='$cueText'")

            return LaneGuidanceResult(
                lanes = lanes,
                totalLanes = totalLanes,
                recommendedLaneIndices = recommendedIndices,
                cueText = cueText,
                laneConfig = laneConfig
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing lane data", e)
            return null
        }
    }

    /**
     * Genera el texto de guía de carril en español.
     */
    private fun generateCueText(
        lanes: List<Lane>,
        recommendedIndices: List<Int>,
        totalLanes: Int
    ): String {
        if (recommendedIndices.isEmpty() || recommendedIndices.size == totalLanes) {
            return "Cualquier carril"
        }

        // Determinar posición de los carriles recomendados
        val isLeftMost = recommendedIndices.contains(0)
        val isRightMost = recommendedIndices.contains(totalLanes - 1)
        val count = recommendedIndices.size

        return when {
            count == 1 && isLeftMost -> "Usa el carril izquierdo"
            count == 1 && isRightMost -> "Usa el carril derecho"
            count == 1 -> "Usa el carril ${recommendedIndices[0] + 1} de $totalLanes"
            count == 2 && isRightMost -> "Usa los 2 carriles derechos"
            count == 2 && isLeftMost -> "Usa los 2 carriles izquierdos"
            isRightMost -> "Usa los $count carriles derechos"
            isLeftMost -> "Usa los $count carriles izquierdos"
            else -> "Usa los carriles centrales"
        }
    }

    /**
     * Infiere el LaneConfig legacy para compatibilidad.
     */
    private fun inferLaneConfig(recommendedIndices: List<Int>, totalLanes: Int): LaneConfig {
        if (recommendedIndices.isEmpty() || recommendedIndices.size == totalLanes) {
            return LaneConfig.ANY_LANE
        }
        val isRightMost = recommendedIndices.contains(totalLanes - 1)
        val isLeftMost = recommendedIndices.contains(0)
        val count = recommendedIndices.size

        return when {
            count == 1 && isRightMost -> LaneConfig.RIGHT_LANE
            count == 1 && isLeftMost -> LaneConfig.LEFT_LANE
            isRightMost -> LaneConfig.RIGHT_LANES
            else -> LaneConfig.ANY_LANE
        }
    }

    /**
     * Genera representación de texto visual de los carriles.
     * Ejemplo: "[↑] [↑✓] [→✓]" donde ✓ marca los carriles recomendados.
     */
    fun lanesVisualString(result: LaneGuidanceResult): String {
        return result.lanes.joinToString(" ") { lane ->
            val arrows = lane.directions.joinToString("") { it.symbol }
            if (lane.isRecommended) "[$arrows✓]" else "[$arrows]"
        }
    }

    /**
     * Parsea todos los steps de una ruta OSRM y devuelve un mapa de guía de carriles.
     *
     * @param routeJson El JSON completo de la ruta OSRM
     * @return Mapa de índice de step → LaneGuidanceResult
     */
    fun parseAllLanesFromRoute(routeJson: JSONObject): Map<Int, LaneGuidanceResult> {
        val result = mutableMapOf<Int, LaneGuidanceResult>()
        try {
            val routes = routeJson.optJSONArray("routes") ?: return result
            if (routes.length() == 0) return result

            val legs = routes.getJSONObject(0).optJSONArray("legs") ?: return result

            var stepIndex = 0
            for (legIdx in 0 until legs.length()) {
                val steps = legs.getJSONObject(legIdx).optJSONArray("steps") ?: continue
                for (stepIdx in 0 until steps.length()) {
                    val laneResult = parseLanesFromStep(steps.getJSONObject(stepIdx))
                    if (laneResult != null) {
                        result[stepIndex] = laneResult
                    }
                    stepIndex++
                }
            }

            Log.d(TAG, "✅ Parsed lane guidance for ${result.size}/$stepIndex steps")
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing route lanes", e)
        }
        return result
    }
}
