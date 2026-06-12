package com.georacing.georacing.car.config

import android.util.Log

/**
 * FASE 3.1: Configuración centralizada para endpoints OSRM.
 * 
 * Permite alternar entre:
 * - OSRM público (router.project-osrm.org) para desarrollo rápido
 * - OSRM local (Docker en red local) para producción
 * 
 * Cómo usar servidor OSRM local:
 * 
 * 1. Levantar OSRM con Docker:
 * ```bash
 * # Descargar mapa de España (o tu región)
 * wget http://download.geofabrik.de/europe/spain-latest.osm.pbf
 * 
 * # Procesar el mapa con OSRM
 * docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-extract -p /opt/car.lua /data/spain-latest.osm.pbf
 * docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-partition /data/spain-latest.osrm
 * docker run -t -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-customize /data/spain-latest.osrm
 * 
 * # Levantar servidor (Puerto 5000)
 * docker run -t -i -p 5000:5000 -v "${PWD}:/data" ghcr.io/project-osrm/osrm-backend osrm-routed --algorithm mld /data/spain-latest.osrm
 * ```
 * 
 * 2. Cambiar modo en esta clase:
 * ```kotlin
 * OsrmConfig.setEnvironment(OsrmEnvironment.LOCAL)
 * OsrmConfig.setLocalHost("192.168.1.100", 5000)  // Tu IP local
 * ```
 * 
 * O usar BuildConfig para cambiar en build time:
 * - En build.gradle.kts añadir:
 *   buildConfigField("String", "OSRM_BASE_URL", "\"https://router.project-osrm.org/\"")
 * - Para builds de producción:
 *   buildConfigField("String", "OSRM_BASE_URL", "\"http://tu-servidor-osrm:5000/\"")
 */
object OsrmConfig {
    
    private const val TAG = "OsrmConfig"
    
    /**
     * Entornos OSRM disponibles
     */
    enum class OsrmEnvironment {
        /** OSRM público de Project OSRM (desarrollo) */
        PUBLIC,
        
        /** OSRM local en red (producción/testing) */
        LOCAL
    }
    
    // URLs base
    private const val PUBLIC_BASE_URL = "https://router.project-osrm.org/"
    private var localBaseUrl = "http://192.168.1.100:5000/"  // Default local IP
    
    // Estado actual
    private var currentEnvironment = OsrmEnvironment.PUBLIC
    
    /**
     * Obtiene la URL base actual de OSRM según el entorno configurado.
     */
    fun getBaseUrl(): String {
        val url = when (currentEnvironment) {
            OsrmEnvironment.PUBLIC -> PUBLIC_BASE_URL
            OsrmEnvironment.LOCAL -> localBaseUrl
        }
        
        Log.d(TAG, "OSRM Base URL: $url (Environment: $currentEnvironment)")
        return url
    }
    
    /**
     * Cambia el entorno OSRM (público o local).
     * 
     * @param environment Entorno a usar
     */
    fun setEnvironment(environment: OsrmEnvironment) {
        currentEnvironment = environment
        Log.i(TAG, "OSRM environment changed to: $environment")
    }
    
    /**
     * Configura el host local para OSRM (solo aplica si environment = LOCAL).
     * 
     * @param host IP o hostname del servidor OSRM local (ej: "192.168.1.100" o "osrm.local")
     * @param port Puerto del servidor (default: 5000)
     */
    fun setLocalHost(host: String, port: Int = 5000) {
        localBaseUrl = "http://$host:$port/"
        Log.i(TAG, "OSRM local host configured: $localBaseUrl")
    }
    
    /**
     * Devuelve el entorno actual.
     */
    fun getCurrentEnvironment(): OsrmEnvironment {
        return currentEnvironment
    }
    
    /**
     * Helper para configuración rápida desde BuildConfig (si está disponible).
     * 
     * Ejemplo en build.gradle.kts:
     * ```
     * android {
     *     defaultConfig {
     *         buildConfigField("String", "OSRM_BASE_URL_PUBLIC", "\"https://router.project-osrm.org/\"")
     *         buildConfigField("String", "OSRM_BASE_URL_LOCAL", "\"http://192.168.1.100:5000/\"")
     *         buildConfigField("String", "OSRM_ENVIRONMENT", "\"PUBLIC\"")  // o "LOCAL"
     *     }
     * }
     * ```
     */
    fun configureFromBuildConfig(
        environment: String?,
        publicUrl: String? = null,
        localUrl: String? = null
    ) {
        // Configurar URLs si se proveen
        publicUrl?.let {
            // PUBLIC_BASE_URL es const, no se puede modificar
            Log.d(TAG, "Public URL from BuildConfig: $it")
        }
        
        localUrl?.let {
            localBaseUrl = it
            Log.d(TAG, "Local URL from BuildConfig: $it")
        }
        
        // Configurar entorno
        environment?.let {
            try {
                val env = OsrmEnvironment.valueOf(it.uppercase())
                setEnvironment(env)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid OSRM environment from BuildConfig: $it, using PUBLIC")
                setEnvironment(OsrmEnvironment.PUBLIC)
            }
        }
    }
}
