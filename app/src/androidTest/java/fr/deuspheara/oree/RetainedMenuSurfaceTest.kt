@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.top
import androidx.compose.ui.test.topLeft
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.GlassSurfaceRole
import fr.deuspheara.oree.core.designsystem.glass.GlassTreatment
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RetainedMenuSurfaceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun exactGlassCacheHasSourceColorAndInvalidatesForSourceAndOpaqueFallback() {
        var revision by mutableIntStateOf(0)
        var color by mutableStateOf(Color.Blue)
        var opaque by mutableStateOf(false)
        compose.setContent {
            val source = rememberHazeState()
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(
                    LocalGlassSource provides source,
                    LocalOpaquePanels provides opaque,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().hazeSource(source)) { drawRect(color) }
                        GlassDialog(onDismissRequest = {}) {
                            Box(Modifier.fillMaxSize().padding(start = 80.dp, top = 60.dp)) {
                                RetainedMenuSurface(revision, true, Modifier.size(200.dp, 140.dp))
                            }
                        }
                    }
                }
            }
        }
        fun waitForImage() {
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag("retained-menu-image").fetchSemanticsNodes().isNotEmpty()
            }
        }
        fun center(): Color {
            val image = compose.onNodeWithTag("retained-menu-image").captureToImage().toPixelMap()
            return image[image.width / 2, image.height / 2]
        }
        waitForImage()
        val blue = center()
        assertTrue("Retained glass contains the actual blue source", blue.blue > blue.red + .1f)
        compose.runOnIdle {
            color = Color.Red
            revision++
        }
        compose.waitForIdle()
        waitForImage()
        val red = center()
        assertTrue("Source replacement invalidates retained output", red.red > red.blue + .1f)
        compose.runOnIdle { opaque = true }
        compose.onNodeWithTag("retained-menu-image").assertDoesNotExist()
    }

    @Test
    fun clearMaterialInvalidatesWhenItsPositionChanges() {
        var x by mutableStateOf(0.dp)
        compose.setContent {
            val source = rememberHazeState()
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(LocalGlassSource provides source) {
                    Box(Modifier.fillMaxSize()) {
                        Canvas(Modifier.fillMaxSize().hazeSource(source)) {
                            drawRect(Color.Blue)
                            drawRect(
                                Color.Red,
                                topLeft = androidx.compose.ui.geometry.Offset(size.width / 2, 0f),
                                size =
                                    androidx.compose.ui.geometry.Size(size.width / 2, size.height),
                            )
                        }
                        RetainedMenuSurface(
                            "fixed",
                            true,
                            Modifier.offset(x = x).size(200.dp, 140.dp),
                            role = GlassSurfaceRole.CONTROL,
                            radius = 30.dp,
                            treatment = GlassTreatment.CLEAR,
                        )
                    }
                }
            }
        }
        fun sample(): Color {
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag("retained-menu-image").fetchSemanticsNodes().isNotEmpty()
            }
            val p = compose.onNodeWithTag("retained-menu-image").captureToImage().toPixelMap()
            return p[p.width / 2, p.height / 2]
        }
        val blue = sample()
        assertTrue(blue.blue > blue.red + .1f)
        compose.runOnIdle { x = 2000.dp }
        compose.waitForIdle()
        compose.onNodeWithTag("retained-menu-image").assertDoesNotExist()
        compose.runOnIdle { x = 600.dp }
        compose.waitForIdle()
        val red = sample()
        assertTrue("Moved dock must sample its new source position", red.red > red.blue + .1f)
    }
}
