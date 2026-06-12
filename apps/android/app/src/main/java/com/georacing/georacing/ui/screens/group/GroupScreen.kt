package com.georacing.georacing.ui.screens.group

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.data.model.GroupMemberLocation
import com.georacing.georacing.data.repository.NetworkGroupRepository
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.components.*
import kotlinx.coroutines.launch
import com.georacing.georacing.data.local.UserPreferencesDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    navController: NavController, 
    userPreferences: UserPreferencesDataStore,
    viewModel: GroupMapViewModel
) {
    val context = LocalContext.current
    
    // ViewModel hoisted to Navigation Graph level
    
    val groupLocations by viewModel.groupLocations.collectAsState()
    val isSharingLocation by viewModel.isSharingLocation.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Obtener UID del usuario actual
    val currentUserId = remember { 
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"
    }
    
    val scope = rememberCoroutineScope()
    // userPreferences passed as parameter
    
    // Obtener ID del grupo activo desde DataStore (Sincronizado con ShareQRViewModel)
    val activeGroupId by userPreferences.activeGroupId.collectAsState(initial = null)
    val groupId = activeGroupId
    
    val defaultGroupId = remember(currentUserId) { "group_${currentUserId}_${System.currentTimeMillis()}" }
    
    // Launcher para pedir permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (granted) {
            viewModel.checkLocationPermission(context)
            viewModel.startSharingLocation(context)
        }
    }
    
    // Iniciar listener cuando haya un grupo activo
    LaunchedEffect(groupId) {
        viewModel.checkLocationPermission(context)
        if (groupId != null) {
            viewModel.startListeningGroupLocations(groupId, context)
        } else {
            viewModel.stopSharingLocation(context)
            viewModel.clearGroupData()
        }
    }
    
    // Detener listener al salir
    DisposableEffect(Unit) {
        onDispose {
            // No detenemos el sharing al salir
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
                            "MI GRUPO",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            ),
                            color = TextPrimary
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = TextPrimary)
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
                        titleContentColor = TextPrimary
                    )
                )
            },
            snackbarHost = {
                errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("OK", color = RacingRed)
                            }
                        },
                        containerColor = AsphaltGrey,
                        contentColor = TextPrimary
                    ) {
                        Text(error)
                    }
                }
            }
        ) { innerPadding ->
            if (groupId != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        LocationSharingCard(
                            isSharing = isSharingLocation,
                            hasPermission = hasLocationPermission,
                            onToggle = {
                                if (!hasLocationPermission) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    viewModel.toggleSharingLocation(context)
                                }
                            }
                        )
                    }
                    item {
                        MapStubCard(
                            memberCount = groupLocations.filter { it.sharing }.size,
                            onOpenMap = {
                                navController.navigate(com.georacing.georacing.ui.navigation.Screen.GroupMap.route)
                            }
                        )
                    }
                    item {
                        ShareQRCard(
                            onOpenQR = {
                                navController.navigate(com.georacing.georacing.ui.navigation.Screen.ShareQR.createRoute(groupId))
                            }
                        )
                    }
                    item {
                        LeaveGroupCard(
                            onLeaveGroup = {
                                viewModel.leaveGroup(context)
                                scope.launch { userPreferences.setActiveGroupId(null) }
                                navController.popBackStack() // Volver atrás al salir
                            }
                        )
                    }
                    item {
                        Text(
                            text = "MIEMBROS DEL GRUPO (${groupLocations.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color(0xFF64748B),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                    if (groupLocations.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay miembros en el grupo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextTertiary
                                )
                            }
                        }
                    } else {
                        items(groupLocations) { member ->
                            GroupMemberItem(
                                member = member,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    NoActiveGroupCard(
                        onCreateGroup = {
                            scope.launch { userPreferences.setActiveGroupId(defaultGroupId) }
                            // activeGroupId will update via Flow
                            viewModel.startListeningGroupLocations(defaultGroupId, context)
                        },
                        onJoinWithQr = {
                            navController.navigate(com.georacing.georacing.ui.navigation.Screen.QRScanner.route)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun GroupScreenPreview() {
    val context = LocalContext.current
    val viewModel: GroupMapViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                GroupMapViewModel(NetworkGroupRepository())
            }
        }
    )
    
    GroupScreen(
        navController = androidx.navigation.compose.rememberNavController(),
        userPreferences = com.georacing.georacing.data.local.UserPreferencesDataStore(context),
        viewModel = viewModel
    )
}

@Preview
@Composable
fun LocationSharingCardPreview() {
    LocationSharingCard(
        isSharing = true,
        hasPermission = true,
        onToggle = {}
    )
}

@Preview
@Composable
fun MapStubCardPreview() {
    MapStubCard(
        memberCount = 5,
        onOpenMap = {}
    )
}

@Composable
fun LocationSharingCard(
    isSharing: Boolean,
    hasPermission: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "COMPARTIR UBICACIÓN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSharing) {
                        "Tu grupo puede ver dónde estás"
                    } else if (!hasPermission) {
                        "Se necesitan permisos de ubicación"
                    } else {
                        "Tu grupo no puede verte"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Switch(
                checked = isSharing,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFF8FAFC),
                    checkedTrackColor = Color(0xFF22C55E),
                    uncheckedThumbColor = TextTertiary,
                    uncheckedTrackColor = Color(0xFF0E0E18)
                )
            )
        }
    }
}

