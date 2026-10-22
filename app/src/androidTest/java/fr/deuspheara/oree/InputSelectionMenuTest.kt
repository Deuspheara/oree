package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputConnection
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.feature.settings.presentation.component.InputSelectionMenu
import fr.deuspheara.oree.feature.settings.presentation.screen.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class InputSelectionMenuTest {
    @get:Rule val compose = createComposeRule()
    private val entries =
        listOf(
            TvInput("one", "HDMI 1", TvInputConnection.CONNECTED),
            TvInput("two", "HDMI 2", TvInputConnection.STANDBY),
        )

    private fun activate(tag: String) {
        compose
            .onNodeWithTag(tag)
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            .performKeyInput { pressKey(Key.DirectionCenter) }
    }

    @Test
    fun chooserOpensFromSettingsAndBackRestoresItsTrigger() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SettingsScreen(
                    value = LauncherPreferences(),
                    cacheBytes = 0,
                    inputs = TvInputs(entries),
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        activate("settings-category:HOME")
        compose.onNodeWithTag("settings-inputs").performScrollTo()
        activate("settings-inputs")
        compose.onNodeWithTag("input-menu").assertWidthIsEqualTo(360.dp)
        compose.onNodeWithTag("input:one").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("input-menu").assertDoesNotExist()
        compose.onNodeWithTag("settings-inputs").assertIsFocused()
    }

    @Test
    fun removalTransfersFocusAndActivationDispatchesOnce() {
        var inputs by mutableStateOf(TvInputs(entries))
        var opened = emptyList<String>()
        var closed = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                InputSelectionMenu(inputs, true, { opened = opened + it.id }, { closed++ })
            }
        }
        compose.onNodeWithTag("input:one").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("input:two").assertIsFocused()
        compose.runOnIdle { inputs = TvInputs(entries.take(1)) }
        compose.onNodeWithTag("input:one").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle {
            assertEquals(listOf("one"), opened)
            assertEquals(1, closed)
        }
    }

    @Test
    fun unavailableAndEmptyKeepCloseReachable() {
        var inputs by mutableStateOf(TvInputs(unavailable = true))
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) { InputSelectionMenu(inputs, true, {}, {}) }
        }
        compose.onNodeWithTag("input-menu-close").assertIsFocused()
        compose
            .onNodeWithText(
                InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getString(R.string.input_unavailable),
            )
            .assertIsDisplayed()
        compose.runOnIdle { inputs = TvInputs() }
        compose
            .onNodeWithText(
                InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getString(R.string.inputs_empty),
            )
            .assertIsDisplayed()
        compose.onNodeWithTag("input-menu-close").assertIsFocused()
    }
}
