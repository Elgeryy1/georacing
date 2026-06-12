package com.georacing.georacing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.georacing.georacing.data.local.entities.CircuitStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para el Estado Global del Circuito.
 * Maneja una tabla singleton (1 sola fila, ID=1).
 */
@Dao
interface CircuitStateDao {

    /**
     * Observa el estado actual del circuito.
     * Emite automáticamente cuando cambia el modo, clima, etc.
     */
    @Query("SELECT * FROM circuit_state WHERE id = 1 LIMIT 1")
    fun getState(): Flow<CircuitStateEntity?>

    /**
     * Obtiene el estado de forma síncrona (para uso puntual).
     */
    @Query("SELECT * FROM circuit_state WHERE id = 1 LIMIT 1")
    suspend fun getStateOnce(): CircuitStateEntity?

    /**
     * Inserta o actualiza el estado del circuito.
     * Como siempre usamos ID=1, REPLACE garantiza que solo haya 1 fila.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertState(state: CircuitStateEntity)

    /**
     * Elimina el estado (reset de caché).
     */
    @Query("DELETE FROM circuit_state")
    suspend fun deleteState()

    /**
     * Verifica si hay estado en caché.
     */
    @Query("SELECT COUNT(*) FROM circuit_state")
    suspend fun hasState(): Int
}
