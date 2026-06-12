package com.georacing.georacing.data.repository

import com.georacing.georacing.domain.model.CircuitMode
import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.repository.CircuitStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCircuitStateRepository : CircuitStateRepository {
    private val stateFlow = MutableStateFlow(CircuitState(CircuitMode.NORMAL, "Disfruta de la carrera", null, ""))

    override fun getCircuitState(): Flow<CircuitState> = stateFlow.asStateFlow()

    override fun setCircuitState(mode: CircuitMode, message: String?) {
        stateFlow.value = CircuitState(mode, message, null, "")
    }

    override val appMode: Flow<com.georacing.georacing.domain.model.AppMode> = kotlinx.coroutines.flow.flowOf(com.georacing.georacing.domain.model.AppMode.ONLINE)
    override val debugInfo: Flow<String> = kotlinx.coroutines.flow.flowOf("Fake Debug")
}
