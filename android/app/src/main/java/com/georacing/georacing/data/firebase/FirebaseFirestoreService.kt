package com.georacing.georacing.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Servicio centralizado para todas las operaciones con Firestore
 * 
 * COLECCIONES UTILIZADAS:
 * - incidents: Reportes de incidencias del público
 * - race_news: Newsletter del circuito (alertas/noticias)
 * - users: Datos de usuarios (opcional para futuro)
 * - test_connection: Para verificar conectividad
 */
class FirebaseFirestoreService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "FirestoreService"
        
        // Nombres de colecciones - Centralizados para consistencia
        const val COLLECTION_INCIDENTS = "incidents"
        const val COLLECTION_RACE_NEWS = "race_news"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_TEST = "test_connection"
    }
    
    /**
     * TEST: Escribe un documento de prueba para verificar conexión con Firestore
     * Llama esto desde el splash screen o settings para debug
     */
    suspend fun testConnection(): Result<String> {
        return try {
            val testData = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "message" to "Conexión exitosa desde GeoRacing",
                "device" to android.os.Build.MODEL
            )
            
            val docRef = db.collection(COLLECTION_TEST)
                .add(testData)
                .await()
            
            val successMessage = "✅ Firestore conectado OK. Doc ID: ${docRef.id}"
            Log.d(TAG, successMessage)
            Result.success(successMessage)
        } catch (e: Exception) {
            val errorMessage = "❌ Error conectando a Firestore: ${e.message}"
            Log.e(TAG, errorMessage, e)
            Result.failure(e)
        }
    }
    
    /**
     * Guarda un documento en una colección específica
     */
    suspend fun <T> addDocument(
        collection: String,
        data: T
    ): Result<String> {
        return try {
            val docRef = db.collection(collection)
                .add(data as Any)
                .await()
            
            Log.d(TAG, "Documento creado en '$collection': ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando en '$collection'", e)
            Result.failure(e)
        }
    }
    
    /**
     * Actualiza o crea un documento con un ID específico
     */
    suspend fun <T> setDocument(
        collection: String,
        documentId: String,
        data: T,
        merge: Boolean = true
    ): Result<Unit> {
        return try {
            if (merge) {
                db.collection(collection)
                    .document(documentId)
                    .set(data as Any, SetOptions.merge())
                    .await()
            } else {
                db.collection(collection)
                    .document(documentId)
                    .set(data as Any)
                    .await()
            }
            
            Log.d(TAG, "Documento actualizado en '$collection': $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando '$collection/$documentId'", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lee un documento específico
     */
    suspend fun <T> getDocument(
        collection: String,
        documentId: String,
        clazz: Class<T>
    ): Result<T?> {
        return try {
            val snapshot = db.collection(collection)
                .document(documentId)
                .get()
                .await()
            
            val data = snapshot.toObject(clazz)
            Log.d(TAG, "Documento leído de '$collection': $documentId")
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo '$collection/$documentId'", e)
            Result.failure(e)
        }
    }
    
    /**
     * Lee todos los documentos de una colección
     */
    suspend fun <T> getCollection(
        collection: String,
        clazz: Class<T>
    ): Result<List<T>> {
        return try {
            val snapshot = db.collection(collection)
                .get()
                .await()
            
            val items = snapshot.documents.mapNotNull { it.toObject(clazz) }
            Log.d(TAG, "Leídos ${items.size} documentos de '$collection'")
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo colección '$collection'", e)
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un documento
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String
    ): Result<Unit> {
        return try {
            db.collection(collection)
                .document(documentId)
                .delete()
                .await()
            
            Log.d(TAG, "Documento eliminado de '$collection': $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando '$collection/$documentId'", e)
            Result.failure(e)
        }
    }
}
