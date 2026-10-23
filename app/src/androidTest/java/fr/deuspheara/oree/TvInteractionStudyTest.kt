package fr.deuspheara.oree

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import fr.deuspheara.oree.feature.home.presentation.component.FolderPanel
import fr.deuspheara.oree.feature.profiles.presentation.screen.PhotoViewerScreen
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.presentation.screen.SearchPanel
import fr.deuspheara.oree.feature.settings.presentation.screen.SettingsScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class TvInteractionStudyTest {
    @get:Rule val compose = createComposeRule()
    private fun focus(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
    private fun activate(tag: String) {
        focus(tag).performKeyInput { pressKey(Key.DirectionCenter) }
        compose.waitForIdle()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        compose.waitForIdle()
        android.os.SystemClock.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.cacheDir, "tvos26-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun categoryReturnRestoresStableOptionAndScroll() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SettingsScreen(LauncherPreferences(), {}, 0, reduceMotion = true)
            }
        }
        activate("settings-category:APPEARANCE")
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("settings-defaults"))
        compose.onNodeWithTag("settings-defaults").performScrollTo()
        focus("settings-defaults")
        back()
        activate("settings-category:HOME")
        back()
        activate("settings-category:APPEARANCE")
        compose.onNodeWithTag("settings-defaults").assertIsFocused()
        compose.onNodeWithTag("settings-content").performScrollToNode(hasTestTag("settings-theme"))
        activate("settings-theme")
        capture("choice")
    }

    @Test
    fun uninstallBackReturnsToItsOwnMenuRow() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                AppContextMenu(
                    "Fixture", Rect(100f, 100f, 200f, 200f), false, false, false,
                    {}, {}, {}, onUninstall = {},
                )
            }
        }
        activate("menu-uninstall")
        compose.onNodeWithTag("uninstall-cancel").assertIsFocused()
        back()
        compose.onNodeWithTag("menu-uninstall").assertIsFocused()
    }

    @Test
    fun folderRemovalExplainsScopeAndStartsAtCancel() {
        var removed = false
        val app = TvApp("org.example.alpha/.TV", "org.example.alpha", "Alpha", 0)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                FolderPanel(
                    AppFolder("fixture", "Cinema", listOf(app.id)), listOf(app), 0, true, false, false,
                    null, {}, { _, _, _ -> }, {}, { removed = true }, {}, { _, _ -> }, {},
                )
            }
        }
        capture("folder")
        activate("folder-options")
        activate("folder-delete")
        compose.onNodeWithTag("folder-remove-cancel").assertIsFocused()
        back()
        compose.runOnIdle { assertFalse(removed) }
        compose.onNodeWithTag("folder-options").assertIsFocused()
    }

    @Test
    fun searchUpdatesDoNotStealFocusFromClose() {
        val first = TvApp("org.example.alpha/.TV", "org.example.alpha", "Alpha", 0)
        var results by mutableStateOf(listOf<SearchItem>(SearchItem.App(first)))
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SearchPanel("", {}, 0, true, {}, { _, _, _ -> }, {}, results)
            }
        }
        focus("search-close")
        compose.runOnIdle { results = results + SearchItem.Folder(AppFolder("folder", "Cinema", emptyList())) }
        compose.onNodeWithTag("search-close").assertIsFocused()
        capture("search")
    }

    @Test
    fun galleryLoadingRetainsUserNavigationWhenPhotosArrive() {
        var photos by mutableStateOf<LocalPhotos?>(null)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                fr.deuspheara.oree.feature.profiles.presentation.screen.PhotoGalleryScreen(
                    photos, true, false, true, false, false, {}, {}, {}, {}, {}, null,
                )
            }
        }
        compose.onNodeWithTag("photo-close").assertIsFocused().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("photo-other-source").assertIsFocused()
        compose.runOnIdle {
            photos = LocalPhotos(PhotoAvailability.AVAILABLE, listOf(LocalPhoto("content://fixture/1", "Fixture")))
        }
        compose.onNodeWithTag("photo-other-source").assertIsFocused()
    }

    @Test
    fun galleryPreviewReturnsToTheOriginalThumbnailWithoutImporting() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = "android.resource://${context.packageName}/${fr.deuspheara.oree.core.platform.R.drawable.landscape_forest}"
        val photos = listOf(LocalPhoto(uri, "Forest"), LocalPhoto("$uri?fixture=second", "Second"))
        val collection = LocalPhotos(PhotoAvailability.AVAILABLE, photos)
        var preview by mutableStateOf<LocalPhoto?>(null)
        var imported = false
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                fr.deuspheara.oree.feature.profiles.presentation.screen.PhotoGalleryScreen(
                    collection, true, false, true, false, false, { imported = true }, {}, {}, {}, {}, null,
                    onPreview = { preview = it },
                )
                preview?.let { PhotoViewerScreen(it, collection, true, { preview = null }) }
            }
        }
        activate("local-photo:1")
        compose.onNodeWithTag("photo-viewer-name").assertTextEquals("Second")
        back()
        compose.onNodeWithTag("local-photo:1").assertIsFocused()
        compose.runOnIdle { assertFalse(imported) }
    }

    @Test
    fun photoDirectionsBrowseAndBackReturnsThroughControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = "android.resource://${context.packageName}/${fr.deuspheara.oree.core.platform.R.drawable.landscape_forest}"
        val photos = listOf(LocalPhoto(uri, "Forest"), LocalPhoto("$uri?fixture=second", "Second"))
        var closed = false
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                PhotoViewerScreen(photos.first(), LocalPhotos(PhotoAvailability.AVAILABLE, photos), true, { closed = true })
            }
        }
        compose.onNodeWithTag("photo-viewer-image").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("photo-viewer-name").assertTextEquals("Second")
        compose.runOnIdle { assertFalse(closed) }
        compose.onNodeWithTag("photo-viewer-image").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("photo-viewer-close").assertIsFocused()
        back()
        compose.onNodeWithTag("photo-viewer-image").assertIsFocused()
        compose.runOnIdle { assertFalse(closed) }
        capture("photo")
        back()
        compose.runOnIdle { assertTrue(closed) }
    }
}
