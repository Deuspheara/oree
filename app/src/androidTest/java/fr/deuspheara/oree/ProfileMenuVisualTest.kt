package fr.deuspheara.oree

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.RecentContent
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import fr.deuspheara.oree.feature.profiles.presentation.screen.WallpaperPanel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real window evidence on a shared, non-personal source; never edits the user's profiles. */
@OptIn(ExperimentalTestApi::class)
class ProfileMenuVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun menuAndEditorMaterialMatrixKeepsTheSurroundingSceneUnchanged() {
        var preferences by mutableStateOf(LauncherPreferences())
        var surface by mutableIntStateOf(0)
        val profile = LocalProfile("fixture", "Sample", "avatar_03")
        compose.setContent {
            key(preferences, surface) {
                HazeTestTheme(preferences) {
                    CompositionLocalProvider(LocalHighContrast provides preferences.highContrast) {
                        when (surface) {
                            0 ->
                                AppContextMenu(
                                    "Sample app",
                                    Rect(100f, 100f, 200f, 200f),
                                    false,
                                    false,
                                    false,
                                    {},
                                    {},
                                    {},
                                    reduceMotion = preferences.reduceMotion,
                                )

                            1 ->
                                WallpaperPanel(
                                    profile,
                                    RecentContent(ContentAvailability.DISABLED),
                                    preferences.reduceMotion,
                                    false,
                                    false,
                                    {},
                                    {},
                                    {},
                                    {},
                                    {},
                                )

                            else ->
                                ProfilePanel(
                                    ProfileSnapshot(profile.id, listOf(profile)),
                                    preferences.reduceMotion,
                                    preferences.reduceTransparency,
                                    false,
                                    false,
                                    {},
                                    { _, _, _ -> },
                                    {},
                                    {},
                                )
                        }
                    }
                }
            }
        }
        fun capture(name: String): Int {
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            try {
                File(instrumentation.targetContext.cacheDir, "profile-material-$name.png")
                    .outputStream()
                    .use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                return bitmap.getPixel(24, bitmap.height - 24)
            } finally {
                bitmap.recycle()
            }
        }
        val variants =
            listOf(
                "dark" to LauncherPreferences(),
                "light" to LauncherPreferences(theme = ThemeMode.LIGHT),
                "glass-off" to LauncherPreferences(liquidGlass = false),
                "opaque" to LauncherPreferences(reduceTransparency = true),
                "contrast" to LauncherPreferences(highContrast = true),
                "reduced" to LauncherPreferences(reduceMotion = true),
            )
        for ((name, value) in variants) {
            compose.runOnIdle {
                preferences = value
                surface = 0
            }
            compose.onNodeWithTag("menu-open").assertIsFocused()
            val scene = capture("$name-app")
            compose.runOnIdle { surface = 1 }
            compose.onNodeWithTag("wallpaper:LANDSCAPE").assertIsFocused()
            assertEquals("Wallpaper must not dim the scene", scene, capture("$name-wallpaper"))
            compose.runOnIdle { surface = 2 }
            compose.onNodeWithTag("profile:fixture").performSemanticsAction(
                SemanticsActions.OnLongClick,
            ) {
                it()
            }
            compose.onNodeWithTag("profile-avatar:avatar_03").assertIsFocused().assertIsSelected()
            assertEquals("Editor must not dim the scene", scene, capture("$name-editor"))
            compose.onNodeWithTag("profile-avatar:avatar_03").performKeyInput {
                pressKey(Key.DirectionRight)
            }
            compose
                .onNodeWithTag("profile-avatar:avatar_04")
                .assertIsFocused()
                .assertIsNotSelected()
            compose.onNodeWithTag("profile-avatar:avatar_03").assertIsSelected()
            capture("$name-unselected-focus")
            if (name == "dark") {
                compose.onNodeWithTag("profile-avatar:avatar_04").performKeyInput {
                    pressKey(Key.DirectionCenter)
                }
                compose.onNodeWithTag("profile-avatar:avatar_04").assertIsSelected()
                capture("dark-selected-focus")
                compose.onNodeWithTag("profile-name").performSemanticsAction(
                    SemanticsActions.RequestFocus,
                ) {
                    it()
                }
                capture("dark-name-focused")
                compose.onNodeWithTag("profile-name").performTextClearance()
                compose.onNodeWithTag("save-profile").assertIsNotEnabled()
                capture("dark-name-empty")
            }
        }
    }
}
