package com.georacing.georacing.data.gamification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GamificationRepositoryTest {

    private lateinit var repository: GamificationRepository

    @Before
    fun setup() {
        repository = GamificationRepository()
    }

    @Test
    fun `initial profile has positive XP`() = runTest {
        val profile = repository.profile.first()
        assertTrue(profile.totalXP > 0)
    }

    @Test
    fun `initial profile has unlocked achievements`() = runTest {
        val profile = repository.profile.first()
        val unlockedCount = profile.achievements.count { it.isUnlocked }
        assertTrue("Should have pre-unlocked achievements for demo", unlockedCount > 0)
    }

    @Test
    fun `getAllAchievements returns non-empty list`() {
        val achievements = GamificationRepository.allAchievements
        assertTrue(achievements.isNotEmpty())
        assertTrue(achievements.size >= 15)
    }

    @Test
    fun `unlockAchievement increases XP`() = runTest {
        val initialProfile = repository.profile.first()
        val initialXP = initialProfile.totalXP

        // Find a locked achievement
        val lockedAchievement = initialProfile.achievements.find { !it.isUnlocked }
        assertNotNull("Should have at least one locked achievement", lockedAchievement)

        repository.unlockAchievement(lockedAchievement!!.id)

        val updatedProfile = repository.profile.first()
        assertTrue("XP should increase after unlocking", updatedProfile.totalXP > initialXP)
    }

    @Test
    fun `unlockAchievement marks achievement as unlocked`() = runTest {
        val initialProfile = repository.profile.first()
        val lockedAchievement = initialProfile.achievements.find { !it.isUnlocked }
        assertNotNull(lockedAchievement)

        repository.unlockAchievement(lockedAchievement!!.id)

        val updatedProfile = repository.profile.first()
        val achievement = updatedProfile.achievements.find { it.id == lockedAchievement.id }
        assertTrue("Achievement should be unlocked now", achievement!!.isUnlocked)
    }

    @Test
    fun `unlocking already unlocked achievement does not duplicate XP`() = runTest {
        val initialProfile = repository.profile.first()
        val unlockedAchievement = initialProfile.achievements.find { it.isUnlocked }
        assertNotNull(unlockedAchievement)
        val xpBefore = initialProfile.totalXP

        repository.unlockAchievement(unlockedAchievement!!.id)

        val updatedProfile = repository.profile.first()
        assertEquals("XP should not change when re-unlocking", xpBefore, updatedProfile.totalXP)
    }

    @Test
    fun `achievements cover multiple categories`() {
        val achievements = GamificationRepository.allAchievements
        val categories = achievements.map { it.category }.toSet()
        assertTrue("Should have at least 3 different categories", categories.size >= 3)
    }

    @Test
    fun `all achievements have valid XP rewards`() {
        val achievements = GamificationRepository.allAchievements
        achievements.forEach { achievement ->
            assertTrue(
                "Achievement '${achievement.title}' should have positive XP reward",
                achievement.xpReward > 0
            )
        }
    }
}
