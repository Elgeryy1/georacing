package com.georacing.georacing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georacing.georacing.ui.theme.*
import com.georacing.georacing.ui.glass.LiquidTopBar
import com.georacing.georacing.ui.glass.LocalBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RacingTopBar(
    onMenuClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = LocalBackdrop.current
    
    LiquidTopBar(
        backdrop = backdrop,
        modifier = modifier,
        surfaceColor = Color(0xFF12121A).copy(alpha = 0.85f),
        navigationIcon = {
             IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(id = com.georacing.georacing.R.string.cd_menu),
                    tint = RacingRed,
                    modifier = Modifier.size(26.dp)
                )
             }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Accent dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RacingRed)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "GEORACING",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    color = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onProfileClick) {
                 Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    RacingRed.copy(alpha = 0.3f),
                                    Color(0xFF14141C).copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(1.dp, RacingRed.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(id = com.georacing.georacing.R.string.cd_profile),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    )
}
