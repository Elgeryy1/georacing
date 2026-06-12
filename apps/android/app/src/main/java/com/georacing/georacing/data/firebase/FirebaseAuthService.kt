package com.georacing.georacing.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Servicio centralizado para gestionar la autenticación con Firebase
 * Soporta login con Google (obligatorio) y fallback a anónimo solo para testing
 */
class FirebaseAuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "FirebaseAuthService"
    }
    
    /**
     * Obtiene el usuario actualmente autenticado
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Verifica si hay un usuario autenticado
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null
    
    /**
     * Login con Google usando ID Token
     * Este es el método PRINCIPAL para usuarios reales
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            
            if (user != null) {
                Log.d(TAG, "✅ Login con Google exitoso")
                Log.d(TAG, "   UID: ${user.uid}")
                Log.d(TAG, "   Email: ${user.email}")
                Log.d(TAG, "   Nombre: ${user.displayName}")
                Result.success(user)
            } else {
                Log.e(TAG, "Login con Google falló: usuario nulo")
                Result.failure(Exception("Usuario nulo después del login"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en login con Google", e)
            Result.failure(e)
        }
    }
    
    /**
     * Login anónimo - SOLO PARA TESTING/DEBUG
     * En producción, los usuarios DEBEN usar Google Sign-In
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user
            
            if (user != null) {
                Log.w(TAG, "⚠️ Login anónimo (solo testing). UID: ${user.uid}")
                Result.success(user)
            } else {
                Log.e(TAG, "Login anónimo falló: usuario nulo")
                Result.failure(Exception("Usuario nulo después del login"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en login anónimo", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verifica que haya un usuario autenticado con Google
     * En producción, rechaza usuarios anónimos
     */
    fun requireGoogleSignedIn(): Result<FirebaseUser> {
        val currentUser = getCurrentUser()
        
        return when {
            currentUser == null -> {
                Log.e(TAG, "❌ No hay usuario autenticado")
                Result.failure(Exception("Usuario no autenticado. Debe hacer login con Google."))
            }
            currentUser.isAnonymous -> {
                Log.e(TAG, "❌ Usuario anónimo no permitido en producción")
                Result.failure(Exception("Login con Google requerido"))
            }
            else -> {
                Log.d(TAG, "✅ Usuario autenticado con Google: ${currentUser.email}")
                Result.success(currentUser)
            }
        }
    }
    
    /**
     * Asegura que haya un usuario autenticado (fallback a anónimo solo para testing)
     * DEPRECATED en producción - usar requireGoogleSignedIn()
     */
    @Deprecated("Usar requireGoogleSignedIn() en producción")
    suspend fun ensureUserSignedIn(): Result<FirebaseUser> {
        val currentUser = getCurrentUser()
        
        return if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: ${currentUser.uid}")
            Result.success(currentUser)
        } else {
            Log.d(TAG, "No hay usuario, realizando login anónimo...")
            signInAnonymously()
        }
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "Sesión cerrada")
    }
    
    /**
     * Obtiene el UID del usuario actual o null si no está autenticado
     */
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
