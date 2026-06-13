package com.georacing.georacing.domain.model

/**
 * Represents the current state of a live navigation/emergency session.
 * Used by LiveSessionService to drive notification updates.
 */
sealed class LiveSessionState {
    
    /** Default state - no active session. */
    object Idle : LiveSessionState()
    
    /** Active navigation towards a destination. */
    data class Navigation(
        val distanceMeters: Int,
        val instruction: String,
        val progress: Int = 0 // 0-100
    ) : LiveSessionState()
    
    /** Emergency/Evacuation mode. */
    data class Emergency(
        val type: EmergencyType,
        val exitRoute: String,
        val message: String = ""
    ) : LiveSessionState()
}

enum class EmergencyType {
    EVACUATION,
    RED_FLAG,
    MEDICAL,
    SECURITY
}
