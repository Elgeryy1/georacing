package com.georacing.georacing.infrastructure.health

import android.content.Context
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Gestor de la API de Health Connect (Infraestructura).
 * Se encarga de solicitar permisos y leer agregados diarios (0 coste de batería).
 */
class HealthConnectManager(
    private val context: Context
) {
    // Inicialización perezosa del cliente
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    // Los permisos mínimos para la Fase 3: Pasos y Distancia de sólo lectura
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    /**
     * Comprueba si la aplicación Health Connect está disponible y soportada.
     */
    fun isAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            true // En Android 14+ está integrada en el framework
        } else {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }
    }

    /**
     * Comprueba si tenemos todos los permisos concedidos.
     */
    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /**
     * Genera el contrato para solicitar permisos desde un Activity/Compose.
     */
    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /**
     * Lee los pasos totales agregados en el día de hoy (desde la medianoche).
     */
    suspend fun getTodaySteps(): Long = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext 0L

        val now = Instant.now()
        val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )

        try {
            val response = healthConnectClient.aggregate(request)
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * Lee la distancia total recorrida hoy (en metros).
     */
    suspend fun getTodayDistanceMeters(): Double = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext 0.0

        val now = Instant.now()
        val startOfDay = now.truncatedTo(ChronoUnit.DAYS)

        val request = AggregateRequest(
            metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )

        try {
            val response = healthConnectClient.aggregate(request)
            response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }
}
