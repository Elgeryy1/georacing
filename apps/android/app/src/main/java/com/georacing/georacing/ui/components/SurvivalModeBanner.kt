package com.georacing.georacing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.ui.theme.LocalEnergyProfile
import com.georacing.georacing.domain.model.EnergyProfile
import com.georacing.georacing.ui.glass.LiquidCard
import com.georacing.georacing.ui.glass.LocalBackdrop

@Composable
fun SurvivalModeBanner() {
    val energyProfile = LocalEnergyProfile.current

    if (energyProfile is EnergyProfile.Survival) {
        val backdrop = LocalBackdrop.current
        
        LiquidCard(
            backdrop = backdrop,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 0.dp, // Full width banner
            surfaceColor = Color(0xFFE8253A).copy(alpha = 0.65f),
            blurRadius = 12.dp
        ) {
            Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)) {
                Text(
                    text = "⚠️ MODO SUPERVIVENCIA: Funciones no críticas desactivadas para asegurar tu retorno.",
                    color = Color(0xFFF8FAFC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
