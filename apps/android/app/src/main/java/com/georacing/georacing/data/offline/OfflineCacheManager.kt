package com.georacing.georacing.data.offline

import com.georacing.georacing.domain.model.CircuitState
import com.georacing.georacing.domain.model.CircuitMode

interface OfflineCacheManager {
    fun cacheCircuitState(state: CircuitState)
    fun getCachedCircuitState(): CircuitState?
    fun cacheStaticMapData(data: String)
    fun getStaticMapData(): String?
}

class InMemoryOfflineCacheManager : OfflineCacheManager {
    private var circuitState: CircuitState? = CircuitState(CircuitMode.NORMAL, "Disfruta de la carrera", null, "")
    private var staticMapData: String? = null

    override fun cacheCircuitState(state: CircuitState) { circuitState = state }
    override fun getCachedCircuitState(): CircuitState? = circuitState
    override fun cacheStaticMapData(data: String) { staticMapData = data }
    override fun getStaticMapData(): String? = staticMapData
}
