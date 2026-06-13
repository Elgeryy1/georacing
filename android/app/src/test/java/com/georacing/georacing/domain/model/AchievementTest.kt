package com.georacing.georacing.domain.model

import org.junit.Assert.*
import org.junit.Test

class AchievementTest {

    @Test
    fun `FanProfile level calculation returns correct level for XP ranges`() {
        // Level 1: 0-99 XP
        val profile1 = FanProfile(totalXP = 0)
        assertEquals(1, profile1.level)
        
        val profile2 = FanProfile(totalXP = 99)
        assertEquals(1, profile2.level)
        
        // Level 2: 100-299 XP
        val profile3 = FanProfile(totalXP = 100)
        assertEquals(2, profile3.level)
        
        // Level 5: 1000-1499 XP
        val profile5 = FanProfile(totalXP = 1200)
        assertEquals(5, profile5.level)
    }

    @Test
    fun `FanProfile level name changes with level`() {
        val novato = FanProfile(totalXP = 0)
        assertEquals("Novato", novato.levelName)
        
        val iniciado = FanProfile(totalXP = 200)
        assertTrue(iniciado.levelName != "Novato")
    }

    @Test
    fun `FanProfile xpProgress is between 0 and 1`() {
        val profile = FanProfile(totalXP = 150)
        assertTrue(profile.xpProgress >= 0f)
        assertTrue(profile.xpProgress <= 1f)
    }

    @Test
    fun `Achievement default progress is 0`() {
        val achievement = Achievement(
            id = "test",
            title = "Test Achievement",
            description = "For testing",
            emoji = "🏆",
            category = AchievementCategory.EXPLORER,
            xpReward = 50,
            isUnlocked = false
        )
        assertEquals(0f, achievement.progress)
        assertFalse(achievement.isUnlocked)
    }

    @Test
    fun `Achievement progress can be set to partial`() {
        val achievement = Achievement(
            id = "test",
            title = "Test",
            description = "Test",
            emoji = "🏆",
            category = AchievementCategory.SPEED,
            xpReward = 100,
            isUnlocked = false,
            progress = 0.5f
        )
        assertEquals(0.5f, achievement.progress)
    }

    @Test
    fun `AchievementCategory has all expected values`() {
        val categories = AchievementCategory.entries
        assertTrue(categories.contains(AchievementCategory.EXPLORER))
        assertTrue(categories.contains(AchievementCategory.SOCIAL))
        assertTrue(categories.contains(AchievementCategory.SPEED))
        assertTrue(categories.contains(AchievementCategory.FAN))
        assertTrue(categories.contains(AchievementCategory.ECO))
        assertTrue(categories.contains(AchievementCategory.SAFETY))
        assertEquals(6, categories.size)
    }
    
    @Test
    fun `FanProfile high XP reaches high level`() {
        val legend = FanProfile(totalXP = 50000)
        assertTrue(legend.level >= 15)
    }
}
