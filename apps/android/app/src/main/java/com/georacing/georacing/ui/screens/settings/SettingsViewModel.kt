package com.georacing.georacing.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.local.UserPreferencesDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.georacing.georacing.data.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val userPreferences: UserPreferencesDataStore,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _healthConnectPermissionsGranted = MutableStateFlow(false)
    val healthConnectPermissionsGranted = _healthConnectPermissionsGranted.asStateFlow()

    init {
        checkHealthConnectPermissions()
    }

    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            _healthConnectPermissionsGranted.value = healthConnectManager.hasPermissions()
        }
    }

    fun isHealthConnectAvailable(): Boolean {
        return healthConnectManager.isAvailable()
    }

    val preferredLanguage: StateFlow<String> = userPreferences.preferredLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "es")

    val highContrast: StateFlow<Boolean> = userPreferences.highContrast
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val largeFont: StateFlow<Boolean> = userPreferences.largeFont
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val avoidStairs: StateFlow<Boolean> = userPreferences.avoidStairs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setLanguage(language: String) {
        viewModelScope.launch {
            userPreferences.setPreferredLanguage(language)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(false)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setHighContrast(enabled) }
    }

    fun setLargeFont(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setLargeFont(enabled) }
    }

    fun setAvoidStairs(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAvoidStairs(enabled) }
    }
}
