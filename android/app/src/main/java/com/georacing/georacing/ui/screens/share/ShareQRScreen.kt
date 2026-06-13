package com.georacing.georacing.ui.screens.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.components.*
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareQRScreen(
    navController: NavController,
    groupId: String,
    viewModel: ShareQRViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
                val app = extras[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application
                return ShareQRViewModel(
                     userPreferences = com.georacing.georacing.data.local.UserPreferencesDataStore(app.applicationContext)
                ) as T
            }
        }
    )
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()
    
    LaunchedEffect(groupId) {
        viewModel.loadSessionIfActive(groupId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "COMPARTIR P2P",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF8FAFC)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            viewModel.deactivateCurrentSession()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color(0xFFF8FAFC))
                        }
                    },
                    actions = {
                        HomeIconButton {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFFF8FAFC)
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Título
                Text(
                    text = "COMPARTIR CON QR",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF8FAFC),
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Comparte tu ubicación durante el evento del día",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Tarjeta con QR
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color(0xFFE8253A))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Generando código QR...", color = Color(0xFF64748B), letterSpacing = 0.5.sp)
                        } else if (qrBitmap != null && currentSession != null) {
                            // Mostrar QR
                            QRCodeDisplay(
                                bitmap = qrBitmap!!,
                                session = currentSession!!
                            )
                        } else {
                            // Sin sesión activa
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(Color(0xFF06B6D4).copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF06B6D4).copy(alpha = 0.6f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "No hay código QR activo",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Genera uno nuevo para compartir tu ubicación hoy",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones de acción
                if (currentSession != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.deactivateCurrentSession() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desactivar", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                        
                        RacingButton(
                            text = "Renovar",
                            onClick = { viewModel.generateQRSession(groupId, Date()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    RacingButton(
                        text = "Generar Código QR",
                        onClick = { viewModel.generateQRSession(groupId, Date()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Lista de miembros que se han unido
                if (groupMembers.isNotEmpty()) {
                    HorizontalDivider(color = Color(0xFF14141C))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "MIEMBROS UNIDOS (${groupMembers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            groupMembers.forEach { member ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color(0xFF06B6D4)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = member.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Divider
                HorizontalDivider(color = Color(0xFF14141C))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Sección para escanear
                Text(
                    text = "¿TIENES UN CÓDIGO QR?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFF8FAFC),
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Escanéalo para unirte a un grupo y ver ubicaciones",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF64748B)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { 
                        // TODO: Implementar escáner QR con CameraX
                        navController.navigate("qr_scanner")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4))
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF06B6D4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ESCANEAR CÓDIGO QR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                
                // Mostrar errores
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFEF4444),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QRCodeDisplay(
    bitmap: Bitmap,
    session: com.georacing.georacing.data.model.ShareSession
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR Code - NEEDS WHITE BACKGROUND
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Código QR",
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info de la sesión
        Text(
            text = "VÁLIDO HASTA:",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = dateFormat.format(session.expiresAt.toDate()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE8253A)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Creado por: ${session.ownerName}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
        )
    }
}
