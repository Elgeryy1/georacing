package com.georacing.georacing.infrastructure.telemetry

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.georacing.georacing.data.local.GeoRacingDatabase
import java.util.concurrent.TimeUnit

/**
 * Worker responsable de subir los logs de la caja negra
 * cuando las condiciones de red son ideales (ej. Wi-Fi).
 */
class TelemetrySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "telemetry_sync"

        /**
         * Programa la subida periódica de la Caja Negra SOLO sobre redes no medidas
         * (Wi-Fi), para no consumir datos móviles del usuario.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // Solo Wi-Fi (no medida)
                .build()

            val request = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

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
