package com.georacing.georacing.ui.screens.splash

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.georacing.georacing.data.firebase.FirebaseInitializer
import com.georacing.georacing.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    var progress by remember { mutableFloatStateOf(0f) }
    var startAnimation by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Iniciando...") }
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "fade"
    )
    
    val carScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    
    val carOffsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 50.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "offsetY"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(300)
        
        // FASE 1: Verificar Firebase
        statusMessage = "Inicializando Firebase..."
        progress = 0.3f
        
        val firebaseInitialized = FirebaseInitializer.verifyInitialization()
        
        if (!firebaseInitialized) {
            Log.e("SplashScreen", "Firebase no inicializado")
            statusMessage = "Error de conexión"
            delay(1500)
        }
        
        // FASE 2: Verificar si hay usuario logueado con Google
        statusMessage = "Verificando sesión..."
        progress = 0.6f
        delay(500)
        
        val authService = com.georacing.georacing.data.firebase.FirebaseAuthService()
        val currentUser = authService.getCurrentUser()
        
        progress = 0.9f
        statusMessage = "Preparando experiencia..."
        delay(500)
        
        progress = 1f
        statusMessage = "¡Listo!"
        delay(300)
        
        // Decidir navegación basada en si hay usuario
        if (currentUser != null && !currentUser.isAnonymous) {
            // Usuario con Google logueado → ir a Home
            Log.d("SplashScreen", "✅ Usuario Google logueado: ${currentUser.email}")
            
            // Garantizar que la DB SQL tiene los datos (importante tras reinstalación de app)
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.georacing.georacing.data.repository.NetworkUserRepository().registerUser(
                        uid = currentUser.uid,
                        name = currentUser.displayName,
                        email = currentUser.email,
                        photoUrl = currentUser.photoUrl?.toString()
                    )
                }
            } catch (e: Exception) {
                Log.w("SplashScreen", "Error registrando usuario en background", e)
            }

            Log.d("SplashScreen", "Navegando a Home...")
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            // Sin usuario o anónimo → ir a Login
            Log.d("SplashScreen", "❌ Sin usuario con Google → Login")
            Log.d("SplashScreen", "Usuario actual: $currentUser")
            Log.d("SplashScreen", "Es anónimo: ${currentUser?.isAnonymous}")
            Log.d("SplashScreen", "Navegando a Login (route=${Screen.Login.route})...")
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06060C),
                        Color(0xFF0A0A12),
                        Color(0xFF080810)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Ambient racing glow ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Red accent glow top-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE8253A).copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.15f),
                    radius = size.width * 0.5f
                ),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.15f)
            )
            // Cyan glow bottom-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.03f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width * 0.4f
                ),
                radius = size.width * 0.4f,
                center = Offset(size.width * 0.2f, size.height * 0.8f)
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .graphicsLayer(alpha = alpha)
        ) {
            // F1 Car Icon
            F1CarIcon(modifier = Modifier
                .width(220.dp)
                .height(90.dp)
                .offset(y = carOffsetY)
                .graphicsLayer(scaleX = carScale, scaleY = carScale)
            )
            
            Spacer(modifier = Modifier.height(36.dp))
            
            // Title with racing accent dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFFE8253A), shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GEORACING",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color.White
                )
            }
            
            Text(
                text = "CIRCUIT DE BARCELONA CATALUNYA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            // Rev Counter Loader
            RevCounterLoader(progress = progress)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = Color(0xFFE8253A)
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Mensaje de estado
            Text(
                text = statusMessage.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFF475569),
                modifier = Modifier.alpha(0.9f)
            )
        }
    }
}

