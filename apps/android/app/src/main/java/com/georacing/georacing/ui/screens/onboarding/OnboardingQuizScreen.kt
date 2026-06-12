package com.georacing.georacing.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.domain.model.Interest
import com.georacing.georacing.domain.model.F1Team
import com.georacing.georacing.domain.model.TransportMethod
import com.georacing.georacing.domain.model.UserType

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingQuizScreen(
    viewModel: WelcomeViewModel,
    onOnboardingComplete: () -> Unit
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isConfiguring by viewModel.isConfiguring.collectAsState()
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) {
            onOnboardingComplete()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF080810)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))))
                .padding(padding)
        ) {
            if (isConfiguring) {
                MagicLoadingScreen()
            } else {
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Progress
                    QuizProgressBar(currentStep = currentStep, totalSteps = 5)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            } else {
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> width } + fadeOut()
                            }.using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "quiz_transition"
                    ) { step ->
                        when(step) {
                            0 -> StepUserType(viewModel)
                            1 -> StepTransport(viewModel)
                            2 -> StepInterests(viewModel)
                            3 -> StepFavoriteTeam(viewModel)
                            4 -> StepAccessibility(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizProgressBar(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0 until totalSteps) {
            val color = if (i <= currentStep) Color(0xFFE8253A) else Color(0xFF14141C)
            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
fun MagicLoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp,
            color = Color(0xFFE8253A),
            trackColor = Color(0xFF14141C)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "CONFIGURANDO TU GEORACING...",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            )
        )
        Text(
            text = "Personalizando experiencia según tus respuestas",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun StepUserType(viewModel: WelcomeViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "¿QUÉ TE TRAE AL CIRCUITO?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val options = listOf(
            Triple(UserType.FAN, "Aficionado", Icons.Default.SportsMotorsports),
            Triple(UserType.FAMILY, "Familia", Icons.Default.FamilyRestroom),
            Triple(UserType.VIP, "Experiencia VIP", Icons.Default.EmojiEvents),
            Triple(UserType.STAFF, "Staff / Equipo", Icons.Default.Badge)
        )

        options.forEach { (type, label, icon) ->
            SelectionCard(
                text = label,
                icon = icon,
                onClick = { viewModel.selectUserType(type) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun StepTransport(viewModel: WelcomeViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "¿CÓMO HAS VENIDO HOY?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        val options = listOf(
            Triple(TransportMethod.CAR, "Coche Propio", Icons.Default.DirectionsCar),
            Triple(TransportMethod.PUBLIC_TRANSPORT, "Tren / Bus", Icons.Default.Train),
            Triple(TransportMethod.WALKING, "A pie / Taxi", Icons.Default.DirectionsWalk)
        )

        options.forEach { (method, label, icon) ->
            SelectionCard(
                text = label,
                icon = icon,
                onClick = { viewModel.selectTransport(method) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.previousStep() }) {
            Text("Volver")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepInterests(viewModel: WelcomeViewModel) {
    val selectedInterests by viewModel.selectedInterests.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "¿QUÉ TE INTERESA MÁS?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Selecciona todo lo que aplique",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
             val options = listOf(
                Triple(Interest.RACING, "Tiempos & Carreras", Icons.Default.Flag),
                Triple(Interest.FOOD, "Comida & Bebida", Icons.Default.Restaurant),
                Triple(Interest.EVENTS, "Eventos Fan", Icons.Default.Event),
                Triple(Interest.TECH, "Tech & Sostenibilidad", Icons.Default.Smartphone)
            )
            
            options.forEach { (interest, label, icon) ->
                InterestChip(
                    text = label,
                    icon = icon,
                    selected = selectedInterests.contains(interest),
                    onClick = { viewModel.toggleInterest(interest) }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.nextStep() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = selectedInterests.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8253A),
                contentColor = Color(0xFFF8FAFC),
                disabledContainerColor = Color(0xFF14141C),
                disabledContentColor = Color(0xFF64748B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "SIGUIENTE",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
         Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.previousStep() }) {
            Text("Volver")
        }
    }
}

@Composable
fun SelectionCard(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE8253A),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF8FAFC)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun InterestChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
             Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
fun StepFavoriteTeam(viewModel: WelcomeViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "¿TU EQUIPO FAVORITO?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Personaliza tu experiencia con tu equipo",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        val teams = F1Team.entries.toList()
        
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(teams.size) { index ->
                val team = teams[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { viewModel.selectFavoriteTeam(team) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141C))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(team.color), shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = team.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF8FAFC)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.previousStep() }) {
            Text("Volver")
        }
    }
}

@Composable
fun StepAccessibility(viewModel: WelcomeViewModel) {
    val needsAccessibility by viewModel.needsAccessibility.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "ACCESIBILIDAD",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "¿Necesitas rutas adaptadas?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Toggle accesibilidad
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (needsAccessibility) Color(0xFF1A2A1A) else Color(0xFF14141C)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setAccessibility(!needsAccessibility) }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Accessible,
                    contentDescription = null,
                    tint = if (needsAccessibility) Color(0xFF22C55E) else Color(0xFF64748B),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Evitar escaleras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF8FAFC)
                    )
                    Text(
                        text = "Rutas accesibles sin escaleras ni desniveles",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
                Switch(
                    checked = needsAccessibility,
                    onCheckedChange = { viewModel.setAccessibility(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF22C55E),
                        checkedThumbColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.completeQuiz() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8253A),
                contentColor = Color(0xFFF8FAFC)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "PERSONALIZAR MI EXPERIENCIA",
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { viewModel.previousStep() }) {
            Text("Volver")
        }
    }
}
