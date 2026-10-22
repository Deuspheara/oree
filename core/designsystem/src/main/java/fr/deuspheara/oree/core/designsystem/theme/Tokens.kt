package fr.deuspheara.oree.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DesignTokens {
    val background: Color
        @Composable get() = LocalLauncherPalette.current.background

    val surface: Color
        @Composable get() = LocalLauncherPalette.current.surface

    val foreground: Color
        @Composable get() = LocalLauncherPalette.current.foreground

    val secondary: Color
        @Composable get() = LocalLauncherPalette.current.secondary

    val accent: Color
        @Composable get() = LocalLauncherPalette.current.accent

    val focusedAccent: Color
        @Composable get() = LocalLauncherPalette.current.focusedAccent

    val gutter = 48.dp
    val gap = 20.dp
    val radius = 16.dp
    val cardHeight = 100.dp
}

object MotionTokens {
    const val FOCUS_SCALE = 1.10f
    const val TILT_DEGREES = 4f
    const val FOCUS_MILLIS = 150
    const val PREVIEW_DELAY_MILLIS = 160L
    const val PANEL_MILLIS = 260
    const val SECTION_MILLIS = 180
}
