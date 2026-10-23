package fr.deuspheara.oree

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.GlassTreatment
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.WallpaperBackground
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Synthetic project-only pixels; captures are correctness checks, never timing evidence. */
class WallpaperBackgroundTest {
    @get:Rule val compose = createComposeRule()

    private fun image(left: Color, right: Color = left): ImageBitmap = ImageBitmap(80, 45).also {
        val canvas = Canvas(it)
        canvas.drawRect(Rect(0f, 0f, 40f, 45f), Paint().apply { color = left })
        canvas.drawRect(Rect(40f, 0f, 80f, 45f), Paint().apply { color = right })
    }

    private fun center(tag: String): Color {
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height / 2]
    }

    @Test
    fun wallpaperReplacementSofteningResizeAndReattachmentStayFresh() {
        var frame by mutableStateOf(image(Color.Red))
        var frost by mutableStateOf<ImageBitmap?>(null)
        var softened by mutableStateOf(false)
        var width by mutableStateOf(240.dp)
        var attached by mutableStateOf(true)
        compose.setContent {
            LauncherTheme(LauncherPreferences()) {
                Box(Modifier.size(width, width * (9f / 16f)).testTag("wallpaper-fixture")) {
                    if (attached) WallpaperBackground(frame, true, frost, softened)
                }
            }
        }
        val red = center("wallpaper-fixture")
        assertTrue(red.red > red.blue + .2f)
        compose.runOnIdle { frame = image(Color.Blue) }
        val blue = center("wallpaper-fixture")
        assertTrue("Replaced frame is visible", blue.blue > blue.red + .2f)
        compose.runOnIdle {
            frost = image(Color.Green)
            softened = true
        }
        val green = center("wallpaper-fixture")
        assertTrue("Softening uses the new frost image", green.green > green.blue + .2f)
        compose.runOnIdle { width = 360.dp }
        val resized = center("wallpaper-fixture")
        assertTrue("Resizing keeps the current pixels", resized.green > resized.blue + .2f)
        compose.runOnIdle { attached = false }
        compose.waitForIdle()
        compose.runOnIdle {
            frame = image(Color.Red)
            frost = null
            softened = false
            attached = true
        }
        val reattached = center("wallpaper-fixture")
        assertTrue("Reattachment cannot reuse the old frost", reattached.red > reattached.green + .2f)
    }

    @Test
    fun softeningKeepsItsIntermediateFrames() {
        val frame = image(Color.Blue)
        val frost = image(Color.Green)
        var softened by mutableStateOf(false)
        compose.setContent {
            LauncherTheme(LauncherPreferences()) {
                Box(Modifier.size(240.dp, 135.dp).testTag("wallpaper-transition")) {
                    WallpaperBackground(frame, false, frost, softened)
                }
            }
        }
        val before = center("wallpaper-transition")
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { softened = true }
            compose.mainClock.advanceTimeBy(100)
            val during = center("wallpaper-transition")
            compose.mainClock.advanceTimeBy(500)
            val after = center("wallpaper-transition")
            assertTrue("Softening is visible before it settles", during.green > before.green + .05f)
            assertTrue("The intermediate frame is not the final frame", after.green > during.green + .05f)
            assertTrue("The original wallpaper fades continuously", before.blue > during.blue && during.blue > after.blue)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun movingGlassSamplesCurrentWallpaperPositionAndReplacement() {
        var frame by mutableStateOf(image(Color.Blue, Color.Red))
        var x by mutableStateOf(0.dp)
        compose.setContent {
            val source = rememberHazeState()
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(LocalGlassSource provides source, LocalLiquidGlass provides true) {
                    Box(Modifier.size(800.dp, 240.dp)) {
                        WallpaperBackground(frame, true)
                        Box(
                            Modifier.offset(x = x).size(120.dp, 100.dp).testTag("wallpaper-glass")
                                .glassSurface(radius = 0.dp, treatment = GlassTreatment.CLEAR),
                        )
                    }
                }
            }
        }
        val blue = center("wallpaper-glass")
        assertTrue("Left glass samples blue", blue.blue > blue.red + .1f)
        compose.runOnIdle { x = 600.dp }
        val red = center("wallpaper-glass")
        assertTrue("Moving glass samples its new position", red.red > red.blue + .1f)
        compose.runOnIdle { frame = image(Color.Red, Color.Blue) }
        val replaced = center("wallpaper-glass")
        assertTrue("Source replacement invalidates moving glass", replaced.blue > replaced.red + .1f)
    }
}
