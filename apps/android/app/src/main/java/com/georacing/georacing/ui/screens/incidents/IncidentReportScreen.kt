package com.georacing.georacing.ui.screens.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.domain.repository.IncidentsRepository
import com.georacing.georacing.domain.model.IncidentCategory
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.components.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentReportScreen(
    navController: NavController,
    incidentsRepository: IncidentsRepository
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as Application
    
    val viewModel: IncidentViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                IncidentViewModel(application, incidentsRepository)
            }
        }
    )

    var category by rememberSaveable { mutableStateOf(IncidentCategory.OTRA) }
    var description by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe ViewModel states
    val isProcessing by viewModel.isProcessing.collectAsState()
    val selectedPhotoUri by viewModel.selectedPhotoUri.collectAsState()
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.setPhotoUri(uri)
    }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is IncidentViewModel.UiEvent.Success -> {
                    snackbarHostState.showSnackbar("Incidencia enviada al staff correctamente")
                    navController.popBackStack()
                }
                is IncidentViewModel.UiEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
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
                            stringResource(R.string.incident_report_title).uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFFF8FAFC)
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, 
                                contentDescription = stringResource(R.string.cd_back),
                                tint = Color(0xFFF8FAFC)
                            )
                        }
                    },
                    actions = {
                        com.georacing.georacing.ui.components.HomeIconButton {
                            navController.navigate(com.georacing.georacing.ui.navigation.Screen.Home.route) {
                                popUpTo(com.georacing.georacing.ui.navigation.Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color(0xFFF8FAFC),
                        navigationIconContentColor = Color(0xFFF8FAFC)
                    )
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Texto de ayuda
                Text(
                    text = "El staff recibirá tu reporte y lo atenderá lo antes posible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Categoría
                Text(
                    text = stringResource(R.string.incident_category_label).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown, 
                                contentDescription = "Expandir",
                                tint = Color(0xFFE8253A)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE8253A),
                            unfocusedBorderColor = Color(0xFF14141C),
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            cursorColor = Color(0xFFE8253A),
                            focusedContainerColor = Color(0xFF0E0E18),
                            unfocusedContainerColor = Color(0xFF0E0E18)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF14141C)).border(1.dp, Color(0xFF0E0E18))
                    ) {
                        IncidentCategory.values().forEach { item ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        item.displayName,
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.SemiBold
                                    ) 
                                },
                                onClick = {
                                    category = item
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = Color(0xFFF8FAFC),
                                    leadingIconColor = Color(0xFFF8FAFC)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Descripción
                Text(
                    text = stringResource(R.string.incident_description_label).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { 
                        Text(
                            stringResource(R.string.incident_description_hint),
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE8253A),
                        unfocusedBorderColor = Color(0xFF14141C),
                        focusedTextColor = Color(0xFFF8FAFC),
                        unfocusedTextColor = Color(0xFFF8FAFC),
                        cursorColor = Color(0xFFE8253A),
                        focusedContainerColor = Color(0xFF0E0E18),
                        unfocusedContainerColor = Color(0xFF0E0E18)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Sección de fotos
                Text(
                    text = stringResource(R.string.incident_photos_label).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (selectedPhotoUri != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display selected photo using native Compose
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val bitmap = remember(selectedPhotoUri) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        val source = ImageDecoder.createSource(context.contentResolver, selectedPhotoUri!!)
                                        ImageDecoder.decodeBitmap(source)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, selectedPhotoUri)
                                    }
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto seleccionada",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tocar para cambiar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF97316).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = stringResource(R.string.cd_camera),
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFF97316)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.incident_add_photo),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón enviar
                RacingButton(
                    text = stringResource(R.string.incident_send),
                    onClick = { viewModel.sendIncident(category, description) },
                    enabled = description.isNotBlank() && !isProcessing
                )
            }
        }
        
        // Loading overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080810).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE8253A),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Procesando imagen...",
                        color = Color(0xFFF8FAFC),
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
