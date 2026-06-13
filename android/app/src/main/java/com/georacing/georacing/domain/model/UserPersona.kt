package com.georacing.georacing.domain.model

enum class UserType {
    FAN, FAMILY, VIP, STAFF
}

enum class TransportMethod {
    CAR, PUBLIC_TRANSPORT, WALKING
}

enum class Interest {
    RACING, FOOD, EVENTS, TECH
}

enum class F1Team(val displayName: String, val color: Long) {
    RED_BULL("Red Bull Racing", 0xFF1E41FF),
    FERRARI("Ferrari", 0xFFDC0000),
    MCLAREN("McLaren", 0xFFFF8700),
    MERCEDES("Mercedes-AMG", 0xFF00D2BE),
    ASTON_MARTIN("Aston Martin", 0xFF006F62),
    ALPINE("Alpine", 0xFF0090FF),
    WILLIAMS("Williams", 0xFF005AFF),
    HAAS("Haas", 0xFFB6BABD),
    RB_VISA("RB Visa Cash App", 0xFF2B4562),
    SAUBER("Sauber / Audi", 0xFF52E252),
    NONE("Sin equipo favorito", 0xFF64748B)
}

data class OnboardingAnswers(
    val userType: UserType?,
    val transportMethod: TransportMethod?,
    val interests: List<Interest> = emptyList(),
    val favoriteTeam: F1Team = F1Team.NONE,
    val needsAccessibility: Boolean = false
)
