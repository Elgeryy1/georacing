package com.georacing.georacing.data.gamification

import android.util.Log
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.domain.model.Achievement
import com.georacing.georacing.domain.model.AchievementCategory
import com.georacing.georacing.domain.model.FanProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repositorio de gamificación para Circuit de Catalunya.
 * Gestiona logros, XP y nivel del fan.
 * 
 * Persistencia: FirestoreLikeApi (backend real en georacing.example.com:4010)
 * Los logros predefinidos son datos de diseño de juego, pero
 * el estado de desbloqueo se persiste al backend.
 */
class GamificationRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _profile = MutableStateFlow(
        FanProfile(
            totalXP = 0,
            level = 1,
            achievements = allAchievements,
            circuitsVisited = 0,
            kmWalked = 0f,
            friendsInGroup = 0
        )
    )
    val profile: StateFlow<FanProfile> = _profile.asStateFlow()

    init {
        // Cargar estado guardado del backend al inicio
        scope.launch { loadProfile() }
    }

    private suspend fun loadProfile() {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "current_user"
            val req = FirestoreLikeApi.GetRequest("gamification_profile", mapOf("id" to userId))
            val saved = FirestoreLikeClient.api.get(req)
            if (saved.isNotEmpty()) {
                val data = saved.first()
                val savedXP = (data["totalXP"] as? Number)?.toInt() ?: 0
                val unlockedIds = (data["unlockedAchievements"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                val updatedAchievements = GamificationRepository.allAchievements.map { achievement ->
                    if (achievement.id in unlockedIds) achievement.copy(isUnlocked = true, progress = 1f, unlockedAt = System.currentTimeMillis())
                    else achievement
                }

                _profile.value = FanProfile(
                    totalXP = savedXP,
                    level = (savedXP / 250) + 1,
                    achievements = updatedAchievements,
                    circuitsVisited = (data["circuitsVisited"] as? Number)?.toInt() ?: 0,
                    kmWalked = (data["kmWalked"] as? Number)?.toFloat() ?: 0f,
                    friendsInGroup = (data["friendsInGroup"] as? Number)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.w("GamificationRepo", "No se pudo cargar perfil del backend: ${e.message}")
        }
    }

    private fun saveProfile() {
        scope.launch {
            try {
                val current = _profile.value
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "current_user"
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = "gamification_profile",
                    data = mapOf(
                        "id" to userId,
                        "totalXP" to current.totalXP,
                        "level" to current.level,
                        "unlockedAchievements" to current.achievements.filter { it.isUnlocked }.map { it.id },
                        "circuitsVisited" to current.circuitsVisited,
                        "kmWalked" to current.kmWalked,
                        "friendsInGroup" to current.friendsInGroup
                    )
                ))
            } catch (e: Exception) {
                Log.w("GamificationRepo", "No se pudo guardar perfil: ${e.message}")
            }
        }
    }

    fun unlockAchievement(id: String) {
        _profile.value = _profile.value.let { current ->
            val updated = current.achievements.map {
                if (it.id == id && !it.isUnlocked) it.copy(isUnlocked = true, progress = 1f, unlockedAt = System.currentTimeMillis())
                else it
            }
            val gained = current.achievements.find { it.id == id }?.xpReward ?: 0
            val newXP = current.totalXP + gained
            current.copy(
                achievements = updated,
                totalXP = newXP,
                level = (newXP / 250) + 1
            )
        }
        saveProfile()
    }

    companion object {
        val allAchievements = listOf(
            // Explorer — todos empieza desbloqueados=false, el backend determina el estado real
            Achievement("exp_first_visit", "Primera Visita", "Llega al Circuit de Catalunya", "🏁", AchievementCategory.EXPLORER, 100, false, 0f),
            Achievement("exp_all_zones", "Explorador Total", "Visita todas las zonas del circuito", "🗺️", AchievementCategory.EXPLORER, 200, false, 0f),
            Achievement("exp_5km", "Maratoniano", "Camina 5km por el circuito", "🏃", AchievementCategory.EXPLORER, 150, false, 0f),
            Achievement("exp_paddock", "Acceso VIP", "Visita el Pit Lane Walk", "⭐", AchievementCategory.EXPLORER, 300, false, 0f),
            // Social
            Achievement("soc_first_group", "En Equipo", "Crea o únete a un grupo", "👥", AchievementCategory.SOCIAL, 100, false, 0f),
            Achievement("soc_5_friends", "Escudería Completa", "Ten 5 amigos en tu grupo", "🏎️", AchievementCategory.SOCIAL, 200, false, 0f),
            Achievement("soc_share_qr", "Conexión Rápida", "Comparte un QR de grupo", "📱", AchievementCategory.SOCIAL, 50, false, 0f),
            Achievement("soc_moment_shared", "Fotógrafo", "Comparte un momento", "📸", AchievementCategory.SOCIAL, 100, false, 0f),
            // Speed
            Achievement("spd_first_nav", "GPS Activado", "Usa la navegación por primera vez", "🛰️", AchievementCategory.SPEED, 100, false, 0f),
            Achievement("spd_arrive_fast", "Pole Position", "Llega al circuito antes de las 9:00", "⏱️", AchievementCategory.SPEED, 200, false, 0f),
            Achievement("spd_find_car", "Memoria de Elefante", "Encuentra tu coche con la app", "🚗", AchievementCategory.SPEED, 100, false, 0f),
            // Fan
            Achievement("fan_first_order", "Primera Compra", "Haz tu primer pedido", "🛒", AchievementCategory.FAN, 100, false, 0f),
            Achievement("fan_merch", "Coleccionista", "Compra merchandising oficial", "👕", AchievementCategory.FAN, 150, false, 0f),
            Achievement("fan_weather_check", "Meteorólogo", "Consulta el clima 3 veces", "🌤️", AchievementCategory.FAN, 50, false, 0f),
            Achievement("fan_telemetry", "Ingeniero de Datos", "Usa F1 Live más de 10 min", "📊", AchievementCategory.FAN, 200, false, 0f),
            // Eco
            Achievement("eco_transport", "Movilidad Verde", "Llega en transporte público", "🚆", AchievementCategory.ECO, 200, false, 0f),
            Achievement("eco_fountain", "Hidratación Sostenible", "Usa una fuente del circuito", "💧", AchievementCategory.ECO, 50, false, 0f),
            // Safety
            Achievement("saf_report", "Ciudadano Responsable", "Reporta una incidencia", "🛡️", AchievementCategory.SAFETY, 100, false, 0f),
            Achievement("saf_emergency", "Preparado", "Configura tu info médica", "🏥", AchievementCategory.SAFETY, 150, false, 0f),
            Achievement("saf_medical", "Prevención", "Revisa la pantalla de emergencia", "🆘", AchievementCategory.SAFETY, 50, false, 0f)
        )
    }
}
