package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.settings.presentation.screen.SettingsScreen
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun choicesBrowseWithoutSavingAndSelectCommits() {
        var saved by mutableStateOf(LauncherPreferences())
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(
                    value = saved,
                    cacheBytes = 0,
                    onAction = { action ->
                        when (action) {
                            is SettingsAction.ChangePreferences -> {
                                val it = action.value
                                saved = it
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("settings-category:APPEARANCE").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("settings-categories").assertDoesNotExist()
        compose.onNodeWithTag("settings-theme").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("settings-theme").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("settings-theme-choice:0").assertIsSelected().assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.runOnIdle { assertEquals(ThemeMode.DARK, saved.theme) }
        compose.onNodeWithTag("settings-theme-choice:1").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(ThemeMode.LIGHT, saved.theme) }
        compose.onNodeWithTag("settings-theme").assertIsFocused()
        compose.onNodeWithTag("settings-apply").assertDoesNotExist()
        compose.onNodeWithTag("settings-cancel").assertDoesNotExist()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("accent:GREEN"))
        compose.onNodeWithTag("accent:GREEN").performScrollTo().activateByRemote()
        compose.runOnIdle { assertEquals(AccentColor.GREEN, saved.accent) }
        compose.onNodeWithTag("accent:GREEN").assertIsSelected()
    }

    @Test
    fun glassIntensityAndOffUpdateImmediately() {
        var saved by mutableStateOf(LauncherPreferences())
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(
                    value = saved,
                    cacheBytes = 0,
                    onAction = { action ->
                        when (action) {
                            is SettingsAction.ChangePreferences -> {
                                val it = action.value
                                saved = it
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("settings-category:APPEARANCE").activateByRemote()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("settings-glass-intensity"))
        compose.onNodeWithTag("settings-glass-intensity").performScrollTo().activateByRemote()
        compose.onNodeWithTag("settings-glass-intensity-rail").assertIsFocused().performKeyInput {
            repeat(5) { pressKey(Key.DirectionRight) }
        }
        compose.runOnIdle { assertEquals(.75f, saved.glassIntensity) }
        compose.onNodeWithTag("settings-glass-intensity-rail").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1f, saved.glassIntensity) }
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("glass-preview"))
        compose.onNodeWithTag("glass-preview").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("settings-liquid-glass"))
        compose.onNodeWithTag("settings-liquid-glass").performScrollTo().activateByRemote()
        compose.runOnIdle { assertFalse(saved.liquidGlass) }
        compose.onNodeWithTag("settings-glass-intensity").assertIsNotEnabled()
    }

    @Test
    fun saveFailureHasReachableRetry() {
        var retries = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SettingsScreen(
                    value = LauncherPreferences(),
                    cacheBytes = 0,
                    writeFailed = true,
                    onAction = { action ->
                        when (action) {
                            SettingsAction.RetrySave -> {
                                retries++
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("settings-save-error").assertExists()
        compose.onNodeWithTag("settings-retry").activateByRemote()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun cacheClearIsExplicitAndDefaultsRequireConfirmation() {
        var saved by mutableStateOf(LauncherPreferences(highContrast = true, textScale = 1.25f))
        var cleared = false
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(
                    value = saved,
                    cacheBytes = 4096,
                    onAction = { action ->
                        when (action) {
                            SettingsAction.ClearCache -> {
                                cleared = true
                            }

                            is SettingsAction.ChangePreferences -> {
                                val it = action.value
                                saved = it
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose
            .onNodeWithTag("settings-categories")
            .performScrollToNode(hasTestTag("settings-category:STORAGE"))
        compose.onNodeWithTag("settings-category:STORAGE").activateByRemote()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("clear-image-cache"))
        compose.onNodeWithTag("clear-image-cache").performScrollTo().activateByRemote()
        compose.runOnIdle { assertTrue(cleared) }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose
            .onNodeWithTag("settings-categories")
            .performScrollToNode(hasTestTag("settings-category:APPEARANCE"))
        compose.onNodeWithTag("settings-category:APPEARANCE").activateByRemote()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("settings-defaults"))
        compose.onNodeWithTag("settings-defaults").performScrollTo().activateByRemote()
        compose.runOnIdle { assertTrue(saved.highContrast) }
        compose.onNodeWithTag("settings-defaults-cancel").assertIsFocused()
        compose.onNodeWithTag("settings-defaults-confirm").activateByRemote()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(LauncherPreferences(), saved) }
    }

    @Test
    fun selectedCategoryAndEntryFocusSurviveSavedStateRestoration() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SettingsScreen(LauncherPreferences(), cacheBytes = 0, reduceMotion = true, onAction = {})
            }
        }
        compose.onNodeWithTag("settings-categories")
            .performScrollToNode(hasTestTag("settings-category:ACCESSIBILITY"))
        compose.onNodeWithTag("settings-category:ACCESSIBILITY").activateByRemote()
        compose.onNodeWithTag("settings-contrast").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("settings-contrast").assertIsDisplayed()
        compose.onNodeWithTag("settings-categories").assertDoesNotExist()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("settings-category:ACCESSIBILITY").assertIsFocused()
    }

    @Test
    fun backCancelsChoiceThenReturnsToCategoryBeforeClosing() {
        var saved by mutableStateOf(LauncherPreferences())
        var closed = false
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(saved, cacheBytes = 0, reduceMotion = true, onAction = {
                    when (it) {
                        is SettingsAction.ChangePreferences -> saved = it.value
                        SettingsAction.Close -> closed = true
                        else -> Unit
                    }
                })
            }
        }
        compose.onNodeWithTag("settings-category:APPEARANCE").activateByRemote()
        compose.onNodeWithTag("settings-theme").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("settings-theme").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("settings-theme-choice:0").performKeyInput { pressKey(Key.DirectionDown) }
        fun back() {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
        }
        back()
        compose.runOnIdle {
            assertEquals(ThemeMode.DARK, saved.theme)
            assertFalse(closed)
        }
        compose.onNodeWithTag("settings-theme").assertIsFocused()
        back()
        compose.onNodeWithTag("settings-category:APPEARANCE").assertIsFocused()
        compose.runOnIdle { assertFalse(closed) }
        back()
        compose.runOnIdle { assertTrue(closed) }
    }

    private fun SemanticsNodeInteraction.activateByRemote() {
        performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        performKeyInput { pressKey(Key.DirectionCenter) }
    }
}
