package com.georacing.georacing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.georacing.georacing.data.local.entities.BeaconEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para Configuración de Beacons BLE.
 * Cachea los datos de las balizas del circuito.
 */
@Dao
interface BeaconDao {

    /**
     * Observa todos los beacons.
     * La UI puede reaccionar automáticamente a cambios.
     */
    @Query("SELECT * FROM beacons ORDER BY zone, name")
    fun getAllBeacons(): Flow<List<BeaconEntity>>

    /**
     * Obtiene beacons filtrados por zona.
     */
    @Query("SELECT * FROM beacons WHERE zone = :zone ORDER BY name")
    fun getBeaconsByZone(zone: String): Flow<List<BeaconEntity>>

    /**
     * Obtiene un beacon por ID.
     */
    @Query("SELECT * FROM beacons WHERE id = :beaconId LIMIT 1")
    suspend fun getBeaconById(beaconId: String): BeaconEntity?

    /**
     * Inserta o actualiza una lista de beacons.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacons(beacons: List<BeaconEntity>)

    /**
     * Inserta o actualiza un solo beacon.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeacon(beacon: BeaconEntity)

    /**
     * Elimina todos los beacons (limpia caché).
     */
    @Query("DELETE FROM beacons")
    suspend fun deleteAll()

    /**
     * Cuenta el número de beacons en caché.
     */
    @Query("SELECT COUNT(*) FROM beacons")
    suspend fun count(): Int
}
