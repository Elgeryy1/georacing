package com.georacing.georacing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.georacing.georacing.data.local.entities.TelemetryEntity

/**
 * Data Access Object para los eventos de la Caja Negra.
 */
@Dao
interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryEntity)

    @Query("SELECT * FROM telemetry_logs ORDER BY timestamp ASC")
    suspend fun getAllLogs(): List<TelemetryEntity>

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearLogs()
}
