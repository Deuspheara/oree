package fr.deuspheara.oree

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color as PixelColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiveGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.platform.image.captureMenuBackdrop
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Foreground artwork must contribute to the circle; wallpaper alone must not substitute for it. */
class ProfileGlassTest {
    @get:Rule val compose = createComposeRule()

    @Test fun addCircleSamplesForegroundSceneWithoutPanelAndStaysTransparentWhenFocused() {
        lateinit var activity: Activity
        lateinit var scope: CoroutineScope
        var backdrop by mutableStateOf<ImageBitmap?>(null)
        var live by mutableStateOf(false)
        var opaque by mutableStateOf(false)
        val profile = LocalProfile("fixture", "Sample")
        compose.setContent {
            activity = checkNotNull(androidx.activity.compose.LocalActivity.current)
            scope = rememberCoroutineScope()
            val wallpaperSource = rememberHazeState()
            LauncherTheme(LauncherPreferences()) {
                CompositionLocalProvider(
                    LocalGlassSource provides wallpaperSource,
                    LocalLiveGlass provides live,
                    LocalReducedTransparency provides opaque,
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().background(Color.Blue).hazeSource(wallpaperSource))
                        Box(Modifier.align(Alignment.TopEnd).size(420.dp, 360.dp).background(Color.Red))
                    }
                    ProfilePanel(
                        ProfileSnapshot(profile.id, listOf(profile)), true, opaque, false, false,
                        {}, { _, _, _ -> }, {}, {}, backdrop = backdrop,
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { scope.launch { backdrop = captureMenuBackdrop(activity)?.image?.asImageBitmap() } }
        compose.waitUntil(3000) { backdrop != null }
        fun capture(name: String): Pair<Int, Int> {
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            val bounds: Rect = compose.onNodeWithTag("add-profile").fetchSemanticsNode().boundsInRoot
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val image = instrumentation.uiAutomation.takeScreenshot()
            try {
                File(instrumentation.targetContext.cacheDir, "profile-circle-$name.png").outputStream().use {
                    image.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                return image.getPixel((bounds.left + bounds.width * .25f).toInt(), bounds.center.y.toInt()) to
                    image.getPixel((bounds.right + 14).toInt(), bounds.center.y.toInt())
            } finally {
                image.recycle()
            }
        }
        fun assertScene(name: String) {
            val (circle, outside) = capture(name)
            assertTrue("Circle samples foreground artwork: $name", PixelColor.red(circle) > PixelColor.blue(circle) + 80)
            assertTrue(
                "Outside the circle stays the original scene, without a panel: $name",
                PixelColor.red(outside) > 245 && PixelColor.blue(outside) < 10,
            )
        }
        assertScene("balanced")
        compose.onNodeWithTag("add-profile").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("add-profile").assertIsFocused()
        assertScene("balanced-focused")
        compose.runOnIdle { live = true }
        assertScene("live-focused")
        compose.runOnIdle { opaque = true }
        val (solid, _) = capture("opaque")
        assertTrue("Reduced transparency overrides scene sampling", kotlin.math.abs(PixelColor.red(solid) - PixelColor.blue(solid)) < 40)
    }
}
