package com.georacing.georacing.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.georacing.georacing.domain.model.SeatInfo
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesDataStore(private val context: Context) {

    private val gson = Gson()

    val activeGroupId: Flow<String?> = context.dataStore.data.map { it[ACTIVE_GROUP_ID] }
    val seatInfo: Flow<SeatInfo?> = context.dataStore.data.map { prefs ->
        val json = prefs[SEAT_INFO]
        if (json != null) {
            try {
                gson.fromJson(json, SeatInfo::class.java)
            } catch (e: Exception) { null }
        } else null
    }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }
    val preferredLanguage: Flow<String> = context.dataStore.data.map { it[PREFERRED_LANGUAGE] ?: "es" }
    val highContrast: Flow<Boolean> = context.dataStore.data.map { it[HIGH_CONTRAST] ?: false }
    val largeFont: Flow<Boolean> = context.dataStore.data.map { it[LARGE_FONT] ?: false }
    val avoidStairs: Flow<Boolean> = context.dataStore.data.map { it[AVOID_STAIRS] ?: false }
    val favoriteTeam: Flow<String> = context.dataStore.data.map { it[FAVORITE_TEAM] ?: "NONE" }
    
    // EcoMeter: Timestamp when the user arrived at the circuit
    val circuitArrivalTime: Flow<Long?> = context.dataStore.data.map { it[CIRCUIT_ARRIVAL_TIME] }

    suspend fun setActiveGroupId(groupId: String?) {
        context.dataStore.edit { prefs ->
            if (groupId == null) {
                prefs.remove(ACTIVE_GROUP_ID)
            } else {
                prefs[ACTIVE_GROUP_ID] = groupId
            }
        }
    }

    suspend fun setSeatInfo(info: SeatInfo?) {
        context.dataStore.edit { prefs ->
            if (info == null) {
                prefs.remove(SEAT_INFO)
            } else {
                prefs[SEAT_INFO] = gson.toJson(info)
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setPreferredLanguage(lang: String) {
        context.dataStore.edit { it[PREFERRED_LANGUAGE] = lang }
    }

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }

    suspend fun setLargeFont(enabled: Boolean) {
        context.dataStore.edit { it[LARGE_FONT] = enabled }
    }

    suspend fun setAvoidStairs(enabled: Boolean) {
        context.dataStore.edit { it[AVOID_STAIRS] = enabled }
    }

    suspend fun setFavoriteTeam(team: String) {
        context.dataStore.edit { it[FAVORITE_TEAM] = team }
    }

    suspend fun setCircuitArrivalTime(timeMs: Long?) {
        context.dataStore.edit { prefs ->
            if (timeMs == null) prefs.remove(CIRCUIT_ARRIVAL_TIME)
            else prefs[CIRCUIT_ARRIVAL_TIME] = timeMs
        }
    }

    val dashboardLayout: Flow<List<com.georacing.georacing.domain.model.WidgetType>> = context.dataStore.data.map { prefs ->
        val output = prefs[DASHBOARD_LAYOUT]
        if (output != null) {
            try {
                output.split(",").mapNotNull { 
                    try { com.georacing.georacing.domain.model.WidgetType.valueOf(it) } catch (e: Exception) { null } 
                }
            } catch (e: Exception) {
                com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
            }
        } else {
             com.georacing.georacing.domain.model.DashboardLayout.DEFAULT.widgets
        }
    }

    suspend fun setDashboardLayout(widgets: List<com.georacing.georacing.domain.model.WidgetType>) {
        context.dataStore.edit { prefs ->
            prefs[DASHBOARD_LAYOUT] = widgets.joinToString(",") { it.name }
        }
    }

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val SEAT_INFO = stringPreferencesKey("seat_info")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LARGE_FONT = booleanPreferencesKey("large_font")
        val AVOID_STAIRS = booleanPreferencesKey("avoid_stairs")
        val FAVORITE_TEAM = stringPreferencesKey("favorite_team")
        val ACTIVE_GROUP_ID = stringPreferencesKey("active_group_id")
        val DASHBOARD_LAYOUT = stringPreferencesKey("dashboard_layout")
        val CIRCUIT_ARRIVAL_TIME = androidx.datastore.preferences.core.longPreferencesKey("circuit_arrival_time")
    }
}
