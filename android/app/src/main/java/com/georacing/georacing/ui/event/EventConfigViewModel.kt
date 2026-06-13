package com.georacing.georacing.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.event.ActiveEventConfig
import com.georacing.georacing.data.event.EventConfigRepository
import com.georacing.georacing.data.event.EventConfigSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventConfigUiState(
    val activeEvent: ActiveEventConfig = ActiveEventConfig.fallback(),
    val isRefreshing: Boolean = false,
    val source: EventConfigSource = EventConfigSource.DEFAULT,
    val lastSuccessfulSyncMs: Long? = null,
    val errorMessage: String? = null
)

class EventConfigViewModel(
    private val repository: EventConfigRepository
) : ViewModel() {

    val uiState: StateFlow<EventConfigUiState> = combine(
        repository.activeEventConfig,
        repository.syncState
    ) { activeEvent, syncState ->
        EventConfigUiState(
            activeEvent = activeEvent,
            isRefreshing = syncState.isRefreshing,
            source = syncState.source,
            lastSuccessfulSyncMs = syncState.lastSuccessfulSyncMs,
            errorMessage = syncState.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventConfigUiState(
            activeEvent = repository.activeEventConfig.value,
            isRefreshing = repository.syncState.value.isRefreshing,
            source = repository.syncState.value.source,
            lastSuccessfulSyncMs = repository.syncState.value.lastSuccessfulSyncMs,
            errorMessage = repository.syncState.value.errorMessage
        )
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }
}
