package com.georacing.georacing.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer / Skeleton loading components para GeoRacing.
 * Usados mientras se cargan datos de red o base de datos.
 */

private val ShimmerColorBase = Color(0xFF1E1E2A)
private val ShimmerColorHighlight = Color(0xFF2A2A3A)

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "translate"
    )
    return Brush.linearGradient(
        colors = listOf(ShimmerColorBase, ShimmerColorHighlight, ShimmerColorBase),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = 16.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

/** Skeleton de una tarjeta típica del dashboard */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF14141C))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(width = 120.dp, height = 12.dp)
        ShimmerBox(height = 20.dp)
        ShimmerBox(width = 200.dp, height = 14.dp)
    }
}

/** Skeleton para el grid de features del home */
@Composable
fun SkeletonGrid(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        ShimmerBox(width = 64.dp, height = 64.dp, shape = RoundedCornerShape(18.dp))
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(width = 48.dp, height = 10.dp)
                    }
                }
            }
        }
    }
}

/** Skeleton de una fila de lista (POI, transport, etc.) */
@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF14141C))
            .padding(14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ShimmerBox(width = 40.dp, height = 40.dp, shape = RoundedCornerShape(10.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ShimmerBox(width = 140.dp, height = 14.dp)
            ShimmerBox(width = 200.dp, height = 10.dp)
        }
    }
}

/** Skeleton de la pantalla home completa */
@Composable
fun SkeletonHomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Greeting skeleton
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerBox(width = 100.dp, height = 12.dp)
                ShimmerBox(width = 180.dp, height = 18.dp)
            }
            ShimmerBox(width = 70.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
        }

        // Status card skeleton
        SkeletonCard()

        // Grid skeleton
        SkeletonGrid()

        // News skeleton
        ShimmerBox(height = 120.dp, shape = RoundedCornerShape(24.dp))
    }
}
