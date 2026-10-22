@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Hardware-window pixels, including a deliberately offset native Dialog. No private app art. */
class HazeGlassTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dialogSamplesTheCorrectSourceRegionAndOpaquePreferenceRemovesIt() {
        var opaque by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var swap by mutableStateOf(false)
        var left = Offset.Zero
        var right = Offset.Zero
        compose.setContent {
            val source = rememberHazeState()
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(
                    LocalGlassSource provides source,
                    LocalLiquidGlass provides enabled,
                    LocalReducedTransparency provides opaque,
                ) {
                    Canvas(Modifier.fillMaxSize().hazeSource(source)) {
                        drawRect(
                            if (swap) Color.Blue else Color.Red,
                            size = Size(size.width / 2, size.height),
                        )
                        drawRect(
                            if (swap) Color.Red else Color.Blue,
                            topLeft = Offset(size.width / 2, 0f),
                        )
                    }
                    GlassDialog({}) {
                        Row(
                            Modifier.fillMaxSize().padding(70.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(120.dp).glassSurface().onGloballyPositioned {
                                    left =
                                        it.positionOnScreen() +
                                        Offset(it.size.width / 2f, it.size.height / 2f)
                                },
                            )
                            Box(
                                Modifier.size(120.dp).glassSurface().onGloballyPositioned {
                                    right =
                                        it.positionOnScreen() +
                                        Offset(it.size.width / 2f, it.size.height / 2f)
                                },
                            )
                        }
                    }
                }
            }
        }
        fun pixels(): Pair<Int, Int> {
            compose.waitForIdle()
            // Allow RenderThread shader output to reach the physical display.
            android.os.SystemClock.sleep(350)
            val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            return try {
                val outside = bitmap.getPixel(20, bitmap.height / 2)
                assertTrue(
                    "Dialog must not dim the scene outside glass",
                    maxOf(
                        android.graphics.Color.red(outside),
                        android.graphics.Color.blue(outside),
                    ) > 245,
                )
                bitmap.getPixel(left.x.toInt(), left.y.toInt()) to
                    bitmap.getPixel(right.x.toInt(), right.y.toInt())
            } finally {
                bitmap.recycle()
            }
        }
        fun red(pixel: Int) = android.graphics.Color.red(pixel)
        fun blue(pixel: Int) = android.graphics.Color.blue(pixel)
        val initial = pixels()
        assertTrue(
            "Left dialog source is red: $initial",
            red(initial.first) > blue(initial.first) + 40,
        )
        assertTrue(
            "Right dialog source is blue: $initial",
            blue(initial.second) > red(initial.second) + 40,
        )
        compose.runOnIdle { swap = true }
        val updated = pixels()
        assertTrue(
            "Changed source invalidates retained output",
            blue(updated.first) > red(updated.first) + 40,
        )
        compose.runOnIdle { enabled = false }
        val disabled = pixels()
        assertTrue("Glass off uses solid fallback", disabled.first == disabled.second)
        compose.runOnIdle { enabled = true }
        val reenabled = pixels()
        assertTrue(
            "Glass on restores source rendering",
            blue(reenabled.first) > red(reenabled.first) + 40,
        )
        compose.runOnIdle { opaque = true }
        val fallback = pixels()
        assertTrue(
            "Opaque panels stop exposing source color",
            kotlin.math.abs(red(fallback.first) - blue(fallback.first)) < 40,
        )
        assertTrue("Opaque panels use the same solid material", fallback.first == fallback.second)
    }
}
