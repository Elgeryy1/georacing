package com.georacing.georacing.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    userPreferences: UserPreferencesDataStore
) {
    val backdrop = com.georacing.georacing.ui.glass.LocalBackdrop.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                OnboardingViewModel(userPreferences)
            }
        }
    )

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Handle permission results if needed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF080810), Color(0xFF0A0A16), Color(0xFF080810))))
            .padding(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPage(page = page)
        }

        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color(0xFFE8253A) else Color(0xFF64748B)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pagerState.currentPage == 2) {
            com.georacing.georacing.ui.glass.LiquidButton(
                onClick = { permissionLauncher.launch(permissionsToRequest) },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            ) {
                Text("Solicitar Permisos")
            }
            Spacer(modifier = Modifier.height(8.dp))
            com.georacing.georacing.ui.glass.LiquidButton(
                onClick = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            ) {
                Text("Empezar")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                com.georacing.georacing.ui.glass.LiquidButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    backdrop = backdrop,
                    surfaceColor = Color.Transparent, // Transparent for outlined feel
                    tint = Color(0xFFF8FAFC).copy(alpha = 0.1f)
                ) {
                    Text("Saltar")
                }
                com.georacing.georacing.ui.glass.LiquidButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    backdrop = backdrop,
                    surfaceColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                ) {
                    Text("Siguiente")
                }
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (page) {
                0 -> "BIENVENIDO A GEORACING"
                1 -> "NAVEGACIÓN INTELIGENTE"
                else -> "SEGURIDAD Y AVISOS"
            },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Color(0xFFF8FAFC)
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (page) {
                0 -> "Tu compañero digital para el Circuit de Barcelona-Catalunya."
                1 -> "Encuentra tu asiento, puntos de interés y servicios usando nuestra tecnología de balizas."
                else -> "Recibe avisos en tiempo real y reporta incidencias para mejorar la experiencia de todos."
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF64748B)
            ),
            textAlign = TextAlign.Center
        )
    }
}
