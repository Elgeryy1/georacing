package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.remote.RetrofitClient
import com.georacing.georacing.data.remote.dto.toDomain
import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.repository.CircuitStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive

class NetworkCircuitStateRepository : CircuitStateRepository {

    companion object {
        private const val TAG = "NetworkCircuitStateRepo"
        private const val POLL_INTERVAL_MS = 5_000L
        private val DEFAULT_STATE = CircuitState(
            mode = CircuitMode.UNKNOWN,
            message = "Conexion inestable",
            temperature = null,
            updatedAt = ""
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val sharedState = flow {
        while (currentCoroutineContext().isActive) {
            try {
                val stateDto = RetrofitClient.api.getCircuitState()
                emit(stateDto.toDomain())
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching circuit state", e)
                emit(DEFAULT_STATE)
            }
            delay(POLL_INTERVAL_MS)
        }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DEFAULT_STATE
        )

    override fun getCircuitState(): Flow<CircuitState> = sharedState

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        // No-op for network repo client-side usually, or impl existing logic
    }

    override val appMode: Flow<com.georacing.georacing.domain.model.AppMode> =
        flowOf(com.georacing.georacing.domain.model.AppMode.ONLINE)

    override val debugInfo: Flow<String> = flowOf("Network")
}
