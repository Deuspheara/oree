@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.GlassTreatment
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassIntensity
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalSplitWallpaperGlass
import fr.deuspheara.oree.core.designsystem.glass.SplitWallpaperGlass
import fr.deuspheara.oree.core.designsystem.glass.WallpaperBackground
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Deterministic synthetic hardware-window frames for comparing experimental render paths. */
class GlassStageFidelityTest {
    @get:Rule val compose = createComposeRule()

    private val opaqueFixture = InstrumentationRegistry.getArguments().getString("opaqueWallpaper") == "true"

    private fun pattern(swap: Boolean): ImageBitmap = ImageBitmap(800, 450, hasAlpha = !opaqueFixture).also { image ->
        val canvas = Canvas(image)
        for (y in 0 until 30) {
            for (x in 0 until 40) {
                val red = ((x * 7 + y * 11) % 31) / 30f
                val blue = ((x * 13 + y * 3) % 29) / 28f
                val green = if ((x + y) % 2 == 0) .8f else .15f
                canvas.drawRect(
                    Rect(x * 20f, y * 15f, (x + 1) * 20f, (y + 1) * 15f),
                    Paint().apply {
                        val alpha = if (!opaqueFixture && (x + y) % 4 == 0) .35f else 1f
                        color = if (swap) Color(blue, green, red, alpha) else Color(red, green, blue, alpha)
                    },
                )
            }
        }
    }

