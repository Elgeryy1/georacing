package com.georacing.georacing.ui.screens.moments

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.georacing.georacing.ui.components.HomeIconButton
import com.georacing.georacing.ui.navigation.Screen
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private data class Moment(
    val id: String,
    val title: String,
    val zone: String,
    val timestamp: LocalDateTime,
    val emoji: String, // Representing the photo visually
    val color: Color,
    val likes: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Mis Momentos", "Comunidad", "Favoritos")
    val context = LocalContext.current

    // ── Captura de cámara ──
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedMoments by remember { mutableStateOf<List<Moment>>(emptyList()) }
    
    // Crear URI para la foto antes de lanzar la cámara
    val createImageUri: () -> Uri? = {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "GeoRacing_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GeoRacing")
            }
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedPhotoUri != null) {
            val newMoment = Moment(
                id = "cap_${System.currentTimeMillis()}",
                title = "Momento capturado",
                zone = "Circuit de BCN-CAT",
                timestamp = LocalDateTime.now(),
                emoji = "📷",
                color = Color(0xFF3B82F6),
                likes = 0
            )
            capturedMoments = listOf(newMoment) + capturedMoments
            Toast.makeText(context, "📸 ¡Momento guardado en galería!", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri()
            if (uri != null) {
                capturedPhotoUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Simulated gallery
    val myMoments = remember {
        listOf(
            Moment("1", "Salida de la carrera", "Recta principal", LocalDateTime.now().minusHours(2), "🏁", Color(0xFFE8253A), 24),
            Moment("2", "Parrilla de salida", "Pit Lane", LocalDateTime.now().minusHours(3), "🏎️", Color(0xFF3B82F6), 18),
            Moment("3", "Mi grada", "Tribuna G", LocalDateTime.now().minusHours(4), "🎟️", Color(0xFFA855F7), 7),
            Moment("4", "Con amigos", "Fan Zone", LocalDateTime.now().minusHours(5), "👥", Color(0xFF22C55E), 31),
            Moment("5", "Atardecer en pista", "Curva 5", LocalDateTime.now().minusHours(1), "🌅", Color(0xFFFF6B2C), 42),
            Moment("6", "Podio", "Recta principal", LocalDateTime.now().minusMinutes(30), "🏆", Color(0xFFD4A855), 89)
        )
    }
    val communityMoments = remember {
        listOf(
            Moment("c1", "Vuelta rápida!", "Curva 9", LocalDateTime.now().minusMinutes(15), "⚡", Color(0xFF00E5FF), 156),
            Moment("c2", "Pit stop increíble", "Pit Lane", LocalDateTime.now().minusMinutes(25), "🔧", Color(0xFFFFA726), 234),
            Moment("c3", "Adelantamiento T1", "Curva 1", LocalDateTime.now().minusMinutes(40), "💨", Color(0xFF3B82F6), 312),
            Moment("c4", "El ambiente!", "Fan Zone", LocalDateTime.now().minusHours(1), "🎉", Color(0xFFEC4899), 198),
            Moment("c5", "Desde mi grada", "Tribuna A", LocalDateTime.now().minusHours(2), "📸", Color(0xFF8B5CF6), 87),
            Moment("c6", "Recta de meta", "Recta principal", LocalDateTime.now().minusMinutes(5), "🚀", Color(0xFFEF4444), 445)
        )
    }

    val displayedMoments = when (selectedTab) {
        0 -> capturedMoments + myMoments
        1 -> communityMoments
        else -> (capturedMoments + myMoments + communityMoments).filter { it.likes > 20 || it.id.startsWith("cap_") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Momentos", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver atrás") } },
                actions = { HomeIconButton { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                },
                containerColor = Color(0xFFE8253A),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capturar momento")
            }
        },
        containerColor = Color(0xFF080810)
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Location
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFE8253A), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Circuit de Barcelona-Catalunya", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
            }

            Spacer(Modifier.height(12.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.width(tabPositions[selectedTab].contentWidth),
                            color = Color(0xFFE8253A)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 13.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stats bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${displayedMoments.size} momentos", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                Text("📸 Hoy en el circuito", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            }

            Spacer(Modifier.height(8.dp))

            // Photo Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(displayedMoments) { moment ->
                    MomentCard(moment)
                }
            }
        }
    }
}

@Composable
private fun MomentCard(moment: Moment) {
    var isLiked by remember { mutableStateOf(false) }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            // Visual area (emoji as "photo" placeholder)
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.65f)
                    .background(
                        Brush.verticalGradient(
                            listOf(moment.color.copy(alpha = 0.3f), moment.color.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(moment.emoji, fontSize = 48.sp)
            }

            // Gradient overlay at bottom
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF14141C))))
            )

            // Info at bottom
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            ) {
                Text(moment.title, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(moment.zone, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                    Text(" · ", color = Color(0xFF64748B))
                    Text(moment.timestamp.format(timeFormatter), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                }
            }

            // Like button
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { isLiked = !isLiked }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Me gusta",
                    tint = if (isLiked) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("${moment.likes + if (isLiked) 1 else 0}", fontSize = 11.sp, color = Color.White)
            }
        }
    }
}
