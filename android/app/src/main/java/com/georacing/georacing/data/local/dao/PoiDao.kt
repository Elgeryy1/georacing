package com.georacing.georacing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.georacing.georacing.data.local.entities.PoiEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para Puntos de Interés (POIs).
 * Proporciona acceso reactivo a la tabla 'pois' usando Flow.
 */
@Dao
interface PoiDao {

    /**
     * Observa todos los POIs en la base de datos.
     * Emite automáticamente cuando hay cambios (INSERT, UPDATE, DELETE).
     */
    @Query("SELECT * FROM pois ORDER BY zone, name")
    fun getAllPois(): Flow<List<PoiEntity>>

    /**
     * Obtiene POIs filtrados por tipo.
     */
    @Query("SELECT * FROM pois WHERE type = :type ORDER BY name")
    fun getPoisByType(type: String): Flow<List<PoiEntity>>

    /**
     * Obtiene un POI específico por ID.
     */
    @Query("SELECT * FROM pois WHERE id = :poiId LIMIT 1")
    suspend fun getPoiById(poiId: String): PoiEntity?

    /**
     * Inserta o actualiza una lista de POIs.
     * REPLACE: Si el ID ya existe, actualiza la fila completa.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPois(pois: List<PoiEntity>)

    /**
     * Inserta o actualiza un solo POI.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoi(poi: PoiEntity)

    /**
     * Elimina todos los POIs (limpia caché).
     */
    @Query("DELETE FROM pois")
    suspend fun deleteAll()

    /**
     * Cuenta el número de POIs en caché.
     */
    @Query("SELECT COUNT(*) FROM pois")
    suspend fun count(): Int
}
