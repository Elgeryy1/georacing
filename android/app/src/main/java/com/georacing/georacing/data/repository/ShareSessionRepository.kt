package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.firebase.FirebaseAuthService
import com.georacing.georacing.data.firestorelike.FirestoreLikeApi
import com.georacing.georacing.data.firestorelike.FirestoreLikeClient
import com.georacing.georacing.data.model.ShareSession
import com.google.firebase.Timestamp
import java.util.*

/**
 * Repositorio para gestionar sesiones de compartir ubicación mediante QR
 * Usando base de datos SQL local a través de FirestoreLikeClient
 */
class ShareSessionRepository {
    private val authService = FirebaseAuthService()
    
    companion object {
        private const val TAG = "ShareSessionRepo"
        private const val TABLE_SESSIONS = "share_sessions"
        private const val TABLE_GROUPS = "groups"
        private const val TABLE_MEMBERS = "group_members"
    }
    
    suspend fun createShareSession(groupId: String, eventDate: Date): Result<ShareSession> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Usuario no autenticado"))
            }
            
            val currentUser = userResult.getOrNull()!!
            val sessionId = UUID.randomUUID().toString()
            
            // Check if group exists natively via API
            val groupReq = FirestoreLikeApi.GetRequest(TABLE_GROUPS, mapOf("id" to groupId))
            val existingGroups = FirestoreLikeClient.api.get(groupReq)
            
            if (existingGroups.isEmpty()) {
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = TABLE_GROUPS,
                    data = mapOf(
                        "id" to groupId,
                        "name" to "Grupo de ${currentUser.displayName}",
                        "ownerId" to currentUser.uid,
                        "createdAt" to System.currentTimeMillis(),
                        "isActive" to true
                    )
                ))
            }
            
            // Add creator to members
            FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                table = TABLE_MEMBERS,
                data = mapOf(
                    "groupId" to groupId,
                    "userId" to currentUser.uid,
                    "displayName" to (currentUser.displayName ?: "Usuario"),
                    "photoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "joinedAt" to System.currentTimeMillis(),
                    "role" to "owner"
                )
            ))
            
            val calendar = Calendar.getInstance().apply {
                time = eventDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            
            val session = ShareSession(
                sessionId = sessionId,
                ownerId = currentUser.uid,
                ownerName = currentUser.displayName ?: "Usuario",
                eventDate = Timestamp(eventDate),
                expiresAt = Timestamp(calendar.time),
                createdAt = Timestamp.now(),
                isActive = true,
                groupId = groupId
            )
            
            // Save Session via API
            FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                table = TABLE_SESSIONS,
                data = mapOf(
                    "sessionId" to session.sessionId,
                    "groupId" to session.groupId,
                    "ownerId" to session.ownerId,
                    "ownerName" to session.ownerName,
                    "eventDate" to session.eventDate.toDate().time,
                    "expiresAt" to session.expiresAt.toDate().time,
                    "createdAt" to session.createdAt.toDate().time,
                    "isActive" to session.isActive
                )
            ))
            
            Log.d(TAG, "✅ Sesión QR creada en BD Local: $sessionId")
            Result.success(session)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creando sesión", e)
            Result.failure(e)
        }
    }
    
    suspend fun getSession(sessionId: String): Result<ShareSession> {
        return try {
            val req = FirestoreLikeApi.GetRequest(TABLE_SESSIONS, mapOf("sessionId" to sessionId))
            val results = FirestoreLikeClient.api.get(req)
            
            if (results.isEmpty()) {
                return Result.failure(Exception("Sesión no encontrada"))
            }
            
            val data = results.first()
            val eventDateMillis = (data["eventDate"] as? Number)?.toLong() ?: 0L
            val expiresAtMillis = (data["expiresAt"] as? Number)?.toLong() ?: 0L
            val createdAtMillis = (data["createdAt"] as? Number)?.toLong() ?: 0L
            
            val session = ShareSession(
                sessionId = data["sessionId"]?.toString() ?: "",
                ownerId = data["ownerId"]?.toString() ?: "",
                ownerName = data["ownerName"]?.toString() ?: "",
                eventDate = Timestamp(Date(eventDateMillis)),
                expiresAt = Timestamp(Date(expiresAtMillis)),
                createdAt = Timestamp(Date(createdAtMillis)),
                isActive = (data["isActive"] as? Boolean) ?: true,
                groupId = data["groupId"]?.toString() ?: ""
            )
            
            if (session.isExpired()) {
                return Result.failure(Exception("Sesión expirada"))
            }
            
            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinSessionGroup(sessionId: String): Result<String> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) return Result.failure(userResult.exceptionOrNull() ?: Exception("No auth"))
            
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Invalid"))
            
            val session = sessionResult.getOrNull()!!
            val currentUser = userResult.getOrNull()!!
            
            FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                table = TABLE_MEMBERS,
                data = mapOf(
                    "groupId" to session.groupId,
                    "userId" to currentUser.uid,
                    "displayName" to (currentUser.displayName ?: "Usuario"),
                    "photoUrl" to (currentUser.photoUrl?.toString() ?: ""),
                    "joinedAt" to System.currentTimeMillis(),
                    "joinedVia" to "qr_session",
                    "sessionId" to sessionId
                )
            ))
            
            Log.d(TAG, "✅ Usuario unido a grupo local: ${session.groupId}")
            Result.success(session.groupId)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMySessions(): Result<List<ShareSession>> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) return Result.failure(Exception("No auth"))
            
            val currentUser = userResult.getOrNull()!!
            val req = FirestoreLikeApi.GetRequest(TABLE_SESSIONS, mapOf("ownerId" to currentUser.uid, "isActive" to true))
            val results = FirestoreLikeClient.api.get(req)
            
            val nowMillis = System.currentTimeMillis()
            val sessions = results.mapNotNull { data ->
                val expiresAtMillis = (data["expiresAt"] as? Number)?.toLong() ?: 0L
                if (expiresAtMillis > nowMillis) {
                    val eventDateMillis = (data["eventDate"] as? Number)?.toLong() ?: 0L
                    val createdAtMillis = (data["createdAt"] as? Number)?.toLong() ?: 0L
                    ShareSession(
                        sessionId = data["sessionId"]?.toString() ?: "",
                        ownerId = data["ownerId"]?.toString() ?: "",
                        ownerName = data["ownerName"]?.toString() ?: "",
                        eventDate = Timestamp(Date(eventDateMillis)),
                        expiresAt = Timestamp(Date(expiresAtMillis)),
                        createdAt = Timestamp(Date(createdAtMillis)),
                        isActive = (data["isActive"] as? Boolean) ?: true,
                        groupId = data["groupId"]?.toString() ?: ""
                    )
                } else null
            }
            
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deactivateSession(sessionId: String): Result<Unit> {
        return try {
            // Our generic api doesn't specifically have a distinct single field update, 
            // but upsert by ID typically overwrites or we can fetch, modify and upsert.
            // In SQL an upsert based on sessionId will work since that's the PK.
            val sessionReq = getSession(sessionId)
            if (sessionReq.isSuccess) {
                val s = sessionReq.getOrNull()!!
                FirestoreLikeClient.api.upsert(FirestoreLikeApi.UpsertRequest(
                    table = TABLE_SESSIONS,
                    data = mapOf(
                        "sessionId" to s.sessionId,
                        "groupId" to s.groupId,
                        "ownerId" to s.ownerId,
                        "ownerName" to s.ownerName,
                        "eventDate" to s.eventDate.toDate().time,
                        "expiresAt" to s.expiresAt.toDate().time,
                        "createdAt" to s.createdAt.toDate().time,
                        "isActive" to false
                    )
                ))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
