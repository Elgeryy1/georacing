package com.georacing.georacing.ui.screens.poi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.domain.model.Poi
import com.georacing.georacing.domain.model.PoiType
import com.georacing.georacing.domain.repository.PoiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PoiViewModel(
    poiRepository: PoiRepository
) : ViewModel() {

    private val allPois = poiRepository.getPois()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedType = MutableStateFlow<PoiType?>(null)
    val selectedType = _selectedType.asStateFlow()

    val visiblePois: StateFlow<List<Poi>> = combine(allPois, _selectedType) { pois, type ->
        if (type == null) pois else pois.filter { it.type == type }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterByType(type: PoiType?) {
        _selectedType.value = type
    }
}
