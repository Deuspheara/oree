@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.core.designsystem.glass

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens

val LocalGlassSource = staticCompositionLocalOf<HazeState?> { null }

val LocalOpaquePanels = staticCompositionLocalOf { false }

val LocalLiquidGlass = staticCompositionLocalOf { true }
val LocalGlassIntensity = staticCompositionLocalOf { .75f }

val LocalReducedTransparency = staticCompositionLocalOf { false }

@Composable
fun WallpaperBackground(image: ImageBitmap?, reduced: Boolean) {
    Box(
        Modifier.fillMaxSize()
            .then(
                LocalGlassSource.current
                    ?.takeIf { LocalLiquidGlass.current && !LocalReducedTransparency.current }
                    ?.let { Modifier.hazeSource(it) } ?: Modifier,
            ),
    ) {
        Crossfade(image, animationSpec = tween(if (reduced) 0 else 300), label = "wallpaper") { current ->
            if (current != null) {
                Image(current, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(DesignTokens.background))
            }
        }
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = .12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = .38f),
                        ),
                    ),
                ),
        )
    }
}

/** Dock keeps shallow diffusion; controls soften backdrop detail for legibility. */
enum class GlassTreatment {
    CLEAR,
    SOFT,
}

enum class GlassSurfaceRole {
    CONTROL,
    MENU,
}

@Composable
private fun usesOpaqueGlass(opaque: Boolean = LocalReducedTransparency.current): Boolean = !LocalLiquidGlass.current ||
    LocalOpaquePanels.current ||
    opaque ||
    LocalReducedTransparency.current ||
    android.os.Build.VERSION.SDK_INT < 33 ||
    LocalGlassSource.current == null

/** Clear Home controls sit over wallpaper even when the settings palette is light. */
@Composable
fun glassControlForeground(): Color = if (usesOpaqueGlass()) DesignTokens.foreground else Color.White

/**
 * Source-backed optical material. Native backdrop is deliberately not enabled. Only the stable
 * wallpaper (or one-shot menu scene) is captured, never focus/clock animation. Haze Auto
 * positioning promotes to screen coordinates for effects in Dialog windows.
 */
@Composable
fun Modifier.glassSurface(
    opaque: Boolean = LocalReducedTransparency.current,
    radius: Dp = 26.dp,
    treatment: GlassTreatment = GlassTreatment.SOFT,
    role: GlassSurfaceRole = GlassSurfaceRole.CONTROL,
    performanceMode: HazePerformanceMode = HazePerformanceMode.Balanced,
): Modifier {
    val source = LocalGlassSource.current
    val shape = RoundedCornerShape(radius)
    val surface = DesignTokens.surface
    val intensity =
        LocalGlassIntensity.current.let { if (it.isFinite()) it.coerceIn(.25f, 1f) else .75f }
    val fallback = usesOpaqueGlass(opaque)
    val lightMenu = DesignTokens.background.luminance() > .5f && role == GlassSurfaceRole.MENU
    val style =
        remember(shape, intensity, treatment, lightMenu) {
            GlassStyle.clear.then {
                shape(shape)
                backgroundColor(
                    if (lightMenu) Color.White.copy(alpha = .32f) else Color.Transparent,
                )
                tint(if (lightMenu) Color.White.copy(alpha = .22f) else Color.Transparent)
                whitePoint(0f)
                contrast(0f)
                optics(
                    refractionStrength = .85f * intensity,
                    refractionHeightFraction = .35f,
                    refractionDisplacement = 56.dp,
                    blurRadius =
                        ((if (treatment == GlassTreatment.CLEAR) 1.25f else 8f) * intensity).dp,
                    refractionProfile = RefractionProfile.Edge(28.dp),
                    refractionDetailIntensity = 0f,
                )
                specularIntensity(.55f * intensity)
                ambientResponse(.22f * intensity)
            }
        }
    return if (fallback || source == null) {
        this.clip(shape).background(surface)
    } else {
        this.clip(shape)
            .hazeGlass(
                input = HazeInput.Sources(source),
                style = style,
                performanceMode = performanceMode,
            )
            .then(if (lightMenu) Modifier.background(Color.White.copy(alpha = .62f)) else Modifier)
    }
}

/** Local menu tint leaves the surrounding scene and light palette unchanged. */
@Composable
fun Modifier.menuSurface(
    opaque: Boolean = LocalReducedTransparency.current,
    performanceMode: HazePerformanceMode = HazePerformanceMode.Balanced,
): Modifier = glassSurface(opaque = opaque, role = GlassSurfaceRole.MENU, performanceMode = performanceMode)
    .then(
        if (DesignTokens.background.luminance() < .5f) {
            Modifier.background(Color.Black.copy(alpha = .60f))
        } else {
            Modifier
        },
    )
