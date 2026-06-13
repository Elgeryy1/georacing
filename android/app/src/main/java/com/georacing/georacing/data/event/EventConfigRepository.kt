package com.georacing.georacing.data.event

import android.content.Context
import android.util.Log
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface EventConfigRepository {
    val activeEventConfig: StateFlow<ActiveEventConfig>
    val syncState: StateFlow<EventConfigSyncState>

    suspend fun refresh()
}

class DefaultEventConfigRepository(
    context: Context,
    private val api: FirestoreLikeApi = FirestoreLikeClient.api
) : EventConfigRepository {

    companion object {
        private const val TAG = "EventConfigRepository"
        private const val PREFS_NAME = "event_config_cache"
        private const val KEY_CONFIG_JSON = "active_event_json"
        private const val KEY_LAST_SYNC_MS = "last_sync_ms"
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val refreshMutex = Mutex()

    private val cachedConfig = loadCachedConfig()
    private val cachedSyncMs = prefs.getLong(KEY_LAST_SYNC_MS, 0L).takeIf { it > 0L }

    private val _activeEventConfig = MutableStateFlow(cachedConfig ?: ActiveEventConfig.fallback())
    override val activeEventConfig: StateFlow<ActiveEventConfig> = _activeEventConfig.asStateFlow()

    private val _syncState = MutableStateFlow(
        EventConfigSyncState(
            source = when {
                cachedConfig != null -> EventConfigSource.CACHE
                else -> EventConfigSource.DEFAULT
            },
            lastSuccessfulSyncMs = cachedSyncMs
        )
    )
    override val syncState: StateFlow<EventConfigSyncState> = _syncState.asStateFlow()

    override suspend fun refresh() {
        refreshMutex.withLock {
            _syncState.value = _syncState.value.copy(
                isRefreshing = true,
                errorMessage = null
            )

            try {
                val remoteConfig = fetchRemoteActiveEvent()
                val syncTimestamp = System.currentTimeMillis()

                cacheConfig(remoteConfig, syncTimestamp)
                _activeEventConfig.value = remoteConfig
                _syncState.value = EventConfigSyncState(
                    isRefreshing = false,
                    source = EventConfigSource.REMOTE,
                    lastSuccessfulSyncMs = syncTimestamp
                )
            } catch (error: Exception) {
                Log.w(TAG, "Falling back to cached/default event config", error)

                val cachedFallback = loadCachedConfig()
                val fallbackSource = if (cachedFallback != null) {
                    _activeEventConfig.value = cachedFallback
                    EventConfigSource.CACHE
                } else {
                    _activeEventConfig.value = ActiveEventConfig.fallback()
                    EventConfigSource.DEFAULT
                }

                _syncState.value = _syncState.value.copy(
                    isRefreshing = false,
                    source = fallbackSource,
                    errorMessage = error.message
                )
            }
        }
    }

    private suspend fun fetchRemoteActiveEvent(): ActiveEventConfig {
        val preferredEventId = activeEventConfig.value.eventId.takeIf { it.isNotBlank() }
        val failures = mutableListOf<String>()

        for (table in EventConfigParser.candidateTables) {
            val activeQueries = listOf(
                mapOf("is_active" to true),
                mapOf("is_active" to 1),
                mapOf("active" to true),
                mapOf("active" to 1),
                mapOf("status" to "active")
            )

            for (query in activeQueries) {
                try {
                    val queriedRows = api.get(FirestoreLikeApi.GetRequest(table = table, where = query))
                    val parsedConfig = EventConfigParser.parseActiveEvent(queriedRows, preferredEventId)
                    if (parsedConfig != null) {
                        Log.d(TAG, "Loaded active event config from $table using query $query")
                        return parsedConfig
                    }
                } catch (error: Exception) {
                    failures += "$table query $query -> ${error.message}"
                }
            }

            try {
                val allRows = api.read(table)
                val parsedConfig = EventConfigParser.parseActiveEvent(allRows, preferredEventId)
                if (parsedConfig != null) {
                    Log.d(TAG, "Loaded active event config from $table using table read")
                    return parsedConfig
                }
            } catch (error: Exception) {
                failures += "$table read -> ${error.message}"
            }
        }

        throw IllegalStateException(
            "No active event config available from remote sources: ${failures.joinToString(" | ")}"
        )
    }

    private fun cacheConfig(config: ActiveEventConfig, syncedAtMs: Long) {
        prefs.edit()
            .putString(KEY_CONFIG_JSON, gson.toJson(config))
            .putLong(KEY_LAST_SYNC_MS, syncedAtMs)
            .apply()
    }

    private fun loadCachedConfig(): ActiveEventConfig? {
        val rawJson = prefs.getString(KEY_CONFIG_JSON, null) ?: return null
        return try {
            gson.fromJson(rawJson, ActiveEventConfig::class.java)
        } catch (error: Exception) {
            Log.w(TAG, "Invalid cached event config. Ignoring cache.", error)
            null
        }
    }
}
