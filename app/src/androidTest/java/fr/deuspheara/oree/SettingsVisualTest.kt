package fr.deuspheara.oree

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.settings.presentation.screen.SettingsScreen
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsAction
import org.junit.Rule
import org.junit.Test

/** Actual physical-window captures of project-only fixtures; no personal catalog is included. */
class SettingsVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun appearanceMatrixProducesRealWindowCaptures() {
        var value by mutableStateOf(LauncherPreferences(reduceMotion = true))
        var english by mutableStateOf(false)
        compose.setContent {
            val base = androidx.compose.ui.platform.LocalContext.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val localizedConfiguration =
                remember(configuration, english) {
                    android.content.res.Configuration(configuration).apply {
                        setLocale(if (english) java.util.Locale.US else java.util.Locale.FRANCE)
                    }
                }
            val localized =
                remember(base, localizedConfiguration) {
                    base.createConfigurationContext(localizedConfiguration)
                }
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localized,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
            ) {
                HazeTestTheme(value) {
                    CompositionLocalProvider(LocalOpaquePanels provides true) {
                        SettingsScreen(
                            value = value,
                            cacheBytes = 0,
                            reduceMotion = true,
                            onAction = { action ->
                                when (action) {
                                    is SettingsAction.ChangePreferences -> {
                                        val it = action.value
                                        value = it
                                    }

                                    else -> Unit
                                }
                            },
                        )
                    }
                }
            }
        }
        fun capture(name: String) {
            compose.waitForIdle()
            android.os.SystemClock.sleep(400)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val screenshot =
                InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            try {
                java.io.File(context.cacheDir, "settings-visual-$name.png").outputStream().use {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                screenshot.recycle()
            }
        }
        capture("dark-top")
        compose.onNodeWithTag("glass-preview").performScrollTo()
        capture("dark-preview")
        compose.runOnIdle { value = value.copy(theme = ThemeMode.LIGHT) }
        capture("light-preview")
        compose.onNodeWithTag("settings-content").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.ScrollBy,
        ) {
            it(0f, -10000f)
        }
        capture("light-top")
        compose.onNodeWithTag("accent:GREEN").performScrollTo().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
        compose.onNodeWithTag("accent:GREEN").assertIsDisplayed()
        capture("light-accents")
        compose.runOnIdle {
            value = value.copy(textScale = 2f, highContrast = true, reduceTransparency = true)
        }
        compose.onNodeWithTag("glass-preview").performScrollTo().assertIsDisplayed()
        capture("light-large-opaque")
        compose.onNodeWithTag("settings-defaults").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            english = true
            value = LauncherPreferences(theme = ThemeMode.LIGHT, reduceMotion = true)
        }
        compose.onNodeWithTag("settings-content").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.ScrollBy,
        ) {
            it(0f, -10000f)
        }
        compose.onNodeWithText("Settings").assertExists()
        capture("english-light")
    }
}
