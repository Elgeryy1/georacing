package com.georacing.georacing.utils

import android.location.Location

/**
 * Detector de situaciones "fuera de ruta" (off-route).
 * 
 * Implementa lógica robusta para evitar falsos positivos:
 * - No marca off-route instantáneamente
 * - Requiere estar fuera durante un tiempo mínimo
 * - Threshold de distancia configurable
 */
object OffRouteDetector {
    
    /**
     * FASE 1.2: Threshold dinámico según velocidad
     * - Parking/ciudad (<40 km/h): 30m (más estricto)
     * - Carretera normal (40-80 km/h): 50m (actual)
     * - Autopista (>80 km/h): 80m (más permisivo, evita falsos positivos en 6 carriles)
     */
    fun dynamicOffRouteThreshold(speedKmh: Float): Double {
        return when {
            speedKmh < 40f -> 30.0
            speedKmh < 80f -> 50.0
            else -> 80.0
        }
    }
    
    /**
     * Distancia máxima permitida al punto más cercano de la ruta (LEGACY - ahora dinámico).
     * @deprecated Usar dynamicOffRouteThreshold(speedKmh) en su lugar
     */
    @Deprecated("Use dynamicOffRouteThreshold(speedKmh) for speed-dependent threshold")
    private const val OFF_ROUTE_THRESHOLD_METERS = 50.0
    
    /**
     * Tiempo mínimo que el usuario debe estar fuera de ruta
     * antes de confirmar que realmente se desvió.
     * 
     * Esto evita recalcular la ruta por:
     * - Imprecisión GPS momentánea
     * - Túneles
     * - Cambios de carril
     */
    private const val MIN_OFF_ROUTE_DURATION_MS = 3000L  // 3 segundos
    
    /**
     * Timestamp de la primera vez que detectamos off-route.
     * null = actualmente dentro de la ruta
     */
    private var firstOffRouteTime: Long? = null
    
    /**
     * Última ubicación que se comprobó
     */
    private var lastCheckedLocation: Location? = null
    
    /**
     * Comprueba si el usuario está fuera de ruta de forma confirmada.
     * 
     * @param location Ubicación GPS actual
     * @param snapResult Resultado del snap a la ruta
     * @return true si confirmamos que está fuera de ruta
     */
    fun checkOffRoute(
        location: Location,
        snapResult: SnapResult
    ): Boolean {
        // FASE 1.2: Usar threshold dinámico según velocidad
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
        val threshold = dynamicOffRouteThreshold(speedKmh)
        
        val isCurrentlyOffRoute = snapResult.distanceToRoute > threshold
        
        if (isCurrentlyOffRoute) {
            val now = System.currentTimeMillis()
            
            if (firstOffRouteTime == null) {
                // Primera detección de off-route
                firstOffRouteTime = now
                lastCheckedLocation = location
                return false  // Dar tiempo de gracia
            }
            
            val durationOffRoute = now - firstOffRouteTime!!
            
            if (durationOffRoute >= MIN_OFF_ROUTE_DURATION_MS) {
                // Confirmar: ha estado fuera de ruta suficiente tiempo
                android.util.Log.w(
                    "OffRouteDetector",
                    "OFF ROUTE CONFIRMED: ${snapResult.distanceToRoute}m away (threshold=${threshold}m at ${speedKmh}km/h) for ${durationOffRoute}ms"
                )
                return true
            } else {
                // Aún en período de gracia
                android.util.Log.d(
                    "OffRouteDetector",
                    "OFF ROUTE WARNING: ${snapResult.distanceToRoute}m away (threshold=${threshold}m at ${speedKmh}km/h), waiting... (${durationOffRoute}ms)"
                )
                return false
            }
        } else {
            // Volvió a la ruta (o nunca salió)
            if (firstOffRouteTime != null) {
                android.util.Log.i("OffRouteDetector", "Back on route")
            }
            reset()
            return false
        }
    }
    
    /**
     * Resetea el estado interno.
     * Llamar cuando:
     * - Usuario vuelve a la ruta
     * - Se recalcula la ruta
     * - Se inicia nueva navegación
     */
    fun reset() {
        firstOffRouteTime = null
        lastCheckedLocation = null
    }
    
    /**
     * Versión alternativa con threshold personalizado.
     * Útil para ajustar sensibilidad en diferentes escenarios.
     * 
     * @param customThresholdMeters Distancia máxima permitida
     * @param customDurationMs Tiempo mínimo fuera de ruta
     */
    fun checkOffRouteCustom(
        location: Location,
        snapResult: SnapResult,
        customThresholdMeters: Double = OFF_ROUTE_THRESHOLD_METERS,
        customDurationMs: Long = MIN_OFF_ROUTE_DURATION_MS
    ): Boolean {
        val isCurrentlyOffRoute = snapResult.distanceToRoute > customThresholdMeters
        
        if (isCurrentlyOffRoute) {
            val now = System.currentTimeMillis()
            
            if (firstOffRouteTime == null) {
                firstOffRouteTime = now
                lastCheckedLocation = location
                return false
            }
            
            val durationOffRoute = now - firstOffRouteTime!!
            return durationOffRoute >= customDurationMs
        } else {
            reset()
            return false
        }
    }
    
    /**
     * Helper para debugging: obtener estado actual
     */
    fun getDebugInfo(): String {
        return if (firstOffRouteTime != null) {
            val duration = System.currentTimeMillis() - firstOffRouteTime!!
            "Off-route for ${duration}ms"
        } else {
            "On route"
        }
    }
}
