package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.toDomain
import com.georacing.georacing.domain.model.BeaconConfig
import com.georacing.georacing.domain.repository.BeaconsRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NetworkBeaconsRepository : BeaconsRepository {
    override fun getBeacons(): Flow<List<BeaconConfig>> = flow {
        try {
            val beacons = RetrofitClient.api.getBeacons().map { it.toDomain() }
            emit(beacons)
        } catch (e: Exception) {
            Log.e("NetworkBeaconsRepo", "Error fetching beacons", e)
            emit(emptyList())
        }
    }
}