@Composable
fun F1CarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // Centrar el dibujo y ajustar escala
        val viewBoxWidth = 200f
        val viewBoxHeight = 60f
        val scaleX = width / viewBoxWidth
        val scaleY = height / viewBoxHeight
        val scale = scaleX.coerceAtMost(scaleY)
        
        // Centrar el contenido
        val offsetX = (width - viewBoxWidth * scale) / 2f
        val offsetY = (height - viewBoxHeight * scale) / 2f
        
        // Perfil lateral del F1
        val mainBodyPath = Path().apply {
            moveTo(5f * scale + offsetX, 45f * scale + offsetY)
            lineTo(35f * scale + offsetX, 45f * scale + offsetY)
            lineTo(35f * scale + offsetX, 40f * scale + offsetY)
            lineTo(55f * scale + offsetX, 35f * scale + offsetY)
            cubicTo(
                70f * scale + offsetX, 30f * scale + offsetY,
                85f * scale + offsetX, 25f * scale + offsetY,
                95f * scale + offsetX, 25f * scale + offsetY
            )
            lineTo(105f * scale + offsetX, 25f * scale + offsetY)
            lineTo(110f * scale + offsetX, 10f * scale + offsetY)
            cubicTo(
                120f * scale + offsetX, 10f * scale + offsetY,
                135f * scale + offsetX, 12f * scale + offsetY,
                150f * scale + offsetX, 20f * scale + offsetY
            )
            lineTo(170f * scale + offsetX, 22f * scale + offsetY)
            lineTo(170f * scale + offsetX, 5f * scale + offsetY)
            lineTo(195f * scale + offsetX, 5f * scale + offsetY)
            lineTo(195f * scale + offsetX, 25f * scale + offsetY)
            lineTo(180f * scale + offsetX, 35f * scale + offsetY)
            lineTo(195f * scale + offsetX, 45f * scale + offsetY)
            lineTo(195f * scale + offsetX, 50f * scale + offsetY)
            lineTo(45f * scale + offsetX, 50f * scale + offsetY)
            lineTo(35f * scale + offsetX, 50f * scale + offsetY)
            lineTo(35f * scale + offsetX, 45f * scale + offsetY)
            close()
        }
        
        drawPath(
            path = mainBodyPath,
            color = Color(0xFFE8253A),
            style = Stroke(width = 3f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Detalle del Halo
        drawLine(
            color = Color(0xFFFF3352),
            start = Offset(95f * scale + offsetX, 25f * scale + offsetY),
            end = Offset(115f * scale + offsetX, 25f * scale + offsetY),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
        )
        
        // Ruedas (arcos estilizados) - rueda trasera
        val rearWheelPath = Path().apply {
            moveTo(35f * scale + offsetX, 50f * scale + offsetY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 35f * scale + offsetX,
                    top = 40f * scale + offsetY,
                    right = 55f * scale + offsetX,
                    bottom = 60f * scale + offsetY
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        }
        
        drawPath(
            path = rearWheelPath,
            color = Color(0xFFE8253A).copy(alpha = 0.5f),
            style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
        )
        
        // Rueda delantera
        val frontWheelPath = Path().apply {
            moveTo(155f * scale + offsetX, 50f * scale + offsetY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = 155f * scale + offsetX,
                    top = 38f * scale + offsetY,
                    right = 185f * scale + offsetX,
                    bottom = 62f * scale + offsetY
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
        }
        
        drawPath(
            path = frontWheelPath,
            color = Color(0xFFE8253A).copy(alpha = 0.5f),
            style = Stroke(width = 2.5f * scale, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun RevCounterLoader(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    // Glow pulse on the arc
    val infiniteTransition = rememberInfiniteTransition(label = "rev_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    Box(
        modifier = modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6f
            val glowStroke = 12f
            val radius = size.minDimension / 2 - glowStroke / 2
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // Background arc
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            // Glow behind progress arc
            val sweepAngle = 270f * animatedProgress
            drawArc(
                color = Color(0xFFE8253A).copy(alpha = glowAlpha * 0.3f),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = glowStroke, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFE8253A),
                        Color(0xFFFF3352),
                        Color(0xFFE8253A)
                    )
                ),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            
            // Tick marks around the arc
            val tickCount = 20
            for (i in 0 until tickCount) {
                val angle = 135f + (270f / tickCount) * i
                val angleRad = Math.toRadians(angle.toDouble())
                val innerR = radius - 8f
                val outerR = radius + 4f
                val tickAlpha = if (i.toFloat() / tickCount <= animatedProgress) 0.5f else 0.1f
                drawLine(
                    color = Color.White.copy(alpha = tickAlpha),
                    start = Offset(
                        centerX + innerR * kotlin.math.cos(angleRad).toFloat(),
                        centerY + innerR * kotlin.math.sin(angleRad).toFloat()
                    ),
                    end = Offset(
                        centerX + outerR * kotlin.math.cos(angleRad).toFloat(),
                        centerY + outerR * kotlin.math.sin(angleRad).toFloat()
                    ),
                    strokeWidth = if (i % 5 == 0) 2f else 1f
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RPM",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = Color(0xFF475569)
            )
        }
    }
}
