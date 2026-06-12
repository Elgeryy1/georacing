package com.georacing.georacing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.georacing.georacing.data.local.entities.MedicalInfoEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para Información Médica del Usuario.
 * Maneja una tabla singleton (1 sola fila, ID=1).
 */
@Dao
interface MedicalInfoDao {

    /**
     * Observa la información médica del usuario.
     * Emite automáticamente cuando se actualiza.
     */
    @Query("SELECT * FROM medical_info WHERE id = 1 LIMIT 1")
    fun getMedicalInfo(): Flow<MedicalInfoEntity?>

    /**
     * Obtiene la información médica de forma síncrona.
     */
    @Query("SELECT * FROM medical_info WHERE id = 1 LIMIT 1")
    suspend fun getMedicalInfoOnce(): MedicalInfoEntity?

    /**
     * Inserta o actualiza la información médica.
     * Como siempre usamos ID=1, REPLACE garantiza solo 1 fila.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMedicalInfo(info: MedicalInfoEntity)

    /**
     * Verifica si hay información médica guardada.
     */
    @Query("SELECT COUNT(*) FROM medical_info")
    suspend fun hasMedicalInfo(): Int

    /**
     * Elimina la información médica (reset).
     */
    @Query("DELETE FROM medical_info")
    suspend fun deleteMedicalInfo()
}
