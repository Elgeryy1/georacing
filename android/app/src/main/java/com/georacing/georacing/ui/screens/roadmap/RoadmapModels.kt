package com.georacing.georacing.ui.screens.roadmap

import androidx.compose.ui.graphics.vector.ImageVector

enum class FeatureStatus {
    DONE, WIP, BACKLOG
}

data class Feature(
    val title: String,
    val description: String,
    val status: FeatureStatus,
    val icon: ImageVector
)

data class FeatureCategory(
    val title: String,
    val features: List<Feature>
)
