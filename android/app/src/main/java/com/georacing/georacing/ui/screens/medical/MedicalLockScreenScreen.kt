package com.georacing.georacing.ui.screens.medical

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georacing.georacing.data.local.GeoRacingDatabase
import com.georacing.georacing.utils.MedicalLockScreenGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalLockScreenScreen(
    navController: NavController,
    userQrData: String = "GEORACING_USER_001"  // ID de entrada del usuario
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Database
    val database = remember { GeoRacingDatabase.getInstance(context) }
    val medicalInfoDao = database.medicalInfoDao()
    
    // States
    var userName by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var medicalNotes by remember { mutableStateOf("") }
    
    var showPreview by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Cargar datos guardados
    LaunchedEffect(Unit) {
        medicalInfoDao.getMedicalInfoOnce()?.let { info ->
            bloodType = info.bloodType ?: ""
            allergies = info.allergies ?: ""
            emergencyContact = info.emergencyContactName ?: ""
            emergencyPhone = info.emergencyContactPhone ?: ""
            medicalNotes = info.medicalNotes ?: ""
        }
    }
    
    // Dropdown para grupo sanguíneo
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    var bloodTypeExpanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🆘 Lock Screen Médico",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color(0xFFE2E8F0))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0E0E18),
                    titleContentColor = Color(0xFFE2E8F0)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))
                    )
                )
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Explicación
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF14141C)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚠️ Información Vital",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Esta información se mostrará en tu pantalla de bloqueo para que el personal de emergencia pueda verla sin desbloquear tu teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
            
            // Nombre
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Tu Nombre") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Grupo Sanguíneo (Dropdown)
            ExposedDropdownMenuBox(
                expanded = bloodTypeExpanded,
                onExpandedChange = { bloodTypeExpanded = it }
            ) {
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Grupo Sanguíneo") },
                    leadingIcon = { Icon(Icons.Default.Favorite, null, tint = Color(0xFFEF4444)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = bloodTypeExpanded,
                    onDismissRequest = { bloodTypeExpanded = false }
                ) {
                    bloodTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                bloodType = type
                                bloodTypeExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Alergias
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text("Alergias (separadas por coma)") },
                leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFFA726)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            // Contacto de Emergencia
            OutlinedTextField(
                value = emergencyContact,
                onValueChange = { emergencyContact = it },
                label = { Text("Nombre Contacto Emergencia") },
                leadingIcon = { Icon(Icons.Default.Call, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = emergencyPhone,
                onValueChange = { emergencyPhone = it },
                label = { Text("Teléfono Emergencia") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // Notas médicas
            OutlinedTextField(
                value = medicalNotes,
                onValueChange = { medicalNotes = it },
                label = { Text("Notas Médicas (diabetes, epilepsia, etc.)") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón Preview
            Button(
                onClick = { showPreview = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF06B6D4)
                )
            ) {
                Icon(Icons.Default.Search, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vista Previa")
            }
            
            // Botón Guardar en Galería
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        
                        // Guardar en DB
                        medicalInfoDao.saveMedicalInfo(
                            com.georacing.georacing.data.local.entities.MedicalInfoEntity(
                                bloodType = bloodType.takeIf { it.isNotBlank() },
                                allergies = allergies.takeIf { it.isNotBlank() },
                                emergencyContactName = emergencyContact.takeIf { it.isNotBlank() },
                                emergencyContactPhone = emergencyPhone.takeIf { it.isNotBlank() },
                                medicalNotes = medicalNotes.takeIf { it.isNotBlank() }
                            )
                        )
                        
                        // Generar bitmap
                        val bitmap = MedicalLockScreenGenerator.generateBitmap(
                            qrData = userQrData,
                            userName = userName.ifBlank { "USUARIO" },
                            bloodType = bloodType.takeIf { it.isNotBlank() },
                            allergies = allergies.takeIf { it.isNotBlank() },
                            emergencyContact = emergencyContact.takeIf { it.isNotBlank() },
                            emergencyPhone = emergencyPhone.takeIf { it.isNotBlank() },
                            medicalNotes = medicalNotes.takeIf { it.isNotBlank() }
                        )
                        
                        // Guardar en galería
                        val uri = MedicalLockScreenGenerator.saveToGallery(context, bitmap)
                        
                        isLoading = false
                        
                        if (uri != null) {
                            Toast.makeText(context, "✅ Imagen guardada en Galería", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ Error al guardar", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFF8FAFC))
                } else {
                    Icon(Icons.Default.Share, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Guardar en Galería")
            }
            
            // Botón Establecer como Wallpaper
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        
                        val bitmap = MedicalLockScreenGenerator.generateBitmap(
                            qrData = userQrData,
                            userName = userName.ifBlank { "USUARIO" },
                            bloodType = bloodType.takeIf { it.isNotBlank() },
                            allergies = allergies.takeIf { it.isNotBlank() },
                            emergencyContact = emergencyContact.takeIf { it.isNotBlank() },
                            emergencyPhone = emergencyPhone.takeIf { it.isNotBlank() },
                            medicalNotes = medicalNotes.takeIf { it.isNotBlank() }
                        )
                        
                        val success = MedicalLockScreenGenerator.setAsLockScreenWallpaper(context, bitmap)
                        
                        isLoading = false
                        
                        if (success) {
                            Toast.makeText(context, "✅ Establecido como fondo de bloqueo", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "❌ Error. Intenta guardar en galería y configurarlo manualmente.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626)
                )
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Establecer como Pantalla de Bloqueo")
            }
        }
    }
    
    // Dialog Preview
    if (showPreview) {
        val previewBitmap = remember(userName, bloodType, allergies, emergencyContact, emergencyPhone, medicalNotes) {
            MedicalLockScreenGenerator.generateBitmap(
                qrData = userQrData,
                userName = userName.ifBlank { "USUARIO" },
                bloodType = bloodType.takeIf { it.isNotBlank() },
                allergies = allergies.takeIf { it.isNotBlank() },
                emergencyContact = emergencyContact.takeIf { it.isNotBlank() },
                emergencyPhone = emergencyPhone.takeIf { it.isNotBlank() },
                medicalNotes = medicalNotes.takeIf { it.isNotBlank() }
            )
        }
        
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text("Vista Previa") },
            text = {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showPreview = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
