package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun remoteChangesApplyImmediatelyWithoutFooter() {
        var saved by mutableStateOf(LauncherPreferences())
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(saved, { saved = it }, {}, {}, 0, {})
            }
        }
        compose.onNodeWithTag("settings-category:APPEARANCE").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
        }
        compose.onNodeWithTag("settings-theme").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals(ThemeMode.LIGHT, saved.theme) }
        compose.onNodeWithTag("settings-apply").assertDoesNotExist()
        compose.onNodeWithTag("settings-cancel").assertDoesNotExist()
        compose.onNodeWithTag("accent:GREEN").performScrollTo().activateByRemote()
        compose.runOnIdle { assertEquals(AccentColor.GREEN, saved.accent) }
        compose.onNodeWithTag("accent:GREEN").assertIsSelected()
    }

    @Test
    fun glassIntensityAndOffUpdateImmediately() {
        var saved by mutableStateOf(LauncherPreferences())
        compose.setContent {
            HazeTestTheme(saved) { SettingsScreen(saved, { saved = it }, {}, {}, 0, {}) }
        }
        compose.onNodeWithTag("settings-glass-intensity").performScrollTo().activateByRemote()
        compose.runOnIdle { assertEquals(1f, saved.glassIntensity) }
        compose.onNodeWithTag("glass-preview").performScrollTo().assertIsDisplayed()
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
                    LauncherPreferences(),
                    {},
                    {},
                    {},
                    0,
                    {},
                    writeFailed = true,
                    onRetrySave = { retries++ },
                )
            }
        }
        compose.onNodeWithTag("settings-save-error").assertExists()
        compose.onNodeWithTag("settings-retry").activateByRemote()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun cacheClearIsExplicitAndDefaultsApplyImmediately() {
        var saved by mutableStateOf(LauncherPreferences(highContrast = true, textScale = 1.25f))
        var cleared = false
        compose.setContent {
            HazeTestTheme(saved) {
                SettingsScreen(saved, { saved = it }, {}, {}, 4096, { cleared = true })
            }
        }
        compose
            .onNodeWithTag("settings-categories")
            .performScrollToNode(hasTestTag("settings-category:STORAGE"))
        compose.onNodeWithTag("settings-category:STORAGE").activateByRemote()
        compose.onNodeWithTag("clear-image-cache").performScrollTo().activateByRemote()
        compose.runOnIdle { assertTrue(cleared) }
        compose
            .onNodeWithTag("settings-categories")
            .performScrollToNode(hasTestTag("settings-category:APPEARANCE"))
        compose.onNodeWithTag("settings-category:APPEARANCE").activateByRemote()
        compose.onNodeWithTag("settings-defaults").performScrollTo().activateByRemote()
        compose.runOnIdle { assertEquals(LauncherPreferences(), saved) }
    }

    private fun SemanticsNodeInteraction.activateByRemote() {
        performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        performKeyInput { pressKey(Key.DirectionCenter) }
    }
}
