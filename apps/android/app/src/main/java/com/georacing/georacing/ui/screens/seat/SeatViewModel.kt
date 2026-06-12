package com.georacing.georacing.ui.screens.seat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.domain.model.SeatInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SeatViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    val seatInfo: StateFlow<SeatInfo?> = userPreferences.seatInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveSeat(grandstand: String, zone: String, row: String, seat: String) {
        viewModelScope.launch {
            val info = SeatInfo(grandstand, zone, row, seat)
            userPreferences.setSeatInfo(info)
        }
    }
}
