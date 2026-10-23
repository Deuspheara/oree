package fr.deuspheara.oree

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.FolderAddAppsPanel
import fr.deuspheara.oree.feature.home.presentation.component.FolderDestinationPanel
import fr.deuspheara.oree.feature.home.presentation.component.FolderPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FolderManagementTest {
    @get:Rule val compose = createComposeRule()
    private val apps = listOf("Alpha", "Bravo", "Charlie").map {
        TvApp("org.example.${it.lowercase()}/.TV", "org.example.${it.lowercase()}", it, 0)
    }
    private val folder = AppFolder("cinema", "Cinema", apps.take(2).map { it.id })

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    @Test
    fun packageChangesKeepHeaderFocusAndFirstRowUpReturnsToTitle() {
        val catalog = mutableStateOf(apps)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderPanel(
                    folder, catalog.value, 0, true, false, false, null,
                    {}, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, {},
                )
            }
        }
        compose.onNodeWithTag("app:${apps[0].id}").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("folder-rename").assertIsFocused()
        compose.runOnIdle { catalog.value = apps.drop(1) }
        compose.onNodeWithTag("folder-rename").assertIsFocused()
        compose.onNodeWithTag("folder-options").requestFocus()
        compose.runOnIdle { catalog.value = emptyList() }
        compose.onNodeWithTag("folder-options").assertIsFocused()
    }

    @Test
    fun heldOpeningReleaseDoesNotActivateFolderRemoval() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderPanel(
                    folder, apps, 0, true, false, false, null,
                    {}, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, {}, initiallyManaging = true,
                )
            }
        }
        compose.onNodeWithTag("folder-delete").assertIsFocused()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        compose.onNodeWithTag("folder-remove-confirm").assertDoesNotExist()
        compose.onNodeWithTag("folder-delete").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("folder-remove-cancel").assertIsFocused()
        compose.onNodeWithTag("app:${apps[0].id}").assertDoesNotExist()
        compose.onNodeWithTag("folder-remove-cancel").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("folder-remove-confirm").assertIsFocused().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("folder-remove-confirm").assertIsFocused()
        back()
        compose.onNodeWithTag("folder-options").assertIsFocused()
    }

    @Test
    fun creatingFocusesNameAndBackReturnsToDestinations() {
        var closed = false
        var created: String? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderDestinationPanel(emptyList(), null, false, false, {}, { created = it }, { closed = true })
            }
        }
        compose.onNodeWithTag("folder-create").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("folder-name").assertIsFocused().performTextInput("Games")
        back()
        // Android IMEs may consume Back before the form receives it.
        if (compose.onAllNodesWithTag("folder-name").fetchSemanticsNodes().isNotEmpty()) back()
        compose.onNodeWithTag("folder-create").assertIsFocused()
        compose.onNodeWithTag("folder-name").assertDoesNotExist()
        compose.runOnIdle {
            assertFalse(closed)
            assertEquals(null, created)
        }
        compose.onNodeWithTag("folder-create").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("folder-name").assertIsFocused()
    }

    @Test
    fun addAppsExcludesMembersAndTransfersOnlyOnSelect() {
        var added: TvApp? = null
        var scene by mutableStateOf<Any?>("first")
        val other = AppFolder("games", "Games", listOf(apps[2].id))
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderAddAppsPanel(folder, listOf(folder, other), apps, false, false, true, { added = it }, {}, sourceKey = scene)
            }
        }
        compose.onNodeWithTag("folder-add:${apps[0].id}").assertDoesNotExist()
        compose.onNodeWithTag("folder-add:${apps[2].id}").assertIsFocused()
        compose.runOnIdle {
            assertEquals(null, added)
            scene = "second"
        }
        compose.onNodeWithTag("folder-add:${apps[2].id}").assertIsFocused()
        compose.onNodeWithTag("folder-add:${apps[2].id}").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(apps[2], added) }
    }

    @Test
    fun cancellingAddRestoresItsHeaderTrigger() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var adding by remember { mutableStateOf(false) }
                var resume by remember { mutableIntStateOf(0) }
                FolderPanel(
                    folder, apps, resume, true, false, false, null,
                    {}, { _, _, _ -> }, {}, {}, {}, { _, _ -> }, {}, onAddApps = { adding = true },
                )
                if (adding) {
                    FolderAddAppsPanel(folder, listOf(folder), apps, false, false, true, {}, {
                        adding = false
                        resume++
                    })
                }
            }
        }
        compose.onNodeWithTag("folder-add").requestFocus().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("folder-add:${apps[2].id}").assertIsFocused()
        back()
        compose.onNodeWithTag("folder-add").assertIsFocused()
    }

    @Test
    fun allAddedLeavesCloseFocused() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderAddAppsPanel(folder, listOf(folder), apps.take(2), false, false, true, {}, {})
            }
        }
        compose.onNodeWithTag("folder-add-close").assertIsFocused()
    }
}
