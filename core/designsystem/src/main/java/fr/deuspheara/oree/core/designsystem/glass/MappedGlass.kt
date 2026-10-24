@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.core.designsystem.glass

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import dev.chrisbanes.haze.HazeEffectFactory
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeSampling
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Presentation override for comparison/fallback; no persisted preference or global renderer. */
val LocalPrecomputedGlass = compositionLocalOf { true }

internal data class MappedGlassStyle(val geometry: GlassOpticalGeometry, val bitmap: Bitmap, val intensity: Float)

/** Keep the geometry map through wallpaper changes; source capture remains Haze-owned and live. */
@Composable
internal fun Modifier.mappedGlassOrHaze(
    input: HazeInput,
    fallbackStyle: GlassStyle,
    performanceMode: HazePerformanceMode,
    radius: Dp,
    intensity: Float,
    prepare: Boolean,
    usePreparedInput: Boolean,
): Modifier {
    if (Build.VERSION.SDK_INT < 33) return hazeGlass(input, fallbackStyle, performanceMode)
    val enabled = LocalPrecomputedGlass.current && prepare
    var measured by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val geometry = remember(measured, radius, density, enabled) {
        if (enabled) {
            GlassOpticalGeometry.create(
                measured.width,
                measured.height,
                with(density) {
                    radius.toPx()
                },
                density.density,
            )
        } else {
            null
        }
    }
    var bitmap by remember(geometry) { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    // Cancellation and geometry-keyed state prevent a late bake replacing a newer shape.
    LaunchedEffect(geometry, failed) {
        if (geometry == null || failed) return@LaunchedEffect
        try {
            bitmap = withContext(Dispatchers.Default) {
                val context = currentCoroutineContext()
                val pixels = bakeGlassOptics(geometry) { context.ensureActive() }
                context.ensureActive()
                Bitmap.createBitmap(pixels.argb, geometry.mapWidth, geometry.mapHeight, Bitmap.Config.ARGB_8888)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: RuntimeException) {
            failed = true
        }
    }
    val factory = remember {
        HazeEffectFactory<MappedGlassStyle> { MappedGlassRenderer { failed = true } }
    }
    val ready = bitmap
    val material = if (geometry != null && ready != null && !failed && usePreparedInput) {
        Modifier.hazeEffect(
            factory = factory,
            input = input,
            style = MappedGlassStyle(geometry, ready, intensity),
            sampling = HazeSampling.FullResolution,
        )
    } else {
        Modifier.hazeGlass(input, fallbackStyle, performanceMode)
    }
    return onSizeChanged { measured = it }.then(material)
}