    @Test
    fun recordMovingOpticsSourceTransitionResizeAndReattachment() {
        var image by mutableStateOf(pattern(false))
        val frost = pattern(true)
        var lowered by mutableStateOf(false)
        var softened by mutableStateOf(false)
        var wide by mutableStateOf(true)
        var attached by mutableStateOf(true)
        var tilted by mutableStateOf(false)
        var clearBounds = Rect.Zero
        val composited = InstrumentationRegistry.getArguments().getString("glassNativeLayer") == "true"
        var prepared: SplitWallpaperGlass? = null
        compose.setContent {
            val source = rememberHazeState()
            val split = remember(source) { SplitWallpaperGlass(source) }
            prepared = split
            val prefiltered = InstrumentationRegistry.getArguments().getString("splitWallpaper") == "true"
            val progressive = remember {
                HazeProgressive.Brush(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            }
            val y by animateDpAsState(if (lowered) 360.dp else 40.dp, tween(360), label = "fixture-position")
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(
                    LocalGlassSource provides source,
                    LocalSplitWallpaperGlass provides if (prefiltered) split else null,
                    LocalLiquidGlass provides true,
                    LocalGlassIntensity provides 1f,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        WallpaperBackground(image, false, frost, softened)
                        if (attached) {
                            Box(
                                Modifier.offset(40.dp, y).size(if (wide) 800.dp else 420.dp, 120.dp)
                                    .onGloballyPositioned { clearBounds = it.boundsInWindow() }
                                    .graphicsLayer {
                                        rotationX = if (tilted) 3f else 0f
                                        rotationY = if (tilted) -4f else 0f
                                        scaleX = if (tilted) .97f else 1f
                                        scaleY = if (tilted) .97f else 1f
                                    }
                                    .graphicsLayer {
                                        compositingStrategy = if (composited && size.width * size.height <= 4_194_304f) {
                                            CompositingStrategy.Offscreen
                                        } else {
                                            CompositingStrategy.Auto
                                        }
                                    }
                                    .glassSurface(radius = 30.dp, treatment = GlassTreatment.CLEAR),
                            )
                            Box(
                                Modifier.offset(100.dp, 210.dp).size(240.dp, 130.dp)
                                    .glassSurface(radius = 26.dp, progressive = progressive),
                            )
                            Box(
                                Modifier.offset(450.dp, 210.dp).size(240.dp, 130.dp)
                                    .glassSurface(radius = 26.dp, treatment = GlassTreatment.SOFT),
                            )
                        }
                    }
                }
            }
        }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(instrumentation.targetContext.cacheDir, "glass-stage-fidelity").apply { mkdirs() }
        fun capture(name: String): Float {
            compose.waitForIdle()
            SystemClock.sleep(500) // Native rendering settle at a frozen test clock; never a latency sample.
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                var sum = 0f
                for (row in 0 until 8) {
                    for (column in 0 until 16) {
                        val x = (clearBounds.left + clearBounds.width * (.2f + column * .6f / 15f)).toInt()
                        val y = (clearBounds.top + clearBounds.height * (.3f + row * .4f / 7f)).toInt()
                        val pixel = bitmap.getPixel(x.coerceIn(0, bitmap.width - 1), y.coerceIn(0, bitmap.height - 1))
                        sum += android.graphics.Color.red(pixel) + android.graphics.Color.green(pixel) + android.graphics.Color.blue(pixel)
                    }
                }
                return sum / (8f * 16f * 3f * 255f)
            } finally {
                bitmap.recycle()
            }
        }
        compose.waitForIdle()
        if (InstrumentationRegistry.getArguments().getString("splitWallpaper") == "true") {
            assertTrue(
                "Opaque fixture must exercise the prepared input; alpha sources must fall back",
                prepared?.available?.value == opaqueFixture,
            )
        }
        compose.mainClock.autoAdvance = false
        try {
            capture("00-initial")
            compose.runOnIdle {
                lowered = true
                softened = true
            }
            for (i in 1..6) {
                compose.mainClock.advanceTimeBy(48)
                capture("01-down-$i")
            }
            compose.runOnIdle { lowered = false }
            for (i in 1..3) {
                compose.mainClock.advanceTimeBy(32)
                capture("02-reversal-$i")
            }
            compose.runOnIdle {
                lowered = true
                tilted = true
            }
            compose.mainClock.advanceTimeBy(64)
            capture("03-press-tilt")
            compose.mainClock.advanceTimeBy(500)
            capture("04-settled")
            compose.runOnIdle {
                image = pattern(true)
                softened = false
                tilted = false
            }
            compose.mainClock.advanceTimeBy(144)
            if (InstrumentationRegistry.getArguments().getString("splitWallpaper") == "true" && opaqueFixture) {
                compose.runOnIdle {
                    assertTrue("Changing wallpaper keeps live glass until its crossfade settles", prepared?.available?.value == false)
                }
            }
            capture("05-source-midpoint")
            compose.mainClock.advanceTimeBy(500)
            capture("06-source-settled")
            compose.runOnIdle { wide = false }
            compose.mainClock.advanceTimeByFrame()
            capture("07-resized")
            compose.runOnIdle { attached = false }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle {
                attached = true
                softened = false
                lowered = false
            }
            compose.mainClock.advanceTimeBy(500)
            val stationarySharp = capture("08-reattached")
            compose.runOnIdle { softened = true }
            compose.mainClock.advanceTimeBy(144)
            capture("09-stationary-frost-midpoint")
            compose.mainClock.advanceTimeBy(500)
            val stationaryFrost = capture("10-stationary-frost-settled")
            assertTrue("The stationary live layer must update for frost/veil changes", stationaryFrost < stationarySharp * .9f)
            assertTrue("The stationary glass must keep its source rather than become empty", stationaryFrost > stationarySharp * .5f)
            compose.runOnIdle {
                wide = true
                softened = false
            }
            compose.mainClock.advanceTimeBy(500)
            val wideSharp = capture("11-wide-stationary-sharp")
            compose.runOnIdle { softened = true }
            compose.mainClock.advanceTimeBy(144)
            capture("12-wide-stationary-frost-midpoint")
            compose.mainClock.advanceTimeBy(500)
            val wideFrost = capture("13-wide-stationary-frost-settled")
            assertTrue("Source-aligned filtering must update while the wide glass stays still", wideFrost < wideSharp * .9f)
            assertTrue("The wide stationary glass must retain its live source", wideFrost > wideSharp * .5f)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
