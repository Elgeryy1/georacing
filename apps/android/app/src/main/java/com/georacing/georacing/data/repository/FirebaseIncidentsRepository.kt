package com.georacing.georacing.data.repository

import android.util.Log
import com.georacing.georacing.data.firebase.FirebaseAuthService
import com.georacing.georacing.data.firebase.FirebaseFirestoreService
import com.georacing.georacing.domain.model.IncidentReport
import com.georacing.georacing.domain.repository.IncidentsRepository

/**
 * Implementación REAL del repositorio de incidencias usando Firebase
 * 
 * Este repositorio:
 * 1. Asegura que el usuario esté autenticado (login anónimo si es necesario)
 * 2. Guarda las incidencias en Firestore en la colección "incidents"
 * 3. Maneja errores de forma robusta
 */
class FirebaseIncidentsRepository(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) : IncidentsRepository {
    
    companion object {
        private const val TAG = "FirebaseIncidentsRepo"
    }
    
    override suspend fun getIncidents(): kotlinx.coroutines.flow.Flow<List<IncidentReport>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun reportIncident(incident: IncidentReport) {
        try {
            // 1. CRÍTICO: Verificar que el usuario está autenticado con Google
            val authResult = authService.requireGoogleSignedIn()
            
            if (authResult.isFailure) {
                Log.e(TAG, "Usuario no autenticado con Google")
                throw authResult.exceptionOrNull() 
                    ?: Exception("Login con Google requerido")
            }
            
            val user = authResult.getOrNull()!!
            Log.d(TAG, "Usuario autenticado: ${user.email}")
            
            // 2. Preparar datos para Firestore (convertir a Map para mejor control)
            val incidentData = hashMapOf(
                "userId" to user.uid,
                "userEmail" to user.email,
                "userName" to user.displayName,
                "category" to incident.category.name,
                "categoryDisplay" to incident.category.displayName,
                "description" to incident.description,
                "beaconId" to incident.beaconId,
                "zone" to incident.zone,
                "timestamp" to incident.timestamp,
                "status" to "pending", // El staff la marcará como "resolved" o "in_progress"
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            // 3. Guardar en Firestore
            val result = firestoreService.addDocument(
                collection = FirebaseFirestoreService.COLLECTION_INCIDENTS,
                data = incidentData
            )
            
            if (result.isSuccess) {
                val docId = result.getOrNull()
                Log.d(TAG, "✅ Incidencia guardada con ID: $docId")
            } else {
                Log.e(TAG, "❌ Error guardando incidencia", result.exceptionOrNull())
                throw result.exceptionOrNull() 
                    ?: Exception("Error desconocido al guardar incidencia")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendIncident", e)
            throw e // Re-lanzar para que el ViewModel lo maneje
        }
    }
}
