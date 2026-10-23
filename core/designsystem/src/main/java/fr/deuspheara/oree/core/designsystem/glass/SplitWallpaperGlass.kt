@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package fr.deuspheara.oree.core.designsystem.glass

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens

/** Presentation-owned stable filtering, followed by the live optical material. */
class SplitWallpaperGlass(val original: HazeState) {
    val filtered = HazeState()
    val available = mutableStateOf(false)
}

val LocalSplitWallpaperGlass = staticCompositionLocalOf<SplitWallpaperGlass?> { null }

@Composable
internal fun SplitWallpaperGlassSources(
    wallpaper: Transition<ImageBitmap?>,
    frost: ImageBitmap?,
    reduced: Boolean,
    diffusion: () -> Float,
    enabled: Boolean,
) {
    val inputs = LocalSplitWallpaperGlass.current?.takeIf { it.original === LocalGlassSource.current } ?: return
    DisposableEffect(inputs) { onDispose { inputs.available.value = false } }
    val image = wallpaper.targetState
    val settled = !wallpaper.isRunning && wallpaper.currentState == wallpaper.targetState
    val intensity = LocalGlassIntensity.current.let { if (it.isFinite()) it.coerceIn(.25f, 1f) else .75f }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val eligible =
            enabled && image != null && frost != null && !image.hasAlpha && !frost.hasAlpha && android.os.Build.VERSION.SDK_INT >= 33 &&
                constraints.maxWidth.toLong() * constraints.maxHeight <= 2_097_152L
        SideEffect { inputs.available.value = eligible && settled }
        if (!eligible) return@BoxWithConstraints
        val filter = remember(intensity) {
            GlassStyle.clear.then {
                shape(RoundedCornerShape(0.dp))
                backgroundColor(Color.Transparent)
                tint(Color.Transparent)
                optics(
                    refractionStrength = 0f,
                    refractionDisplacement = 0.dp,
                    blurRadius = (1.25f * intensity).dp,
                    refractionDetailIntensity = 0f,
                )
                specularIntensity(0f)
                edgeShadow(Color.Transparent)
                ambientResponse(0f)
                edgeSoftness(0.dp)
                chromaticAberrationStrength(0f)
                contrast(0f)
                whitePoint(0f)
                chromaMultiplier(1f)
                contentNormalBlend(0f)
            }
        }
        // Haze records the inner source independently of the outer window clip. Each static
        // input filter sits below its animated alpha layer.
        Box(Modifier.fillMaxSize().drawWithContent { clipRect(0f, 0f, 0f, 0f) { this@drawWithContent.drawContent() } }) {
            Box(Modifier.fillMaxSize().hazeSource(inputs.filtered)) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .hazeGlass(HazeInput.Content, filter, HazePerformanceMode.Balanced),
                ) {
                    wallpaper.Crossfade(animationSpec = tween(if (reduced) 0 else 300)) { current ->
                        WallpaperGlassComponent(current)
                    }
                }
                Box(
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = diffusion()
                        compositingStrategy = CompositingStrategy.Offscreen
                    }.hazeGlass(HazeInput.Content, filter, HazePerformanceMode.Balanced),
                ) { WallpaperGlassComponent(frost) }
                Box(
                    Modifier.fillMaxSize().graphicsLayer { alpha = diffusion() }
                        .background(Color.Black.copy(alpha = .25f)),
                )
            }
        }
    }
}

@Composable
private fun WallpaperGlassComponent(image: ImageBitmap?) {
    if (image != null) {
        Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.fillMaxSize().background(DesignTokens.background))
    }
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Black.copy(alpha = .12f), Color.Transparent, Color.Black.copy(alpha = .38f))),
        ),
    )
}
