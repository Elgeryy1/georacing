package com.georacing.georacing.data.gamification

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric provides a real android.util.Log so the repository's logging
 * (used in its backend-load fallback path) does not throw "not mocked".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class GamificationRepositoryTest {

    private lateinit var repository: GamificationRepository

    @Before
    fun setup() {
        repository = GamificationRepository()
    }

    @Test
    fun `initial profile starts at zero XP and level one`() = runTest {
        // Before any backend state loads, the profile is the empty seed: no XP earned,
        // so the derived level is 1.
        val profile = repository.profile.first()
        assertEquals(0, profile.totalXP)
        assertEquals(1, profile.level)
    }

    @Test
    fun `initial profile exposes the full achievement catalogue all locked`() = runTest {
        val profile = repository.profile.first()
        // Every catalogue achievement is present...
        assertEquals(GamificationRepository.allAchievements.size, profile.achievements.size)
        // ...and none is unlocked until the player earns it.
        assertEquals(0, profile.achievements.count { it.isUnlocked })
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
        // Unlock a fresh achievement first, then attempt to unlock the same one again.
        val target = repository.profile.first().achievements.first { !it.isUnlocked }
        repository.unlockAchievement(target.id)
        val xpAfterFirstUnlock = repository.profile.first().totalXP

        // Re-unlocking the same achievement must be idempotent for XP.
        repository.unlockAchievement(target.id)

        val xpAfterSecondUnlock = repository.profile.first().totalXP
        assertEquals(
            "XP should not change when re-unlocking",
            xpAfterFirstUnlock,
            xpAfterSecondUnlock
        )
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
