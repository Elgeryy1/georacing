package com.georacing.georacing.domain.model

/**
 * Sistema de gamificación — Logros y badges del Circuit de Catalunya.
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val category: AchievementCategory,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val unlockedAt: Long? = null
)

enum class AchievementCategory(val displayName: String, val emoji: String) {
    EXPLORER("Explorador", "🗺️"),
    SOCIAL("Social", "👥"),
    SPEED("Velocidad", "⚡"),
    FAN("Superfan", "🏆"),
    ECO("Eco", "🌿"),
    SAFETY("Seguridad", "🛡️")
}

data class FanProfile(
    val totalXP: Int = 0,
    val level: Int = 1,
    val achievements: List<Achievement> = emptyList(),
    val circuitsVisited: Int = 1,
    val kmWalked: Float = 0f,
    val friendsInGroup: Int = 0
) {
    val levelName: String
        get() = when {
            level >= 20 -> "Leyenda del Circuito"
            level >= 15 -> "Piloto de Élite"
            level >= 10 -> "Veterano de Paddock"
            level >= 7 -> "Fan Apasionado"
            level >= 5 -> "Copiloto"
            level >= 3 -> "Aficionado"
            else -> "Novato"
        }

    val xpForNextLevel: Int get() = level * 250
    val xpProgress: Float get() = (totalXP % xpForNextLevel).toFloat() / xpForNextLevel
}
