package com.georacing.georacing.ui.screens.share

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georacing.georacing.data.model.ShareSession
import com.georacing.georacing.data.repository.NetworkGroupRepository
import com.georacing.georacing.data.repository.ShareSessionRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel para gestionar sesiones QR (Simplificado para usar API NAS)
 */
class ShareQRViewModel(
    private val repository: NetworkGroupRepository = NetworkGroupRepository(),
    private val userPreferences: com.georacing.georacing.data.local.UserPreferencesDataStore
) : ViewModel() {
    
    // ... existing StateFlows ...
    private val _currentSession = MutableStateFlow<ShareSession?>(null)
    val currentSession: StateFlow<ShareSession?> = _currentSession.asStateFlow()
    
    private val _qrBitmap = MutableStateFlow<Bitmap?>(null)
    val qrBitmap: StateFlow<Bitmap?> = _qrBitmap.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _groupMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<GroupMember>> = _groupMembers.asStateFlow()

    // Repositorio dedicado para sesiones QR
    private val shareRepository = ShareSessionRepository()
    
    companion object {
        private const val TAG = "ShareQRViewModel"
        private const val QR_SIZE = 512
    }
    
    data class GroupMember(
        val userId: String,
        val displayName: String,
        val photoUrl: String,
        val joinedAt: Date
    )
    
    fun createNewGroup(groupName: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val groupId = UUID.randomUUID().toString().substring(0, 8).uppercase()
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid ?: "anon_${UUID.randomUUID()}"
                
                repository.createGroup(groupId, userId, groupName)
                
                // Persist
                userPreferences.setActiveGroupId(groupId)
                
                // Setup Session
                generateQRSession(groupId)
                
                Log.d(TAG, "Group Created: $groupId ($groupName)")
            } catch (e: Exception) {
                _errorMessage.value = "Error creating group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadSessionIfActive(groupId: String) {
        if (_currentSession.value == null) {
            generateQRSession(groupId)
        }
    }
    
    fun generateQRSession(groupId: String, eventDate: Date = Date()) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val sessionId = groupId
                val session = ShareSession(
                    sessionId = sessionId,
                    ownerId = "current",
                    ownerName = "User",
                    eventDate = com.google.firebase.Timestamp(eventDate),
                    expiresAt = com.google.firebase.Timestamp(Date(System.currentTimeMillis() + 86400000)),
                    createdAt = com.google.firebase.Timestamp.now(),
                    isActive = true,
                    groupId = groupId
                )
                _currentSession.value = session
                generateQRCode(groupId)
                listenToGroupMembers(groupId)
            } catch (e: Exception) {
                 _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun listenToGroupMembers(groupId: String) {
        viewModelScope.launch {
             val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
             val currentUserId = currentUser?.uid
             repository.getGroupMembers(groupId).collectLatest { locations ->
                 _groupMembers.value = locations.map { loc ->
                     GroupMember(
                         userId = loc.userId,
                         displayName = loc.displayName ?: "Usuario",
                         photoUrl = loc.photoUrl ?: "",
                         joinedAt = loc.lastUpdated.toDate()
                     )
                 }
             }
        }
    }
    
    fun joinSessionByCode(code: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // El código escaneado es el sessionId (que actualmente es igual al groupId)
                val result = shareRepository.joinSessionGroup(code)
                
                if (result.isSuccess) {
                    val groupId = result.getOrNull()!!
                    userPreferences.setActiveGroupId(groupId)
                    Log.d(TAG, "✅ Unido al grupo exitosamente: $groupId")
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "❌ Error uniéndose al grupo con código: $code", exception)
                    _errorMessage.value = "Error al unirse: ${exception?.message ?: "Desconocido"}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Excepción en joinSessionByCode", e)
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ... generateQRCode ...
    private fun generateQRCode(content: String) {
        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
            val bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565)
            for (x in 0 until QR_SIZE) {
                for (y in 0 until QR_SIZE) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            _qrBitmap.value = bitmap
        } catch (e: Exception) {
            _errorMessage.value = "Error generating QR: ${e.message}"
        }
    }

    fun clearError() { _errorMessage.value = null }
    fun deactivateCurrentSession() { _currentSession.value = null; _qrBitmap.value = null }
    override fun onCleared() { super.onCleared(); deactivateCurrentSession() }
}
