package com.georacing.georacing.data.local.mappers

import com.georacing.georacing.data.local.entities.BeaconEntity
import com.georacing.georacing.data.local.entities.CircuitStateEntity
import com.georacing.georacing.data.local.entities.PoiEntity
import com.georacing.georacing.data.remote.dto.BeaconConfigDto
import com.georacing.georacing.data.remote.dto.CircuitStateDto
import com.georacing.georacing.data.remote.dto.PoiDto
import com.georacing.georacing.domain.model.ArrowDirection
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.model.BeaconMode
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType

// =============================================================================
// POI MAPPERS
// =============================================================================

/**
 * DTO -> Entity (Network to Local Cache)
 */
fun PoiDto.toEntity(): PoiEntity = PoiEntity(
    id = id,
    name = name,
    type = type,
    description = description,
    zone = zone,
    mapX = mapX,
    mapY = mapY,
    latitude = 0.0,  // Not in DTO, will be enriched later if needed
    longitude = 0.0
)

/**
 * Entity -> Domain (Local Cache to UI)
 */
fun PoiEntity.toDomain(): Poi = Poi(
    id = id,
    name = name,
    type = type.toPoiType(),
    description = description,
    zone = zone,
    mapX = mapX,
    mapY = mapY,
    latitude = latitude,
    longitude = longitude
)

/**
 * Domain -> Entity (for manual inserts if needed)
 */
fun Poi.toEntity(): PoiEntity = PoiEntity(
    id = id,
    name = name,
    type = type.name,
    description = description,
    zone = zone,
    mapX = mapX,
    mapY = mapY,
    latitude = latitude,
    longitude = longitude
)

private fun String.toPoiType(): PoiType = try {
    PoiType.valueOf(this.uppercase())
} catch (e: Exception) {
    PoiType.OTHER
}

// =============================================================================
// CIRCUIT STATE MAPPERS
// =============================================================================

/**
 * DTO -> Entity (Network to Local Cache)
 */
fun CircuitStateDto.toEntity(): CircuitStateEntity = CircuitStateEntity(
    id = CircuitStateEntity.SINGLETON_ID,
    mode = (global_mode ?: mode) ?: "UNKNOWN",
    message = message,
    temperature = temperature,
    humidity = humidity,
    wind = wind,
    forecast = forecast,
    updatedAt = last_updated ?: ""
)

/**
 * Entity -> Domain (Local Cache to UI)
 */
/**
 * Entity -> Domain (Local Cache to UI)
 */
fun CircuitStateEntity.toDomain(): CircuitState = CircuitState(
    mode = mode.toCircuitMode(),
    message = message,
    temperature = temperature,
    updatedAt = updatedAt,
    humidity = humidity,
    wind = wind,
    forecast = forecast,
    sessionInfo = null
)

/**
 * Domain -> Entity (for manual updates)
 */
fun CircuitState.toEntity(): CircuitStateEntity = CircuitStateEntity(
    id = CircuitStateEntity.SINGLETON_ID,
    mode = mode.name,
    message = message,
    temperature = temperature,
    humidity = humidity,
    wind = wind,
    forecast = forecast,
    updatedAt = updatedAt
)

private fun String.toCircuitMode(): CircuitMode = when (this.uppercase()) {
    "NORMAL" -> CircuitMode.NORMAL
    "GREEN_FLAG", "GREEN" -> CircuitMode.GREEN_FLAG
    "YELLOW_FLAG", "YELLOW" -> CircuitMode.YELLOW_FLAG
    "SAFETY_CAR", "SC" -> CircuitMode.SAFETY_CAR
    "VSC" -> CircuitMode.VSC
    "RED_FLAG", "RED" -> CircuitMode.RED_FLAG
    "EVACUATION" -> CircuitMode.EVACUATION
    else -> CircuitMode.UNKNOWN
}

// =============================================================================
// BEACON CONFIG MAPPERS
// =============================================================================

/**
 * DTO -> Entity (Network to Local Cache)
 */
fun BeaconConfigDto.toEntity(): BeaconEntity = BeaconEntity(
    id = id ?: beaconUid ?: "",
    beaconUid = beaconUid ?: id ?: "",
    name = name ?: "",
    zone = zone ?: (zoneId?.toString() ?: ""),
    mapX = mapX ?: latitude?.toFloat() ?: 0f,
    mapY = mapY ?: longitude?.toFloat() ?: 0f,
    latitude = latitude ?: mapX?.toDouble() ?: 0.0,
    longitude = longitude ?: mapY?.toDouble() ?: 0.0,
    messageNormal = messageNormal ?: message ?: "",
    messageEmergency = messageEmergency ?: message ?: "",
    message = message ?: messageNormal ?: "",
    arrowDirection = arrowDirection ?: "NONE",
    mode = mode ?: "NORMAL",
    color = color ?: "#00FF00",
    brightness = brightness ?: 100,
    batteryLevel = batteryLevel ?: 100,
    isOnline = isOnline ?: true,
    hasScreen = hasScreen ?: false
)

/**
 * Entity -> Domain (Local Cache to UI)
 */
fun BeaconEntity.toDomain(): BeaconConfig = BeaconConfig(
    id = id,
    beaconUid = beaconUid,
    name = name,
    zone = zone,
    mapX = mapX,
    mapY = mapY,
    latitude = latitude,
    longitude = longitude,
    messageNormal = messageNormal,
    messageEmergency = messageEmergency,
    message = message,
    arrowDirection = arrowDirection.toArrowDirection(),
    mode = mode.toBeaconMode(),
    color = color,
    brightness = brightness,
    batteryLevel = batteryLevel,
    isOnline = isOnline,
    hasScreen = hasScreen
)

/**
 * Domain -> Entity (for manual inserts)
 */
fun BeaconConfig.toEntity(): BeaconEntity = BeaconEntity(
    id = id,
    beaconUid = beaconUid,
    name = name,
    zone = zone,
    mapX = mapX,
    mapY = mapY,
    latitude = latitude,
    longitude = longitude,
    messageNormal = messageNormal,
    messageEmergency = messageEmergency,
    message = message,
    arrowDirection = arrowDirection.name,
    mode = mode.name,
    color = color,
    brightness = brightness,
    batteryLevel = batteryLevel,
    isOnline = isOnline,
    hasScreen = hasScreen
)

private fun String.toArrowDirection(): ArrowDirection = try {
    ArrowDirection.valueOf(this.uppercase())
} catch (e: Exception) {
    ArrowDirection.NONE
}

private fun String.toBeaconMode(): BeaconMode = try {
    BeaconMode.valueOf(this.uppercase())
} catch (e: Exception) {
    BeaconMode.NORMAL
}

// =============================================================================
// BATCH MAPPERS (for list operations)
// =============================================================================

fun List<PoiDto>.toEntities(): List<PoiEntity> = map { it.toEntity() }
fun List<PoiEntity>.toDomainPois(): List<Poi> = map { it.toDomain() }

fun List<BeaconConfigDto>.toBeaconEntities(): List<BeaconEntity> = map { it.toEntity() }
fun List<BeaconEntity>.toDomainBeacons(): List<BeaconConfig> = map { it.toDomain() }
