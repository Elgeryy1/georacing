package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.firebase.FirebaseAuthService
import com.georacing.georacing.data.model.GroupMemberLocation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar ubicaciones en tiempo real de miembros del grupo
 * Estructura Firestore: groups/{groupId}/locations/{uid}
 */
class GroupLocationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val authService = FirebaseAuthService()
    private var locationListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "GroupLocationRepo"
        private const val COLLECTION_GROUPS = "groups"
        private const val SUBCOLLECTION_LOCATIONS = "locations"
    }
    
    /**
     * Actualiza la ubicación del usuario actual en su grupo activo
     * Solo el propio usuario puede actualizar su ubicación (según reglas Firestore)
     */
    suspend fun updateMyLocation(
        groupId: String,
        lat: Double,
        lng: Double,
        displayName: String? = null,
        photoUrl: String? = null,
        sharing: Boolean = true
    ): Result<Unit> {
        return try {
            // Verificar que hay usuario autenticado
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Usuario no autenticado"))
            }
            
            val currentUser = userResult.getOrNull()!!
            val uid = currentUser.uid
            
            // Crear el documento de ubicación
            val locationData = hashMapOf(
                "userId" to uid,
                "displayName" to (displayName ?: currentUser.displayName ?: "Usuario"),
                "photoUrl" to (photoUrl ?: currentUser.photoUrl?.toString() ?: ""),
                "position" to GeoPoint(lat, lng),
                "lastUpdated" to Timestamp.now(),
                "sharing" to sharing
            )
            
            // Guardar en Firestore: groups/{groupId}/locations/{uid}
            firestore.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_LOCATIONS)
                .document(uid)
                .set(locationData)
                .await()
            
            Log.d(TAG, "✅ Ubicación actualizada: ($lat, $lng) para grupo $groupId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando ubicación", e)
            Result.failure(e)
        }
    }
    
    /**
     * Marca que el usuario dejó de compartir ubicación (sin borrar el documento)
     */
    suspend fun stopSharingLocation(groupId: String): Result<Unit> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Usuario no autenticado"))
            }
            
            val uid = userResult.getOrNull()!!.uid
            
            firestore.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_LOCATIONS)
                .document(uid)
                .update("sharing", false)
                .await()
            
            Log.d(TAG, "✅ Usuario dejó de compartir ubicación")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al dejar de compartir", e)
            Result.failure(e)
        }
    }
    
    /**
     * Escucha en tiempo real las ubicaciones de todos los miembros del grupo
     * 
     * NOTA: Asegúrate de que las reglas de Firestore permiten leer:
     * - Cualquier usuario autenticado puede leer groups/{groupId}/locations
     */
    fun listenGroupLocations(
        groupId: String,
        onResult: (List<GroupMemberLocation>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        // Cancelar listener anterior si existe
        stopListening()
        
        Log.d(TAG, "👂 Escuchando ubicaciones del grupo: $groupId")
        
        locationListener = firestore.collection(COLLECTION_GROUPS)
            .document(groupId)
            .collection(SUBCOLLECTION_LOCATIONS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error escuchando ubicaciones: ${error.message}", error)
                    
                    // Manejo específico de PERMISSION_DENIED
                    if (error.message?.contains("PERMISSION_DENIED") == true) {
                        Log.e(TAG, """
                            ❌ PERMISSION_DENIED en groups/$groupId/locations
                            
                            Verifica las reglas de Firestore:
                            
                            match /groups/{groupId}/locations/{uid} {
                              allow read: if request.auth != null;
                              allow create, update: if request.auth != null && request.auth.uid == uid;
                              allow delete: if request.auth != null && request.auth.uid == uid;
                            }
                            
                            Usuario actual: ${authService.getCurrentUser()?.uid ?: "NO AUTENTICADO"}
                        """.trimIndent())
                    }
                    
                    onError(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val locations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            GroupMemberLocation.fromFirestore(data)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando documento ${doc.id}", e)
                            null
                        }
                    }
                    
                    Log.d(TAG, "✅ Recibidas ${locations.size} ubicaciones")
                    onResult(locations)
                } else {
                    Log.d(TAG, "⚠️ No hay ubicaciones en el grupo")
                    onResult(emptyList())
                }
            }
    }
    
    /**
     * Detiene el listener de ubicaciones en tiempo real
     */
    fun stopListening() {
        locationListener?.remove()
        locationListener = null
        Log.d(TAG, "🛑 Listener de ubicaciones detenido")
    }
    
    /**
     * Obtiene las ubicaciones del grupo una sola vez (no en tiempo real)
     */
    suspend fun getGroupLocations(groupId: String): Result<List<GroupMemberLocation>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_LOCATIONS)
                .get()
                .await()
            
            val locations = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    GroupMemberLocation.fromFirestore(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando documento ${doc.id}", e)
                    null
                }
            }
            
            Result.success(locations)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ubicaciones", e)
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene el estado actual de compartir ubicación del usuario
     */
    suspend fun getMyLocationStatus(groupId: String): Result<Boolean> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Usuario no autenticado"))
            }
            
            val uid = userResult.getOrNull()!!.uid
            
            val doc = firestore.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_LOCATIONS)
                .document(uid)
                .get()
                .await()
            
            val isSharing = doc.getBoolean("sharing") ?: false
            Log.d(TAG, "Estado actual de sharing: $isSharing")
            Result.success(isSharing)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo estado de sharing", e)
            Result.success(false) // Por defecto false si no existe
        }
    }
    
    /**
     * Borra completamente el documento de ubicación del usuario
     * Útil cuando el usuario abandona el grupo
     */
    suspend fun deleteMyLocation(groupId: String): Result<Unit> {
        return try {
            val userResult = authService.requireGoogleSignedIn()
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Usuario no autenticado"))
            }
            
            val uid = userResult.getOrNull()!!.uid
            
            firestore.collection(COLLECTION_GROUPS)
                .document(groupId)
                .collection(SUBCOLLECTION_LOCATIONS)
                .document(uid)
                .delete()
                .await()
            
            Log.d(TAG, "✅ Ubicación borrada del grupo $groupId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error borrando ubicación", e)
            Result.failure(e)
        }
    }
}
