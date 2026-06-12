package com.georacing.georacing.ui.glass

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop

/**
 * Empty backdrop stub for default CompositionLocal value.
 * Actual backdrop should be provided via rememberLayerBackdrop() in MainActivity.
 */
private object StubBackdrop : Backdrop {
    override val isCoordinatesDependent: Boolean = false
    
    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        // No-op: stub implementation for default
    }
}

/**
 * CompositionLocal for providing the Backdrop across the app.
 * The Backdrop is used for Liquid Glass effects in navigation bars, toggles, etc.
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { StubBackdrop }
