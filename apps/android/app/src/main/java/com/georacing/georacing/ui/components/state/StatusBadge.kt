package com.georacing.georacing.ui.components.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class StatusLevel { OK, WARN, CRIT }

@Composable
fun StatusBadge(level: StatusLevel, label: String, modifier: Modifier = Modifier) {
    val color = when (level) {
        StatusLevel.OK -> Color(0xFF22C55E)
        StatusLevel.WARN -> Color(0xFFF97316)
        StatusLevel.CRIT -> Color(0xFFE8253A)
    }
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = color, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}
