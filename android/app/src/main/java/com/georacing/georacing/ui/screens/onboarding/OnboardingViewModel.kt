package com.georacing.georacing.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.local.UserPreferencesDataStore
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            userPreferences.setPreferredLanguage(language)
        }
    }
}
