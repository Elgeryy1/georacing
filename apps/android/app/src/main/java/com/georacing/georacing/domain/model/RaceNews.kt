package com.georacing.georacing.domain.model

data class RaceNews(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val category: NewsCategory,
    val priority: NewsPriority,
    val imageUrl: String? = null
)

enum class NewsCategory(val displayName: String) {
    RACE_UPDATE("Actualización de Carrera"),
    SCHEDULE_CHANGE("Cambio de Horario"),
    WEATHER("Meteorología"),
    TRAFFIC("Tráfico"),
    DRIVER_NEWS("Noticias de Pilotos"),
    SAFETY("Seguridad"),
    EVENT("Evento Especial"),
    GENERAL("General")
}

enum class NewsPriority {
    HIGH,     // Rojo - Urgente
    MEDIUM,   // Amarillo - Importante
    LOW       // Verde - Informativo
}
