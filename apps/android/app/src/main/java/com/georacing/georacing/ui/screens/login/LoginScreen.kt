package com.georacing.georacing.ui.screens.login

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.data.firebase.FirebaseAuthService
import com.georacing.georacing.data.repository.NetworkUserRepository
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.CarbonBlack
import com.georacing.georacing.ui.theme.RacingRed
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.georacing.georacing.ui.theme.*
import androidx.compose.ui.graphics.Brush

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val backdrop = com.georacing.georacing.ui.glass.LocalBackdrop.current
    val scope = rememberCoroutineScope()
    val authService = remember { FirebaseAuthService() }
    val userRepository = remember { NetworkUserRepository() }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Web Client ID de Firebase (OAuth 2.0)
    val webClientId = "62243274149-iv3ra1epplkgsr3oeipgrej6i9r62qfs.apps.googleusercontent.com"
    
    // GoogleSignInClient (fallback para Credential Manager)
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    
    // Launcher para el resultado del Intent de Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                
                if (idToken != null) {
                    Log.d("LoginScreen", "✅ Google ID Token obtenido (GoogleSignInClient)")
                    scope.launch {
                        isLoading = true
                        val authResult = authService.signInWithGoogle(idToken)
                        
                        if (authResult.isSuccess) {
                            val user = authResult.getOrNull()
                            Log.d("LoginScreen", "✅ Login exitoso: ${user?.email}")
                            user?.let {
                                userRepository.registerUser(
                                    uid = it.uid,
                                    name = it.displayName,
                                    email = it.email,
                                    photoUrl = it.photoUrl?.toString()
                                )
                            }
                            
                            // Consultar rol del usuario en Firestore
                            var userRole = "user"
                            try {
                                val firestore = FirebaseFirestore.getInstance()
                                val doc = firestore.collection("staff_roles")
                                    .document(user?.uid ?: "")
                                    .get()
                                    .await()
                                if (doc.exists()) {
                                    userRole = doc.getString("role") ?: "user"
                                    Log.d("LoginScreen", "🔑 Rol del usuario: $userRole")
                                }
                            } catch (e: Exception) {
                                Log.w("LoginScreen", "No se pudo consultar rol, default=user", e)
                            }
                            
                            // Redirigir según rol
                            when (userRole) {
                                "staff", "admin" -> {
                                    Log.d("LoginScreen", "🔐 Redirigiendo a modo Staff")
                                    navController.navigate(Screen.StaffMode.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                else -> {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        } else {
                            errorMessage = "Error al autenticar con Firebase"
                            Log.e("LoginScreen", "Error Firebase", authResult.exceptionOrNull())
                        }
                        isLoading = false
                    }
                } else {
                    errorMessage = "No se pudo obtener el token de Google"
                    isLoading = false
                }
            } catch (e: ApiException) {
                Log.e("LoginScreen", "Error Google Sign-In: ${e.statusCode}", e)
                errorMessage = "Error al iniciar sesión con Google"
                isLoading = false
            }
        } else {
            Log.d("LoginScreen", "Login cancelado por el usuario")
            isLoading = false
        }
    }
    
    // Log de debug al entrar en la pantalla
    LaunchedEffect(Unit) {
        Log.d("LoginScreen", "🎬 LoginScreen cargada correctamente")
    }
    
    // Animación de pulsación del logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    fun handleGoogleSignIn() {
        Log.d("LoginScreen", "🚀 Iniciando Google Sign-In con GoogleSignInClient (compatible con emuladores)")
        isLoading = true
        errorMessage = null
        
        // Usar Intent directo de Google Sign-In
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06060C),
                        Color(0xFF0A0A12),
                        Color(0xFF0E0E18),
                        Color(0xFF080810)
                    )
                )
            )
    ) {
        // ── Ambient racing glows ──
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            // Racing red glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE8253A).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Icono con glow sutil
            Box(contentAlignment = Alignment.Center) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    RacingRed.copy(alpha = 0.15f * pulseScale),
                                    Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "GeoRacing",
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                    tint = RacingRed
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Título con accent dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(RacingRed, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GEORACING",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = TextPrimary
                )
            }
            
            Text(
                text = "CIRCUIT DE BARCELONA CATALUNYA",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                modifier = Modifier.padding(top = 8.dp),
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            // Texto de bienvenida
            Text(
                text = "La experiencia racing\ndefinitiva",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Inicia sesión con tu cuenta de Google\npara acceder a todas las funciones",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Botón de Google Sign-In (Custom Styled)
            // Botón de Google Sign-In (Liquid Glass Styled)
            com.georacing.georacing.ui.glass.LiquidButton(
                onClick = { handleGoogleSignIn() },
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                isInteractive = !isLoading,
                surfaceColor = Color.White.copy(alpha = 0.9f)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = RacingRed,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Icono de Google
                        Icon(
                            imageVector = Icons.Default.Speed, // Placeholder, idealmente logo G real
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black 
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "CONTINUAR CON GOOGLE",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }
            
            // Mensaje de error
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(12.dp),
                            level = GlassLevel.L1
                        )
                        .background(CircuitStop.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = StatusRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Términos y condiciones
            Text(
                text = "Al continuar, aceptas nuestros términos\ny condiciones de uso",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
