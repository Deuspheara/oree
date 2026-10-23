package fr.deuspheara.oree

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.down
import androidx.compose.ui.test.height
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.top
import androidx.compose.ui.test.width
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ProfilePanelTest {
    @get:Rule val compose = createComposeRule()
    private val snapshot =
        ProfileSnapshot(
            "first",
            listOf(LocalProfile("first", "First"), LocalProfile("second", "Second", "avatar_03")),
        )

    private fun activate(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
        compose.onNodeWithTag(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }

    private fun holdProfile(id: String) {
        compose.onNodeWithTag("profile:$id").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
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
        compose.onNodeWithTag("profile-name").assertExists()
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
        compose.waitForIdle()
    }

    @Test
    fun longPressEditsThePressedInactiveProfileWithoutSelectingOrSaving() {
        var selected: String? = null
        var saves = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    { selected = it },
                    { _, _, _ -> saves++ },
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithTag("edit-profile").assertDoesNotExist()
        compose.onNodeWithText("✓").assertDoesNotExist()
        holdProfile("second")
        compose.onNodeWithTag("profile-name").assertTextEquals("Second")
        compose.onNodeWithTag("profile-avatar:avatar_03").assertIsFocused()
        val title =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .targetContext
                .getString(R.string.edit_profile)
        val titleBounds =
            compose.onNodeWithText(title).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val panelBounds = compose.onNodeWithTag("profile-panel").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Editor title stays visible when avatar receives focus",
            titleBounds.top >= panelBounds.top,
        )
        compose.runOnIdle {
            assertNull(selected)
            assertEquals(0, saves)
        }
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("profile-name").assertDoesNotExist()
        compose.onNodeWithTag("profile:second").assertIsFocused()
    }

    @Test
    fun selectionUsesStableProfileIdentity() {
        var selected: String? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    { selected = it },
                    { _, _, _ -> },
                    {},
                    {},
                )
            }
        }
        compose.onNodeWithTag("profile:first").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
        }
        compose.onNodeWithTag("profile-active:first", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("profile-active:second", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("profile:second").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals("second", selected) }
    }

    @Test
    fun createKeepsTheEditedNameAndAvatar() {
        var saved: Triple<String?, String, String>? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    {},
                    { id, name, avatar -> saved = Triple(id, name, avatar) },
                    {},
                    {},
                )
            }
        }
        activate("add-profile")
        compose.onNodeWithTag("profile-name").performTextInput("New person")
        compose.onNodeWithTag("profile-avatar:avatar_01").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) {
            it()
        }
        compose.onNodeWithTag("profile-avatar:avatar_01").performKeyInput {
            repeat(7) { pressKey(Key.DirectionRight) }
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("profile-avatar:avatar_08").assertIsSelected()
        activate("save-profile")
        compose.runOnIdle { assertEquals(Triple(null, "New person", "avatar_08"), saved) }
    }

    @Test
    fun blankProfileNameDisablesApplyUntilTextIsEntered() {
        var saves = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    {},
                    { _, _, _ -> saves++ },
                    {},
                    {},
                )
            }
        }
        activate("add-profile")
        compose.onNodeWithTag("save-profile").assertIsNotEnabled()
        compose.onNodeWithTag("profile-name").performTextInput("   ")
        compose.onNodeWithTag("save-profile").assertIsNotEnabled()
        compose.onNodeWithTag("profile-name").performTextReplacement("New name")
        compose.onNodeWithTag("save-profile").assertIsEnabled()
        activate("save-profile")
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun unselectedAvatarFocusAndSelectionStaySeparate() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(snapshot, false, false, false, false, {}, { _, _, _ -> }, {}, {})
            }
        }
        holdProfile("first")
        compose.onNodeWithTag("profile-avatar:avatar_01").performKeyInput {
            pressKey(Key.DirectionRight)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("profile-avatar:avatar_02").assertIsFocused().assertIsNotSelected()
        compose.onNodeWithTag("profile-avatar:avatar_01").assertIsSelected()
        compose.onNodeWithTag("profile-avatar:avatar_02").performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("profile-avatar:avatar_02").assertIsSelected()
    }

    @Test
    fun rowEdgesAndBackRetainLogicalSelection() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(snapshot, true, false, false, false, {}, { _, _, _ -> }, {}, {})
            }
        }
        holdProfile("first")
        compose.onNodeWithTag("profile-avatar-row").assertIsDisplayed()
        val bounds =
            compose.onNodeWithTag("profile-avatar:avatar_01").fetchSemanticsNode().boundsInRoot
        assertEquals("Avatar input remains square", bounds.width, bounds.height, 1f)
        compose.onNodeWithTag("profile-avatar:avatar_01").performKeyInput {
            pressKey(Key.DirectionLeft)
        }
        compose.onNodeWithTag("profile-avatar:avatar_01").assertIsFocused().performKeyInput {
            repeat(7) { pressKey(Key.DirectionRight) }
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("profile-avatar:avatar_08").assertIsFocused().assertIsSelected()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("profile:first").assertIsFocused()
        holdProfile("first")
        // Back cancels the unsaved edit.
        compose.onNodeWithTag("profile-avatar:avatar_01").assertIsSelected().assertIsFocused()
    }

    @Test
    fun editorDraftSurvivesSavedStateRestoration() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(snapshot, true, false, false, false, {}, { _, _, _ -> }, {}, {})
            }
        }
        activate("add-profile")
        compose.onNodeWithTag("profile-name").performTextInput("Restored")
        compose.onNodeWithTag("profile-avatar-row").performScrollToIndex(7)
        activate("profile-avatar:avatar_08")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("profile-name").assertTextEquals("Restored")
        compose.onNodeWithTag("profile-avatar:avatar_08").assertIsSelected().assertIsFocused()
    }

    @Test
    fun renameKeepsTheOriginalProfileIdentity() {
        var saved: Triple<String?, String, String>? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    {},
                    { id, name, avatar -> saved = Triple(id, name, avatar) },
                    {},
                    {},
                )
            }
        }
        holdProfile("first")
        compose.onNodeWithTag("profile-name").performTextReplacement("Updated")
        activate("save-profile")
        compose.runOnIdle { assertEquals(Triple("first", "Updated", "avatar_01"), saved) }
    }

    @Test
    fun deletingRequiresTwoExplicitActions() {
        var deleted: String? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    snapshot,
                    true,
                    false,
                    false,
                    false,
                    {},
                    { _, _, _ -> },
                    { deleted = it },
                    {},
                )
            }
        }
        holdProfile("first")
        activate("delete-profile")
        compose.runOnIdle { assertNull(deleted) }
        activate("delete-profile")
        compose.runOnIdle { assertEquals("first", deleted) }
    }
}
