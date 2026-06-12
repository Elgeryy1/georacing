package com.georacing.georacing.ui.theme

import androidx.compose.ui.unit.dp

// 🏎️ Spacing Scale (4px base grid)
object Spacing {
    val none = 0.dp
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    val huge = 64.dp
    val hero = 96.dp    // Hero sections, splash
}

// Layout Constants — Racing Dashboard
object Layout {
    val screenPadding = 20.dp     // Slightly wider for premium feel
    val cardPadding = Spacing.lg
    val sectionSpacing = Spacing.xl
    val iconSmall = 16.dp
    val iconMedium = 24.dp
    val iconLarge = 32.dp
    val iconXLarge = 48.dp
    val buttonHeight = 56.dp
    val buttonHeightCompact = 44.dp
    val pillHeight = 34.dp
    val topBarHeight = 56.dp
    val bottomBarHeight = 72.dp
    val cardElevation = 8.dp
}

// Corner Radius — Consistent rounding
object Radius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 28.dp    // Hero cards
    val full = 9999.dp // Circle/Pill
}
