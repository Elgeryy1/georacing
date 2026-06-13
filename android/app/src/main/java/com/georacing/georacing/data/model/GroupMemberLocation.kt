package com.georacing.georacing.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.time.Instant

/**
 * Representa la ubicación en tiempo real de un miembro del grupo
 * Almacenado en Firestore: groups/{groupId}/locations/{uid}
 */
data class GroupMemberLocation(
    val userId: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now(),
    val sharing: Boolean = false
) {
    /**
     * Convierte a Map para guardarlo en Firestore
     */
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "userId" to userId,
            "displayName" to (displayName ?: ""),
            "photoUrl" to (photoUrl ?: ""),
            "position" to GeoPoint(latitude, longitude),
            "lastUpdated" to lastUpdated,
            "sharing" to sharing
        )
    }
    
    companion object {
        /**
         * Crea una instancia desde un documento de Firestore
         */
        fun fromFirestore(data: Map<String, Any>): GroupMemberLocation {
            val position = data["position"] as? GeoPoint
            val timestamp = data["lastUpdated"] as? Timestamp ?: Timestamp.now()
            
            return GroupMemberLocation(
                userId = data["userId"] as? String ?: "",
                displayName = data["displayName"] as? String,
                photoUrl = data["photoUrl"] as? String,
                latitude = position?.latitude ?: 0.0,
                longitude = position?.longitude ?: 0.0,
                lastUpdated = timestamp,
                sharing = data["sharing"] as? Boolean ?: false
            )
        }
    }
    
    /**
     * Convierte el Timestamp a Instant para facilitar comparaciones
     */
    fun getInstant(): Instant {
        return Instant.ofEpochSecond(lastUpdated.seconds, lastUpdated.nanoseconds.toLong())
    }
    
    /**
     * Calcula hace cuántos segundos fue la última actualización
     */
    fun getSecondsAgo(): Long {
        val now = Instant.now()
        val updated = getInstant()
        return now.epochSecond - updated.epochSecond
    }
    
    /**
     * Devuelve un texto descriptivo del estado de actualización
     */
    fun getStatusText(): String {
        if (!sharing) return "No compartiendo"
        
        val secondsAgo = getSecondsAgo()
        return when {
            secondsAgo < 30 -> "Ahora"
            secondsAgo < 60 -> "Hace ${secondsAgo}s"
            secondsAgo < 3600 -> "Hace ${secondsAgo / 60}min"
            else -> "Hace ${secondsAgo / 3600}h"
        }
    }
}
