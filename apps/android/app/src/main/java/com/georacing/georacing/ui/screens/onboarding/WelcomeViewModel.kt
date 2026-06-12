package com.georacing.georacing.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// import com.georacing.georacing.data.repository.LayoutPreferencesRepository // Replaced by UserPreferencesDataStore as per context
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.domain.model.Interest
import com.georacing.georacing.domain.model.F1Team
import com.georacing.georacing.domain.model.OnboardingAnswers
import com.georacing.georacing.domain.model.TransportMethod
import com.georacing.georacing.domain.model.UserType
import com.georacing.georacing.domain.usecases.ProfileGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val userPreferences: UserPreferencesDataStore
) : ViewModel() {

    private val profileGenerator = ProfileGenerator()

    // State
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _userType = MutableStateFlow<UserType?>(null)
    val userType: StateFlow<UserType?> = _userType.asStateFlow()

    private val _transportMethod = MutableStateFlow<TransportMethod?>(null)
    val transportMethod: StateFlow<TransportMethod?> = _transportMethod.asStateFlow()

    private val _selectedInterests = MutableStateFlow<List<Interest>>(emptyList())
    val selectedInterests: StateFlow<List<Interest>> = _selectedInterests.asStateFlow()

    private val _favoriteTeam = MutableStateFlow(F1Team.NONE)
    val favoriteTeam: StateFlow<F1Team> = _favoriteTeam.asStateFlow()

    private val _needsAccessibility = MutableStateFlow(false)
    val needsAccessibility: StateFlow<Boolean> = _needsAccessibility.asStateFlow()

    private val _isConfiguring = MutableStateFlow(false)
    val isConfiguring: StateFlow<Boolean> = _isConfiguring.asStateFlow()
    
    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun selectUserType(type: UserType) {
        _userType.value = type
        nextStep()
    }

    fun selectTransport(method: TransportMethod) {
        _transportMethod.value = method
        nextStep()
    }

    fun toggleInterest(interest: Interest) {
        val currentList = _selectedInterests.value.toMutableList()
        if (currentList.contains(interest)) {
            currentList.remove(interest)
        } else {
            currentList.add(interest)
        }
        _selectedInterests.value = currentList
    }

    fun selectFavoriteTeam(team: F1Team) {
        _favoriteTeam.value = team
        nextStep()
    }

    fun setAccessibility(needs: Boolean) {
        _needsAccessibility.value = needs
    }

    fun nextStep() {
        if (_currentStep.value < 4) {
            _currentStep.value += 1
        }
    }
    
    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }
    
    fun completeQuiz() {
        finishOnboarding()
    }

    private fun finishOnboarding() {
        _isConfiguring.value = true
        viewModelScope.launch {
            // Generate Profile logic
            val answers = OnboardingAnswers(
                userType = _userType.value,
                transportMethod = _transportMethod.value,
                interests = _selectedInterests.value,
                favoriteTeam = _favoriteTeam.value,
                needsAccessibility = _needsAccessibility.value
            )
            val layout = profileGenerator.generateDefaultLayout(answers)

            // Simulate "Configuration" magic and algorithm processing
            delay(2500)

            // Save preferences
            userPreferences.setDashboardLayout(layout.widgets)
            userPreferences.setFavoriteTeam(_favoriteTeam.value.name)
            userPreferences.setAvoidStairs(_needsAccessibility.value)
            userPreferences.setOnboardingCompleted(true)

            _isConfiguring.value = false
            _onboardingComplete.value = true
        }
    }
}
