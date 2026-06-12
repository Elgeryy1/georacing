package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.toDomain
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.repository.CircuitStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

class NetworkCircuitStateRepository : CircuitStateRepository {
    override fun getCircuitState(): Flow<CircuitState> = flow {
        while (true) {
            try {
                val stateDto = RetrofitClient.api.getCircuitState()
                emit(stateDto.toDomain())
            } catch (e: Exception) {
                Log.e("NetworkCircuitStateRepo", "Error fetching circuit state", e)
                // Emit a default safe state or keep previous if possible (simplified here)
                emit(CircuitState(CircuitMode.UNKNOWN, "Conexión Inestable", null, ""))
            }
            delay(5000) // Poll every 5 seconds
        }
    }

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        // No-op for network repo client-side usually, or impl existing logic
    }

    override val appMode: Flow<com.georacing.georacing.domain.model.AppMode> = kotlinx.coroutines.flow.flowOf(com.georacing.georacing.domain.model.AppMode.ONLINE)
    override val debugInfo: Flow<String> = kotlinx.coroutines.flow.flowOf("Network")
}
