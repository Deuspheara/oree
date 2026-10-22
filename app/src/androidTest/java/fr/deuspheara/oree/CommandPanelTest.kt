package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.width
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.ControlSize
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.platform.status.AudioOutput
import fr.deuspheara.oree.core.platform.status.ControlStatus
import fr.deuspheara.oree.core.platform.status.NetworkKind
import fr.deuspheara.oree.feature.settings.presentation.component.CommandPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CommandPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun resizeHandleCommitsAndBackCancelsWithoutOpeningAction() {
        var sizes by mutableStateOf(emptyMap<ControlId, ControlSize>())
        var opens = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(
                    true,
                    false,
                    { opens++ },
                    {},
                    onWallpaper = {},
                    onNetwork = {},
                    onAudio = {},
                    sizes = sizes,
                    onResize = { id, size -> sizes = sizes + (id to size) },
                )
            }
        }
        compose.onNodeWithTag("command-settings").performTouchInput { longClick() }
        val handle = compose.onNodeWithTag("resize:SETTINGS").assertIsFocused()
        val pixels = handle.captureToImage().toPixelMap()
        var darkest = 1f
        var brightest = 0f
        for (y in pixels.height / 4 until pixels.height * 3 / 4) {
            for (x in pixels.width / 4 until pixels.width * 3 / 4) {
                val luminance = pixels[x, y].luminance()
                darkest = minOf(darkest, luminance)
                brightest = maxOf(brightest, luminance)
            }
        }
        assertTrue(
            "Focused resize grip must contrast with its actual rendered background",
            (brightest + .05f) / (darkest + .05f) > 3f,
        )
        handle.performKeyInput {
            pressKey(Key.DirectionCenter)
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle {
            assertEquals(ControlSize(2, 2), sizes[ControlId.SETTINGS])
            assertEquals(0, opens)
        }
        compose.onNodeWithTag("command-settings").assertIsFocused().performTouchInput {
            longClick()
        }
        compose.onNodeWithTag("resize:SETTINGS").performKeyInput {
            pressKey(Key.DirectionCenter)
            pressKey(Key.DirectionLeft)
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("command-settings").assertIsFocused()
        compose.runOnIdle {
            assertEquals(ControlSize(2, 2), sizes[ControlId.SETTINGS])
            assertEquals(0, opens)
        }
        compose.onNodeWithTag("command-close").assertDoesNotExist()
        compose.onNodeWithTag("command-android-settings").assertDoesNotExist()
    }

    @Test
    fun physicalHeldOkOpensHandleAndReleaseCannotStartResizingOrOpenSettings() {
        var opens = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(true, false, { opens++ }, {}, onNetwork = {})
            }
        }
        compose.onNodeWithTag("command-settings").assertIsFocused()
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val now = android.os.SystemClock.uptimeMillis()
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                now,
                now,
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                0,
            ),
        )
        try {
            android.os.SystemClock.sleep(600)
            compose.mainClock.advanceTimeBy(600)
            instrumentation.sendKeySync(
                android.view.KeyEvent(
                    now,
                    android.os.SystemClock.uptimeMillis(),
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    1,
                ),
            )
            compose.waitForIdle()
            compose.onNodeWithTag("resize:SETTINGS").assertIsFocused()
            instrumentation.sendKeySync(
                android.view.KeyEvent(
                    now,
                    android.os.SystemClock.uptimeMillis(),
                    android.view.KeyEvent.ACTION_DOWN,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    2,
                ),
            )
        } finally {
            instrumentation.sendKeySync(
                android.view.KeyEvent(
                    now,
                    android.os.SystemClock.uptimeMillis(),
                    android.view.KeyEvent.ACTION_UP,
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    0,
                ),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("resize:SETTINGS").assertIsFocused()
        compose.runOnIdle { assertEquals(0, opens) }
    }

    @Test
    fun resizingHasIntermediateBoundsAndRetargetsWithoutJumping() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(false, false, {}, {}, onWallpaper = {}, onNetwork = {}, onAudio = {})
            }
        }
        val tile = compose.onNodeWithTag("command-settings")
        tile.performTouchInput { longClick() }
        val handle = compose.onNodeWithTag("resize:SETTINGS").assertIsFocused()
        handle.performKeyInput { pressKey(Key.DirectionCenter) }
        val start = tile.fetchSemanticsNode().boundsInRoot.width
        try {
            compose.mainClock.autoAdvance = false
            handle.performKeyInput { pressKey(Key.DirectionRight) }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(80)
            val middle = tile.fetchSemanticsNode().boundsInRoot.width
            compose.mainClock.advanceTimeBy(250)
            val end = tile.fetchSemanticsNode().boundsInRoot.width
            assertTrue(
                "Resize must render intermediate widths",
                middle > start + 1f && middle < end - 1f,
            )
            handle.performKeyInput { pressKey(Key.DirectionLeft) }
            compose.mainClock.advanceTimeBy(64)
            val returning = tile.fetchSemanticsNode().boundsInRoot.width
            handle.performKeyInput { pressKey(Key.DirectionRight) }
            val retargeted = tile.fetchSemanticsNode().boundsInRoot.width
            // pressKey advances input time between down/up; a small in-flight movement is valid.
            assertTrue(
                "Retarget must remain between endpoints rather than snap",
                retargeted > start + 1f && retargeted < end - 1f,
            )
            assertTrue(
                "Direction reversal must preserve current geometry",
                kotlin.math.abs(returning - retargeted) < (end - start) * .25f,
            )
            compose.mainClock.advanceTimeBy(300)
            assertEquals(end, tile.fetchSemanticsNode().boundsInRoot.width, 2f)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun movingUsesFourByFourBoundsCommitsAndBackCancels() {
        var positions by mutableStateOf(emptyMap<ControlId, ControlPosition>())
        var commits = 0
        var homeHidden = false
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(
                    true,
                    false,
                    {},
                    {},
                    onWallpaper = {},
                    onNetwork = {},
                    onAudio = {},
                    positions = positions,
                    onEditingChanged = { homeHidden = it },
                    onLayout = { layout ->
                        commits++
                        positions = layout.associate { it.id to ControlPosition(it.column, it.row) }
                    },
                )
            }
        }
        fun moveMode() {
            compose.onNodeWithTag("command-settings").performTouchInput { longClick() }
            compose
                .onNodeWithTag("control-move")
                .performSemanticsAction(
                    androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
                ) {
                    it()
                }
                .performKeyInput { pressKey(Key.DirectionCenter) }
            compose.onNodeWithTag("resize:SETTINGS").assertIsFocused()
            compose.runOnIdle { assertTrue(homeHidden) }
        }
        moveMode()
        compose.onNodeWithTag("resize:SETTINGS").performKeyInput {
            repeat(6) {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle {
            assertEquals(1, commits)
            assertFalse(homeHidden)
            assertEquals(ControlPosition(3, 3), positions[ControlId.SETTINGS])
        }
        compose.onNodeWithTag("command-settings").assertIsFocused()
        moveMode()
        compose.onNodeWithTag("resize:SETTINGS").performKeyInput { pressKey(Key.DirectionLeft) }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, commits)
            assertFalse(homeHidden)
        }
        compose.onNodeWithTag("command-settings").assertIsFocused()
    }

    @Test
    fun compactAudioKeepsStatusAccessibleWithoutClippedDetail() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(
                    true,
                    false,
                    {},
                    {},
                    onAudio = {},
                    sizes = mapOf(ControlId.AUDIO to ControlSize(1, 1)),
                    status = ControlStatus(audio = setOf(AudioOutput.HDMI)),
                )
            }
        }
        val status =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(
                    R.string.audio_available,
                    "HDMI",
                )
        compose
            .onNodeWithTag("command-audio")
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                    status,
                ),
            )
        compose.onNodeWithText(status).assertDoesNotExist()
        val label =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(R.string.audio_settings)
        compose.onNodeWithTag("command-audio").assertContentDescriptionEquals(label)
        compose.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun onlySquareControlsHideTheirVisibleLabel() {
        var size by mutableStateOf(ControlSize(1, 1))
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(true, false, {}, {}, sizes = mapOf(ControlId.SETTINGS to size))
            }
        }
        val label =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(R.string.launcher_settings)
        val square = compose.onNodeWithTag("command-settings").fetchSemanticsNode().boundsInRoot
        assertEquals("A 1x1 control must actually be square", square.width, square.height, 1f)
        compose.onNodeWithText(label).assertDoesNotExist()
        for (
        expanded in
        listOf(
            ControlSize(1, 2),
            ControlSize(2, 1),
            ControlSize(2, 2),
        )
        ) {
            compose.runOnIdle { size = expanded }
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun statusChangesKeepFocusAndNetworkActionDispatchesOnce() {
        var status by mutableStateOf(ControlStatus(network = NetworkKind.OFFLINE))
        var opens = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(
                    true,
                    false,
                    {},
                    {},
                    onNetwork = { opens++ },
                    onAudio = {},
                    status = status,
                )
            }
        }
        compose.onNodeWithTag("command-network").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
        compose.onNodeWithTag("command-network").assertIsFocused()
        compose.runOnIdle {
            status =
                status.copy(
                    network = NetworkKind.WIFI,
                    validated = true,
                    audio = setOf(AudioOutput.HDMI),
                )
        }
        compose.onNodeWithTag("command-network").assertIsFocused().assertTextContains("Wi-Fi")
        compose.onNodeWithTag("command-audio").assertExists()
        compose.onNodeWithTag("command-network").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test
    fun focusStartsInPanelAndDpadCannotEscapeToUnderlyingControl() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                Button(
                    onClick = {},
                    modifier = androidx.compose.ui.Modifier.testTag("underlying"),
                ) {
                    Text("Underlying")
                }
                CommandPanel(true, true, {}, {})
            }
        }
        compose.onNodeWithTag("command-settings").assertIsFocused().performKeyInput {
            repeat(4) { pressKey(Key.DirectionLeft) }
        }
        compose.onNodeWithTag("underlying").assertIsNotFocused()
        compose.onNodeWithTag("command-settings").assertIsFocused()
    }

    @Test
    fun settingsActionDispatchesOnceAndDisablesClosingPanel() {
        var selected = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(true, false, { selected++ }, {})
            }
        }
        compose.onNodeWithTag("command-settings").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, selected) }
        compose.onNodeWithTag("command-settings").assertIsNotEnabled()
    }

    @Test
    fun backDismissesOnlyTheCommandLayer() {
        var open by mutableStateOf(true)
        var closes = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                Button(
                    onClick = {},
                    modifier = androidx.compose.ui.Modifier.testTag("underlying"),
                ) {
                    Text("Underlying")
                }
                if (open) {
                    CommandPanel(
                        true,
                        false,
                        {},
                        {
                            closes++
                            open = false
                        },
                    )
                }
            }
        }
        compose.onNodeWithTag("command-settings").assertIsFocused()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("command-panel").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, closes) }
        compose.onNodeWithTag("underlying").assertExists()
    }
}
