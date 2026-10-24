@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassIntensity
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier

/** Exercise the real shared glass path in remote regression fixtures, including native Dialogs. */
@Composable
fun HazeTestTheme(preferences: LauncherPreferences, content: @Composable () -> Unit) {
    val source = rememberHazeState()
    val inputMode = LocalInputModeManager.current
    // Compose key injection requires an explicit keyboard-mode precondition.
    LaunchedEffect(inputMode) { inputMode.requestInputMode(InputMode.Keyboard) }
    LauncherTheme(preferences) {
        CompositionLocalProvider(
            LocalGlassSource provides source,
            LocalLiquidGlass provides preferences.liquidGlass,
            LocalGlassIntensity provides preferences.glassIntensity,
            fr.deuspheara.oree.core.designsystem.glass.LocalLiveGlass provides preferences.liveGlass,
            LocalReducedTransparency provides
                (
                    preferences.reduceTransparency ||
                        preferences.highContrast ||
                        preferences.quality == QualityTier.ECONOMY
                    ),
        ) {
            Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize().hazeSource(source)) {
                    drawRect(Brush.horizontalGradient(listOf(Color(0xFF315270), Color(0xFF6C7865))))
                }
                content()
            }
        }
    }
}
