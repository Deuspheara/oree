package fr.deuspheara.oree.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ThemeMode

fun accentColor(accent: AccentColor, dark: Boolean = false): Color = Color(
    when (accent) {
        AccentColor.BLUE -> if (dark) 0xFF409CFF else 0xFF0068DC
        AccentColor.PURPLE -> if (dark) 0xFFBF8CFF else 0xFF8245C5
        AccentColor.PINK -> if (dark) 0xFFFF709C else 0xFFC52D68
        AccentColor.RED -> if (dark) 0xFFFF6961 else 0xFFD32D28
        AccentColor.ORANGE -> if (dark) 0xFFFFB050 else 0xFFAA5700
        AccentColor.GREEN -> if (dark) 0xFF63D684 else 0xFF237A3D
    },
)

data class LauncherPalette(
    val background: Color,
    val surface: Color,
    val foreground: Color,
    val secondary: Color,
    val accent: Color,
    val focusedAccent: Color = accent,
)

val DarkPalette =
    LauncherPalette(
        Color(0xFF111217),
        Color(0xFF292B33),
        Color(0xFFF4F5F7),
        Color(0xFFB4B7C1),
        Color(0xFFF0F2F7),
    )
val LightPalette =
    LauncherPalette(
        Color(0xFFF5F5F7),
        Color(0xFFFFFFFF),
        Color(0xFF1D1D1F),
        Color(0xFF57575E),
        Color(0xFF163D5A),
    )
val LocalLauncherPalette = staticCompositionLocalOf { DarkPalette }

@Composable
fun LauncherTheme(preferences: LauncherPreferences, content: @Composable () -> Unit) {
    val dark =
        when (preferences.theme) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val palette =
        remember(dark, preferences.highContrast, preferences.accent) {
            val base =
                (if (dark) DarkPalette else LightPalette).copy(
                    accent = accentColor(preferences.accent, dark),
                    focusedAccent = accentColor(preferences.accent, !dark),
                )
            if (preferences.highContrast) {
                base.copy(
                    background = if (dark) Color.Black else Color.White,
                    foreground = if (dark) Color.White else Color.Black,
                    secondary = if (dark) Color(0xFFE4EAF0) else Color(0xFF172431),
                )
            } else {
                base
            }
        }
    val density = LocalDensity.current
    val scaled =
        remember(density, preferences.textScale) {
            Density(density.density, density.fontScale * preferences.normalized().textScale)
        }
    val colors =
        if (dark) {
            darkColorScheme(
                background = palette.background,
                surface = palette.surface,
                onBackground = palette.foreground,
                onSurface = palette.foreground,
                primary = palette.accent,
                onPrimary = palette.background,
            )
        } else {
            lightColorScheme(
                background = palette.background,
                surface = palette.surface,
                onBackground = palette.foreground,
                onSurface = palette.foreground,
                primary = palette.accent,
                onPrimary = palette.background,
            )
        }
    CompositionLocalProvider(LocalLauncherPalette provides palette, LocalDensity provides scaled) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
