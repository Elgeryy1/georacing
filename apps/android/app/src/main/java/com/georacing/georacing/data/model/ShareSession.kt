package com.georacing.georacing.data.model

import com.google.firebase.Timestamp

/**
 * Sesión temporal de compartir ubicación mediante QR usando Firebase
 * El QR contiene el sessionId para unirse al grupo
 */
data class ShareSession(
    val sessionId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val eventDate: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val groupId: String = ""
) {
    /**
     * Verifica si la sesión sigue válida
     */
    fun isExpired(): Boolean {
        return Timestamp.now().toDate().after(expiresAt.toDate()) || !isActive
    }
    
    /**
     * Convierte a Map para Firestore
     */
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            "sessionId" to sessionId,
            "ownerId" to ownerId,
            "ownerName" to ownerName,
            "eventDate" to eventDate,
            "expiresAt" to expiresAt,
            "createdAt" to createdAt,
            "isActive" to isActive,
            "groupId" to groupId
        )
    }
    
    companion object {
        /**
         * Crea ShareSession desde documento Firestore
         */
        fun fromMap(map: Map<String, Any>): ShareSession {
            return ShareSession(
                sessionId = map["sessionId"] as? String ?: "",
                ownerId = map["ownerId"] as? String ?: "",
                ownerName = map["ownerName"] as? String ?: "",
                eventDate = map["eventDate"] as? Timestamp ?: Timestamp.now(),
                expiresAt = map["expiresAt"] as? Timestamp ?: Timestamp.now(),
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                isActive = map["isActive"] as? Boolean ?: true,
                groupId = map["groupId"] as? String ?: ""
            )
        }
    }
}
