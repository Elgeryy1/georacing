package com.georacing.georacing.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters para tipos complejos.
 * Convierte listas y objetos a/desde String para almacenamiento en SQLite.
 */
class Converters {

    private val gson = Gson()

    /**
     * Convierte List<String> a String JSON para almacenar en Room.
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    /**
     * Convierte String JSON de vuelta a List<String>.
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }

    /**
     * Alternativa simple: Lista separada por comas (más eficiente para listas pequeñas).
     */
    @TypeConverter
    fun fromCommaSeparated(value: String?): List<String> {
        return value?.split(",")?.map { it.trim() } ?: emptyList()
    }

    @TypeConverter
    fun toCommaSeparated(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
}
