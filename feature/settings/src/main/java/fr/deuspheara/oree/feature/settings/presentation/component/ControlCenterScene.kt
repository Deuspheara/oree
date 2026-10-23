package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency

/** Tiles sample the rendered underlay, so their optics start from the same softened scene. */
@Composable
internal fun ControlCenterScene(image: ImageBitmap?, content: @Composable (HazeState) -> Unit) {
    val underlay = rememberHazeState()
    val enabled = image != null && LocalLiquidGlass.current && !LocalReducedTransparency.current && !LocalOpaquePanels.current
    CompositionLocalProvider(LocalGlassSource provides if (enabled) underlay else LocalGlassSource.current) {
        Box(Modifier.fillMaxSize()) { content(underlay) }
    }
}
