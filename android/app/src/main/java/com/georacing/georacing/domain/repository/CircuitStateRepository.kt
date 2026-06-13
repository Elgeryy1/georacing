package com.georacing.georacing.domain.repository

import com.georacing.georacing.domain.model.CircuitState
import kotlinx.coroutines.flow.Flow

interface CircuitStateRepository {
    fun getCircuitState(): Flow<CircuitState>
    fun setCircuitState(mode: com.georacing.georacing.domain.model.CircuitMode, message: String?)
    
    // Hybrid Mode Support
    val appMode: Flow<com.georacing.georacing.domain.model.AppMode>
    
    // Debug info for connectivity troubleshooting
    val debugInfo: Flow<String>
}
