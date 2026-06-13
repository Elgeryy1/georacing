package com.georacing.georacing.data.offline

import com.georacing.georacing.domain.model.CircuitState

class OfflineRepository(private val cache: OfflineCacheManager) {
    fun saveCircuitState(state: CircuitState) = cache.cacheCircuitState(state)
    fun getCircuitState(): CircuitState? = cache.getCachedCircuitState()
    fun saveMap(data: String) = cache.cacheStaticMapData(data)
    fun getMap(): String? = cache.getStaticMapData()
}
