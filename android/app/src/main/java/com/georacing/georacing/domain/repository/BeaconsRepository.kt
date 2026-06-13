package com.georacing.georacing.domain.repository

import com.georacing.georacing.domain.model.BeaconConfig
import kotlinx.coroutines.flow.Flow

interface BeaconsRepository {
    fun getBeacons(): Flow<List<BeaconConfig>>
}
