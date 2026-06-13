package com.georacing.georacing.domain.model

sealed class EnergyProfile(
    val canUseAR: Boolean,
    val canUseGamification: Boolean,
    val canSyncBackground: Boolean,
    val forceOledBlack: Boolean
) {
    object Performance : EnergyProfile(
        canUseAR = true,
        canUseGamification = true,
        canSyncBackground = true,
        forceOledBlack = false
    )

    object Balanced : EnergyProfile(
        canUseAR = false,
        canUseGamification = true,
        canSyncBackground = true,
        forceOledBlack = false
    )

    object Survival : EnergyProfile(
        canUseAR = false,
        canUseGamification = false,
        canSyncBackground = false,
        forceOledBlack = true
    )
}
