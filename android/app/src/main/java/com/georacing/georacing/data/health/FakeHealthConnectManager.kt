package com.georacing.georacing.data.health

import android.content.Context
import android.util.Log
import com.georacing.georacing.debug.ScenarioSimulator
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant

/**
 * Fake implementation of HealthConnectManager for Demo/Mock purposes.
 * Always returns true for availability/permissions and returns simulated data.
 */
class FakeHealthConnectManager(context: Context) : HealthConnectManager(context) {

    companion object {
        private const val TAG = "FakeHealthConnectManager"
        private const val BASE_STEPS = 8432L
        private const val BASE_DISTANCE = 5800.0 // 5.8 km
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override suspend fun hasPermissions(): Boolean {
        // Always return true so the UI never asks for permissions
        return true
    }

    override suspend fun readDailyMetrics(startTime: Instant?): DailyMetrics {
        // Simulate "Live" feel with slight randomness
        // Random between 8400 and 8500
        val steps = kotlin.random.Random.nextLong(8400, 8500)
        
        // Add any extra steps injected via Simulator
        val extraSteps = ScenarioSimulator.extraFakeSteps.value
        val totalSteps = steps + extraSteps
        val totalDistance = BASE_DISTANCE + (extraSteps * 0.7) // consistent distance for base + extra

        Log.d(TAG, "Serving Fake Metrics (Live): Steps=$totalSteps, Dist=$totalDistance")
        return DailyMetrics(
            steps = totalSteps,
            distanceMeters = totalDistance
        )
    }
}
