package com.georacing.georacing.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

/**
 * Industrial-grade Health Connect manager for on-demand step reading.
 * 
 * Design Principles:
 * - Energy-First: No background listeners, only on-demand reads.
 * - Graceful Degradation: Returns 0 if unavailable/denied.
 */
open class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectManager"
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class)
        )

        fun getPermissionRequestContract() = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    }

    data class DailyMetrics(
        val steps: Long = 0,
        val distanceMeters: Double = 0.0
    )

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (isAvailable()) {
                HealthConnectClient.getOrCreate(context)
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect client creation failed", e)
            null
        }
    }

    /**
     * Checks if Health Connect is available on this device.
     */
    open fun isAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.w(TAG, "Health Connect availability check failed", e)
            false
        }
    }

    /**
     * Checks if required permissions are granted.
     */
    open suspend fun hasPermissions(): Boolean {
        return try {
            val client = healthConnectClient ?: return false
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(PERMISSIONS)
        } catch (e: Exception) {
            Log.w(TAG, "Permission check failed", e)
            false
        }
    }

    /**
     * Reads today's total steps and distance on-demand.
     * @param startTime Optional start time to filter metrics (e.g., circuit arrival time)
     */
    open suspend fun readDailyMetrics(startTime: Instant? = null): DailyMetrics {
        return try {
            if (!isAvailable() || !hasPermissions()) {
                return DailyMetrics()
            }

            val client = healthConnectClient ?: return DailyMetrics()

            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            // Use provided startTime if it's after startOfDay, otherwise fallback to startOfDay
            val filterStart = if (startTime != null && startTime.isAfter(startOfDay)) startTime else startOfDay

            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(filterStart, endOfDay)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val distance = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0

            Log.d(TAG, "Metrics: Steps=$steps, Dist=$distance")
            DailyMetrics(steps, distance)

        } catch (e: Exception) {
            Log.e(TAG, "Error reading daily metrics", e)
            DailyMetrics()
        }
    }
}
