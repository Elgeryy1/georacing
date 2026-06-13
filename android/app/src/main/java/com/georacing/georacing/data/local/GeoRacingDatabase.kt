package com.georacing.georacing.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.georacing.georacing.data.local.dao.BeaconDao
import com.georacing.georacing.data.local.dao.CircuitStateDao
import com.georacing.georacing.data.local.dao.MedicalInfoDao
import com.georacing.georacing.data.local.dao.PoiDao
import com.georacing.georacing.data.local.entities.BeaconEntity
import com.georacing.georacing.data.local.entities.CircuitStateEntity
import com.georacing.georacing.data.local.entities.MedicalInfoEntity
import com.georacing.georacing.data.local.entities.PoiEntity
import com.georacing.georacing.data.local.entities.TelemetryEntity

/**
 * Base de Datos principal de GeoRacing (Room).
 * 
 * Implementa el patrón Singleton para garantizar una única instancia.
 * Contiene las tablas para POIs, Estado del Circuito, Beacons e Info Médica.
 * 
 * Uso:
 * ```
 * val database = GeoRacingDatabase.getInstance(context)
 * val pois = database.poiDao().getAllPois()
 * ```
 */
@Database(
    entities = [
        PoiEntity::class,
        CircuitStateEntity::class,
        BeaconEntity::class,
        MedicalInfoEntity::class,
        com.georacing.georacing.data.local.entities.IncidentEntity::class, // 🆕
        TelemetryEntity::class // Phase 5 BlackBox
    ],
    version = 4, // 🆕 Incremented for Telemetry
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GeoRacingDatabase : RoomDatabase() {

    // =========================================================================
    // DAOs
    // =========================================================================
    
    abstract fun poiDao(): PoiDao
    abstract fun circuitStateDao(): CircuitStateDao
    abstract fun beaconDao(): BeaconDao
    abstract fun medicalInfoDao(): MedicalInfoDao
    abstract fun incidentDao(): com.georacing.georacing.data.local.dao.IncidentDao // 🆕 
    abstract fun telemetryDao(): com.georacing.georacing.data.local.dao.TelemetryDao 


    // =========================================================================
    // Singleton Pattern
    // =========================================================================
    
    companion object {
        private const val DATABASE_NAME = "georacing_db"

        @Volatile
        private var INSTANCE: GeoRacingDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos.
         * Thread-safe usando double-checked locking.
         * 
         * @param context Application context (se usa applicationContext internamente)
         * @return Instancia singleton de GeoRacingDatabase
         */
        fun getInstance(context: Context): GeoRacingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context): GeoRacingDatabase {
            return Room.databaseBuilder(
                context,
                GeoRacingDatabase::class.java,
                DATABASE_NAME
            )
                // Estrategia de migración: destruir y recrear si hay cambio de schema
                // En producción, usar migraciones apropiadas
                .fallbackToDestructiveMigration()
                // Permitir queries en main thread solo para debugging
                // .allowMainThreadQueries() // NO USAR en producción
                .build()
        }

        /**
         * Cierra la base de datos (útil para tests).
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
