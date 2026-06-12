package com.georacing.georacing.ui.screens.incidents

import android.app.Application
import android.graphics.ImageDecoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.domain.model.IncidentCategory
import com.georacing.georacing.domain.repository.IncidentsRepository
import com.georacing.georacing.ui.components.background.CarbonBackground
import com.georacing.georacing.ui.components.GlassCard
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.components.RacingButton
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.*

/**
 * IncidentReportScreen — Premium Incident Report HUD
 * Reportar incidencias al staff con fotos y categoría.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentReportScreen(
    navController: NavController,
    incidentsRepository: IncidentsRepository
) {
    val backdrop = LocalBackdrop.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as Application

    val viewModel: IncidentViewModel = viewModel(
        factory = viewModelFactory {
            initializer { IncidentViewModel(application, incidentsRepository) }
        }
    )

    var category by remember { mutableStateOf(IncidentCategory.OTRA) }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showContent = true }

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

    Box(Modifier.fillMaxSize()) {
        CarbonBackground()

        Column(Modifier.fillMaxSize()) {
            // ── Premium LiquidTopBar ──
            LiquidTopBar(
                backdrop = backdrop,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(StatusAmber))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(stringResource(R.string.incident_report_title).uppercase(), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text("Reporte directo al staff", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                },
                actions = {
                    HomeIconButton {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )

            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Help text
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -15 }
                    ) {
                        Text(
                            "El staff recibirá tu reporte y lo atenderá lo antes posible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }

                    // ── Category Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, 100)) + slideInVertically(tween(400, 100)) { 20 }
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.incident_category_label).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = TextTertiary,
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
                                        Icon(Icons.Default.ArrowDropDown, "Expandir", tint = RacingRed)
                                    },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = RacingRed,
                                        unfocusedBorderColor = MetalGrey,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = RacingRed,
                                        focusedContainerColor = AsphaltGrey.copy(alpha = 0.5f),
                                        unfocusedContainerColor = AsphaltGrey.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .background(AsphaltGrey)
                                        .border(1.dp, MetalGrey.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                ) {
                                    IncidentCategory.values().forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                                            onClick = { category = item; expanded = false },
                                            colors = MenuDefaults.itemColors(
                                                textColor = TextPrimary,
                                                leadingIconColor = TextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Description Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, 200)) + slideInVertically(tween(400, 200)) { 20 }
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.incident_description_label).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = TextTertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.incident_description_hint),
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RacingRed,
                                    unfocusedBorderColor = MetalGrey,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = RacingRed,
                                    focusedContainerColor = AsphaltGrey.copy(alpha = 0.5f),
                                    unfocusedContainerColor = AsphaltGrey.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Photos Section ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(400, 300)) + slideInVertically(tween(400, 300)) { 20 }
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.incident_photos_label).uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = TextTertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (selectedPhotoUri != null) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val ctx = androidx.compose.ui.platform.LocalContext.current
                                        val bitmap = remember(selectedPhotoUri) {
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                    val source = ImageDecoder.createSource(ctx.contentResolver, selectedPhotoUri!!)
                                                    ImageDecoder.decodeBitmap(source)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    android.provider.MediaStore.Images.Media.getBitmap(ctx.contentResolver, selectedPhotoUri)
                                                }
                                            } catch (_: Exception) { null }
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Foto seleccionada",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(150.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text("Tocar para cambiar", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(StatusAmber.copy(alpha = 0.12f))
                                                .drawBehind {
                                                    drawCircle(StatusAmber.copy(alpha = 0.06f), radius = size.minDimension * 0.9f)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AddAPhoto, stringResource(R.string.cd_camera), modifier = Modifier.size(20.dp), tint = StatusAmber)
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            stringResource(R.string.incident_add_photo),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // ── Submit Button ──
                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn(tween(500, 450)) + slideInVertically(tween(500, 450)) { 20 }
                    ) {
                        RacingButton(
                            text = stringResource(R.string.incident_send),
                            onClick = { viewModel.sendIncident(category, description) },
                            enabled = description.isNotBlank() && !isProcessing
                        )
                    }

                    Spacer(Modifier.height(100.dp))
                }

                // ── Loading Overlay ──
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CarbonBlack.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val pulseAnim = rememberInfiniteTransition(label = "proc")
                            val pulseScale by pulseAnim.animateFloat(
                                initialValue = 0.9f, targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s"
                            )
                            Box(
                                Modifier
                                    .size(64.dp)
                                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                                    .drawBehind {
                                        drawCircle(RacingRed.copy(alpha = 0.15f), radius = size.minDimension / 2)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = RacingRed, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Procesando imagen...",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Snackbar
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
