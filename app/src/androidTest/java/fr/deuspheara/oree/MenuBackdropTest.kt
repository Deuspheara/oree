package fr.deuspheara.oree

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.platform.image.WallpaperFrame
import fr.deuspheara.oree.core.platform.image.captureMenuBackdrop
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MenuBackdropTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun menuSamplesSnapshotWithoutPaintingOrDimmingTheActivity() {
        val bitmap =
            android.graphics.Bitmap.createBitmap(384, 216, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(AndroidColor.RED)
        val frame = WallpaperFrame(bitmap, bitmap, AndroidColor.RED)
        compose.setContent {
            LauncherTheme(LauncherPreferences()) {
                Box(Modifier.fillMaxSize().background(Color.Blue))
                AppContextMenu(
                    "Test app",
                    androidx.compose.ui.geometry.Rect(100f, 100f, 200f, 200f),
                    false,
                    false,
                    false,
                    {},
                    {},
                    {},
                    backdrop = frame,
                )
            }
        }
        compose.waitForIdle()
        android.os.SystemClock.sleep(500)
        val screenshot =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .takeScreenshot()
        try {
            val outside = screenshot.getPixel(screenshot.width - 30, screenshot.height / 2)
            assertTrue(
                "Real scene stays blue and undimmed",
                AndroidColor.blue(outside) > 245 && AndroidColor.red(outside) < 10,
            )
            // The distinct red snapshot must still feed the glass, despite being hidden outside it.
            var redPixels = 0
            for (y in 50 until screenshot.height step 4) {
                for (x in 50 until screenshot.width / 2 step 4) {
                    val pixel = screenshot.getPixel(x, y)
                    // The existing menu tint is 60% black: a red source settles near 102,
                    // while the opaque fallback is neutral and the real Activity stays blue.
                    if (
                        AndroidColor.red(pixel) > 65 &&
                        AndroidColor.red(pixel) > 3 * AndroidColor.blue(pixel) &&
                        AndroidColor.blue(pixel) < 40
                    ) {
                        redPixels++
                    }
                }
            }
            assertTrue("Source-only snapshot must render inside the menu", redPixels > 500)
        } finally {
            screenshot.recycle()
        }
    }

    @Test
    fun snapshotIncludesForegroundCardAndPreservesItsColorAfterBlur() {
        lateinit var activity: Activity
        lateinit var scope: CoroutineScope
        val captured = AtomicReference<WallpaperFrame?>()
        compose.setContent {
            activity = checkNotNull(androidx.activity.compose.LocalActivity.current)
            scope = rememberCoroutineScope()
            Box(
                Modifier.fillMaxSize().background(Color.Blue),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(160.dp).background(Color.Red))
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { scope.launch { captured.set(captureMenuBackdrop(activity)) } }
        compose.waitUntil(3000) { captured.get() != null }
        val frame = captured.get()!!
        assertEquals(384, frame.frost.width)
        assertEquals(216, frame.frost.height)
        val center = frame.frost.getPixel(192, 108)
        val corner = frame.frost.getPixel(20, 20)
        assertTrue(
            "Foreground card must contribute to the blurred scene",
            AndroidColor.red(center) > 200 && AndroidColor.blue(center) < 30,
        )
        assertTrue(
            "Background must remain independently represented",
            AndroidColor.blue(corner) > 200 && AndroidColor.red(corner) < 30,
        )
    }
}
