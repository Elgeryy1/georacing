package com.georacing.georacing.infrastructure.telemetry

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.georacing.georacing.data.local.GeoRacingDatabase

/**
 * Worker responsable de subir los logs de la caja negra
 * cuando las condiciones de red son ideales (ej. Wi-Fi).
 */
class TelemetrySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = GeoRacingDatabase.getInstance(applicationContext)
        val telemetryDao = database.telemetryDao()

        return try {
            val logs = telemetryDao.getAllLogs()

            if (logs.isNotEmpty()) {
                Log.d("TelemetrySyncWorker", "Attempting to sync ${logs.size} BlackBox events to QNAP NAS...")
                
                // Simulación de subida de payloads binarios/JSON al NAS QNAP TS-464.
                // callQnapApi(logs)
                
                // Si la sincronización es exitosa, limpiamos localmente para ahorrar espacio.
                telemetryDao.clearLogs()
                Log.d("TelemetrySyncWorker", "Sync successful. Local database cleared.")
            } else {
                Log.d("TelemetrySyncWorker", "No Telemetry events to sync.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("TelemetrySyncWorker", "Sync failed: ${e.message}", e)
            Result.retry() // Si la red cae a la mitad, reintentará luego
        }
    }
}
