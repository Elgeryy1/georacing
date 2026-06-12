package com.georacing.georacing.ui.components.toast

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ToastType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class ToastData(
    val message: String,
    val type: ToastType,
    val duration: Long = 3000L
)

class ToastManager {
    private val _currentToast = MutableStateFlow<ToastData?>(null)
    val currentToast: StateFlow<ToastData?> = _currentToast
    
    fun showToast(message: String, type: ToastType = ToastType.INFO, duration: Long = 3000L) {
        _currentToast.value = ToastData(message, type, duration)
    }
    
    fun hideToast() {
        _currentToast.value = null
    }
}

@Composable
fun ToastHost(toastManager: ToastManager) {
    val currentToast by toastManager.currentToast.collectAsState()
    
    LaunchedEffect(currentToast) {
        currentToast?.let { toast ->
            delay(toast.duration)
            toastManager.hideToast()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = currentToast != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            currentToast?.let { toast ->
                ToastCard(toast)
            }
        }
    }
}

@Composable
fun ToastCard(toast: ToastData) {
    val (bgColor, borderColor, icon) = when (toast.type) {
        ToastType.SUCCESS -> Triple(
            Color(0xFF0A2A1A),
            Color(0xFF22C55E),
            Icons.Default.CheckCircle
        )
        ToastType.ERROR -> Triple(
            Color(0xFF2A0A0A),
            Color(0xFFEF4444),
            Icons.Default.Error
        )
        ToastType.WARNING -> Triple(
            Color(0xFF2A1A05),
            Color(0xFFFFA726),
            Icons.Default.Warning
        )
        ToastType.INFO -> Triple(
            Color(0xFF0A1A2A),
            Color(0xFF06B6D4),
            Icons.Default.Info
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14141C)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = toast.message,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
