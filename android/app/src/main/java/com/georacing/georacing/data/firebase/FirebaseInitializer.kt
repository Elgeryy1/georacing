package com.georacing.georacing.data.firebase

import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Clase para inicializar Firebase en el arranque de la app
 * 
 * NOTA: En versiones recientes de Firebase (con google-services.json), 
 * la inicialización es AUTOMÁTICA. Sin embargo, esta clase permite:
 * 
 * 1. Verificar que Firebase se inicializó correctamente
 * 2. Hacer login anónimo al inicio (para cumplir reglas de Firestore)
 * 3. Test de conexión opcional
 */
class FirebaseInitializer {
    
    companion object {
        private const val TAG = "FirebaseInitializer"
        private var isInitialized = false
        
        /**
         * Verifica que Firebase esté inicializado
         * No es necesario llamar a FirebaseApp.initializeApp() manualmente
         * con google-services.json presente
         */
        fun verifyInitialization(): Boolean {
            return try {
                val app = FirebaseApp.getInstance()
                isInitialized = true
                Log.d(TAG, "✅ Firebase inicializado: ${app.name}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Firebase NO inicializado", e)
                false
            }
        }
        
        /**
         * Inicializa la sesión de usuario con Google
         * SOLO se debe llamar si el usuario ya hizo login con Google
         * @deprecated No usar - el login ahora es manual desde LoginScreen
         */
        @Deprecated("Login manual requerido desde LoginScreen")
        suspend fun initializeSession(): Result<String> {
            val authService = FirebaseAuthService()
            
            return try {
                // Verificar si hay usuario con Google
                val currentUser = authService.getCurrentUser()
                
                if (currentUser != null && !currentUser.isAnonymous) {
                    val message = "✅ Usuario Google: ${currentUser.email}"
                    Log.d(TAG, message)
                    Result.success(message)
                } else {
                    val message = "❌ No hay usuario con Google"
                    Log.w(TAG, message)
                    Result.failure(Exception("Login con Google requerido"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error verificando sesión", e)
                Result.failure(e)
            }
        }
        
        /**
         * Función de inicialización básica de Firebase
         * NO hace login automático - el usuario debe loguearse desde LoginScreen
         * @param runConnectionTest: hacer test de conexión a Firestore (solo debug)
         */
        suspend fun initializeFirebase(runConnectionTest: Boolean = false): Result<String> {
            val messages = mutableListOf<String>()
            
            // 1. Verificar inicialización de Firebase
            if (!verifyInitialization()) {
                return Result.failure(Exception("Firebase no pudo inicializarse"))
            }
            messages.add("Firebase OK")
            
            // 2. Verificar si hay usuario logueado (opcional)
            val authService = FirebaseAuthService()
            val currentUser = authService.getCurrentUser()
            
            if (currentUser != null && !currentUser.isAnonymous) {
                messages.add("Usuario: ${currentUser.email}")
            } else {
                messages.add("Sin sesión")
            }
            
            // 3. Test de conexión opcional (solo para debug)
            if (runConnectionTest) {
                val firestoreService = FirebaseFirestoreService()
                val testResult = firestoreService.testConnection()
                
                if (testResult.isSuccess) {
                    messages.add("Firestore OK")
                } else {
                    Log.w(TAG, "Test de conexión falló (no crítico)", testResult.exceptionOrNull())
                    messages.add("Firestore: test falló")
                }
            }
            
            val finalMessage = messages.joinToString(" | ")
            Log.d(TAG, "🚀 Inicialización: $finalMessage")
            return Result.success(finalMessage)
        }
    }
}
