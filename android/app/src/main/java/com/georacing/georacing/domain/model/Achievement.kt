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
    val achievements: List<Achievement> = emptyList(),
    val circuitsVisited: Int = 1,
    val kmWalked: Float = 0f,
    val friendsInGroup: Int = 0
) {
    /**
     * Current level, derived solely from [totalXP] so it can never drift out of
     * sync with the experience the player has actually earned.
     *
     * Levels use a quadratic curve where level `n` begins at `50 * n * (n - 1)` XP:
     * L1 → 0, L2 → 100, L3 → 300, L4 → 600, L5 → 1000, ...
     * Each level is therefore wider than the last, which is the conventional shape
     * for a fan-progression ladder.
     */
    val level: Int
        get() = xpToLevel(totalXP)

    val levelName: String
        get() = when {
            level >= 20 -> "Leyenda del Circuito"
            level >= 15 -> "Piloto de Élite"
            level >= 10 -> "Veterano de Paddock"
            level >= 7 -> "Fan Apasionado"
            level >= 5 -> "Copiloto"
            level >= 2 -> "Aficionado"
            else -> "Novato" // Only level 1 (0-99 XP)
        }

    /** Total XP required to reach the start of the next level. */
    val xpForNextLevel: Int get() = levelStartXp(level + 1)

    /** XP banked toward the current level, i.e. since the current level began. */
    val xpIntoCurrentLevel: Int get() = totalXP - levelStartXp(level)

    /** Progress through the current level, clamped to the inclusive range [0, 1]. */
    val xpProgress: Float
        get() {
            val span = xpForNextLevel - levelStartXp(level)
            if (span <= 0) return 1f
            return (xpIntoCurrentLevel.toFloat() / span).coerceIn(0f, 1f)
        }

    companion object {
        /** First XP value that belongs to [level] (level 1 starts at 0). */
        fun levelStartXp(level: Int): Int {
            val n = level.coerceAtLeast(1)
            return 50 * n * (n - 1)
        }

        /**
         * Highest level whose [levelStartXp] is still <= [xp]. Implemented as a
         * forward scan (levels are unbounded but grow quadratically, so the loop
         * is short even for very large XP totals) to stay exact and avoid the
         * rounding pitfalls of a floating-point closed form.
         */
        fun xpToLevel(xp: Int): Int {
            if (xp <= 0) return 1
            var level = 1
            while (levelStartXp(level + 1) <= xp) {
                level++
            }
            return level
        }
    }
}
