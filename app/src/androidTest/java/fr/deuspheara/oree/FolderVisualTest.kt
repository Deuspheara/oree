package fr.deuspheara.oree

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.FolderAddAppsPanel
import fr.deuspheara.oree.feature.home.presentation.component.FolderPanel
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test

/** Project-only fixtures; screenshots remain in private device cache for visual review. */
class FolderVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun folderSurfacesRemainReachableAcrossAppearanceVariants() {
        val apps = listOf("Alpha", "Bravo", "Charlie").map {
            TvApp("org.example.${it.lowercase()}/.TV", "org.example.${it.lowercase()}", it, 0)
        }
        val folder = AppFolder("visual", "Cinema", apps.take(2).map { it.id })
        var pane by mutableIntStateOf(0)
        var english by mutableStateOf(true)
        var preferences by mutableStateOf(LauncherPreferences(reduceMotion = true))
        compose.setContent {
            val context = LocalContext.current
            val base = LocalConfiguration.current
            val configuration = remember(base, english) {
                Configuration(base).apply { setLocale(if (english) Locale.US else Locale.FRANCE) }
            }
            val localized = remember(context, configuration) { context.createConfigurationContext(configuration) }
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalResources provides localized.resources,
            ) {
                HazeTestTheme(preferences) {
                    when (pane) {
                        0 -> HomeScreen(HomeUiState(apps, false), resume = 0, folders = listOf(folder), reduceMotion = true, onAction = {})

                        1 -> FolderPanel(
                            folder, apps, 0, true, false, false, null,
                            {}, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, {}, onAddApps = {},
                        )

                        else -> FolderAddAppsPanel(folder, listOf(folder), apps, false, false, true, {}, {})
                    }
                }
            }
        }
        fun capture(name: String) {
            compose.waitForIdle()
            android.os.SystemClock.sleep(300)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            try {
                File(instrumentation.targetContext.cacheDir, "folder-visual-$name.png").outputStream().use {
                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                screenshot.recycle()
            }
        }
        capture("card-en-dark")
        compose.runOnIdle { pane = 1 }
        compose.onNodeWithTag("folder-add").assertIsDisplayed()
        compose.onNodeWithText("Add apps").assertIsDisplayed()
        capture("panel-en-dark")
        compose.runOnIdle {
            english = false
            preferences = preferences.copy(theme = ThemeMode.LIGHT)
        }
        compose.onNodeWithText("Ajouter des apps").assertIsDisplayed()
        capture("panel-fr-light")
        compose.runOnIdle { preferences = preferences.copy(textScale = 2f, highContrast = true, reduceTransparency = true) }
        compose.onNodeWithTag("folder-add").assertIsDisplayed()
        compose.onNodeWithTag("app:${apps[0].id}").assertIsDisplayed()
        capture("panel-fr-large-opaque")
        compose.runOnIdle { pane = 2 }
        compose.onNodeWithTag("folder-add-close").assertIsDisplayed()
        compose.onNodeWithTag("folder-add:${apps[2].id}").assertIsDisplayed()
        capture("picker-fr-large-opaque")
    }
}
