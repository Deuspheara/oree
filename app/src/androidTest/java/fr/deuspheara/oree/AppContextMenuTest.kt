package fr.deuspheara.oree

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toLong
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppContextMenuTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dpadSelectsFavoriteAndErrorKeepsTheMenuUsable() {
        var desired: Boolean? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                AppContextMenu(
                    "Alpha",
                    Rect(20f, 20f, 350f, 230f),
                    false,
                    false,
                    true,
                    {},
                    {
                        desired = it
                    },
                    {},
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("menu-open").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("menu-favorite-add").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals(true, desired) }
        compose.onNodeWithTag("app-menu").assertExists()
        compose.onNodeWithTag("menu-favorite-add").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("menu-close").assertIsFocused()
    }

    @Test
    fun openingLongPressReleaseCannotClickTheNewMenuAndBackRestoresOrigin() {
        var opened = 0
        compose.setContent {
            var target by remember { mutableStateOf<Pair<TvApp, Rect>?>(null) }
            var resume by remember { mutableIntStateOf(0) }
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(
                        listOf(TvApp("Alpha", "Alpha", "Alpha", 0)),
                        false,
                    ),
                    resume,
                    true,
                    { opened++ },
                    {},
                    {},
                    {},
                    onAppMenu = { app, bounds, _ -> target = app to bounds },
                )
                target?.let { (app, bounds) ->
                    AppContextMenu(
                        app.label,
                        bounds,
                        false,
                        false,
                        false,
                        { opened++ },
                        {},
                        {
                            target = null
                            resume++
                        },
                        awaitingOpeningRelease = true,
                    )
                }
            }
        }
        compose.waitForIdle()
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val down = android.os.SystemClock.uptimeMillis()
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                down,
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                0,
            ),
        )
        android.os.SystemClock.sleep(600)
        // Advance the controlled Compose timeout as well as the real native hold.
        compose.mainClock.advanceTimeBy(600)
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                1,
            ),
        )
        compose.waitForIdle()
        compose.onNodeWithTag("menu-open").assertIsFocused()
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                2,
            ),
        )
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                0,
            ),
        )
        compose.runOnIdle { assertEquals(0, opened) }
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
    }

    @Test
    fun backDuringEntryRetargetsAndClosesOnlyOnce() {
        var closed = 0
        var shown by mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent {
                HazeTestTheme(LauncherPreferences()) {
                    if (shown) {
                        AppContextMenu(
                            "Alpha",
                            Rect(20f, 20f, 350f, 230f),
                            false,
                            false,
                            false,
                            {},
                            {},
                            {
                                closed++
                                shown = false
                            },
                            reduceMotion = false,
                        )
                    }
                }
            }
            compose.mainClock.advanceTimeBy(48)
            compose.waitForIdle()
            val instrumentation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.mainClock.advanceTimeBy(MotionTokens.PANEL_MILLIS.toLong() + 96)
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(1, closed) }
            compose.onNodeWithTag("app-menu").assertDoesNotExist()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun rtlLargeTextKeepsAllActionsReachableByDpad() {
        compose.setContent {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    androidx.compose.ui.unit.LayoutDirection.Rtl,
            ) {
                HazeTestTheme(LauncherPreferences(textScale = 2f, highContrast = true)) {
                    AppContextMenu(
                        "A long application title",
                        Rect(20f, 20f, 350f, 230f),
                        true,
                        false,
                        false,
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("menu-open").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("menu-favorite-remove").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("menu-close").assertIsFocused().assertIsDisplayed()
    }
}