@Composable
fun MapStubCard(
    memberCount: Int,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(horizontal = 16.dp)
            .liquidGlass(shape = RoundedCornerShape(24.dp), level = GlassLevel.L2)
            .clickable { onOpenMap() }
    ) {
        // Placeholder background (could be an image)
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E0E18)))
        
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MAPA DEL CIRCUITO",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Circuit de Barcelona-Catalunya",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = "$memberCount miembros activos",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = Color(0xFF06B6D4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            RacingButton(
                text = "Ver Mapa Completo",
                onClick = onOpenMap,
                modifier = Modifier.width(200.dp)
            )
        }
    }
}

@Composable
fun GroupMemberItem(
    member: GroupMemberLocation,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(12.dp), level = GlassLevel.L1)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(RacingRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar del miembro",
                tint = RacingRed,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.displayName ?: "Usuario",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = member.getStatusText(),
                style = MaterialTheme.typography.bodySmall,
                color = if (member.sharing && member.getSecondsAgo() < 60) 
                    CircuitGreen
                else 
                    TextTertiary
            )
        }
        
        // Indicador
        if (member.sharing) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (member.getSecondsAgo() < 60) CircuitGreen else CircuitCongestion
                    )
            )
        }
    }
}

@Composable
fun ShareQRCard(
    onOpenQR: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenQR() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC).copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.QrCode,
                    contentDescription = "Código QR",
                    modifier = Modifier.size(24.dp),
                    tint = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "COMPARTIR CON QR",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Genera un código QR para compartir",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ir a compartir QR",
                tint = TextTertiary
            )
        }
    }
}

@Composable
fun NoActiveGroupCard(
    onCreateGroup: () -> Unit,
    onJoinWithQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "NO HAY GRUPO ACTIVO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "Crea un grupo o únete para ver a tus amigos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            RacingButton(
                text = "Crear Nuevo Grupo",
                onClick = onCreateGroup
            )
            
            OutlinedButton(
                onClick = onJoinWithQr,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.QrCode, "Unirme con código QR")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "UNIRME CON QR",
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LeaveGroupCard(
    onLeaveGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { showConfirmDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Salir del grupo",
                tint = Color(0xFFEF4444)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SALIR DEL GRUPO",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color(0xFFEF4444)
                )
                Text(
                    text = "Detener ubicación y salir",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ir a salir del grupo",
                tint = TextTertiary
            )
        }
    }
    
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF14141C),
            title = {
                Text(
                    "¿Salir del grupo?",
                    color = Color(0xFFF8FAFC),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Dejarás de compartir tu ubicación con el grupo y no podrás ver a los demás miembros.",
                    color = Color(0xFF64748B)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onLeaveGroup()
                }) {
                    Text("SALIR", color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("CANCELAR", color = Color(0xFF64748B))
                }
            },
            modifier = Modifier.border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
        )
    }
}
