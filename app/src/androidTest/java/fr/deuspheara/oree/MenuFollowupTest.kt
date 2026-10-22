package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.platform.launch.contentIntent
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import fr.deuspheara.oree.feature.profiles.presentation.route.PhotoGalleryPanel
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import fr.deuspheara.oree.feature.settings.presentation.component.CommandPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MenuFollowupTest {
    @get:Rule val compose = createComposeRule()

    private fun focus(tag: String) = compose.onNodeWithTag(tag).performSemanticsAction(
        androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
    ) {
        it()
    }

    @Test
    fun uninstallRequiresExplicitConfirmationAndStartsOnCancel() {
        var requests = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                AppContextMenu(
                    "Example",
                    Rect(20f, 20f, 200f, 200f),
                    false,
                    false,
                    false,
                    {},
                    {},
                    {},
                    reduceMotion = true,
                    onUninstall = { requests++ },
                )
            }
        }
        focus("menu-uninstall").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("uninstall-cancel").assertIsFocused()
        compose.runOnIdle { assertEquals(0, requests) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("menu-open").assertIsFocused()
        focus("menu-uninstall").performKeyInput { pressKey(Key.DirectionCenter) }
        focus("uninstall-confirm").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun upClosesOnlyTopControlRow() {
        var closes = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                CommandPanel(
                    true,
                    false,
                    {},
                    { closes++ },
                    onWallpaper = {},
                    onNetwork = {},
                    onAudio = {},
                )
            }
        }
        focus("command-audio").performKeyInput { pressKey(Key.DirectionUp) }
        compose.runOnIdle { assertEquals(0, closes) }
        focus("command-settings").performKeyInput { pressKey(Key.DirectionUp) }
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun upClosesProfileChooserFirstRow() {
        var closes = 0
        val profiles =
            ProfileSnapshot(
                "a",
                listOf(
                    LocalProfile("a", "Alpha"),
                    LocalProfile("b", "Beta"),
                    LocalProfile("c", "Gamma"),
                ),
            )
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ProfilePanel(
                    profiles,
                    true,
                    false,
                    false,
                    false,
                    {},
                    { _, _, _ -> },
                    {},
                    { closes++ },
                )
            }
        }
        focus("profile:c").performKeyInput { pressKey(Key.DirectionUp) }
        compose.runOnIdle { assertEquals(0, closes) }
        focus("profile:a").performKeyInput { pressKey(Key.DirectionUp) }
        compose.runOnIdle { assertEquals(1, closes) }
    }

    @Test
    fun watchNextNavigatesToCatalogAndRestoresAfterReturn() {
        var resume by mutableIntStateOf(0)
        val entries =
            listOf(
                ContentEntry(
                    "one",
                    "One",
                    "org.example.player",
                    null,
                    "player://one",
                    System.currentTimeMillis(),
                ),
            )
        val apps = listOf(TvApp("app", "org.example.player", "Player", 0))
        var opened = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    resume,
                    true,
                    {},
                    {},
                    {},
                    {},
                    contentEntries = entries,
                    onOpenContent = { opened++ },
                )
            }
        }
        compose.onNodeWithTag("app:app").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("content:one").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionLeft)
            pressKey(Key.DirectionRight)
        }
        compose.onNodeWithTag("content:one").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle {
            assertEquals(1, opened)
            resume++
        }
        compose.onNodeWithTag("content:one").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onNodeWithTag("app:app").assertIsFocused()
    }

    @Test
    fun galleryEmptyAndLoadingHaveReachableCloseAndOneQuery() {
        var queries = 0
        val permitted = object : android.content.ContextWrapper(
            InstrumentationRegistry.getInstrumentation().targetContext,
        ) {
            override fun checkSelfPermission(permission: String) = android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        compose.setContent {
            val registry = checkNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides permitted,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides registry,
            ) {
                HazeTestTheme(LauncherPreferences()) {
                    PhotoGalleryPanel(
                        true,
                        false,
                        false,
                        {},
                        {},
                        {},
                        requestOnOpen = false,
                        loadPhotos = {
                            queries++
                            LocalPhotos(PhotoAvailability.EMPTY)
                        },
                    )
                }
            }
        }
        compose.waitUntil(5000) { queries == 1 }
        compose.onNodeWithTag("photo-close").assertIsDisplayed().assertIsFocused()
        compose.runOnIdle { assertEquals(1, queries) }
    }

    @Test
    fun untrustedContentLinksCannotEscapeSourceOrCarryGrants() {
        val source = "org.example.player"
        assertNotNull(contentIntent("player://episode/1", source))
        assertNotNull(contentIntent("https://example.org/episode/1", source))
        assertNull(contentIntent("file:///private/image", source))
        assertNull(contentIntent("javascript:alert(1)", source))
        assertNull(
            contentIntent(
                "intent://episode#Intent;scheme=player;component=org.other/.Activity;end",
                source,
            ),
        )
        assertNull(
            contentIntent("intent://episode#Intent;scheme=player;launchFlags=0x1;end", source),
        )
        assertNull(
            contentIntent("intent://episode#Intent;scheme=player;SEL;package=org.other;end", source),
        )
        val clean =
            contentIntent(
                "intent://episode#Intent;scheme=player;package=org.example.player;S.secret=discard;end",
                source,
            )
        assertNotNull(clean)
        assertNull(clean!!.extras)
        assertEquals(source, clean.`package`)
    }

    @Test
    fun removingTheFocusedContentShelfRestoresASurvivingApp() {
        var entries by
            mutableStateOf(
                listOf(
                    ContentEntry(
                        "one",
                        "One",
                        "org.example.player",
                        null,
                        "player://one",
                        System.currentTimeMillis(),
                    ),
                ),
            )
        val apps = listOf(TvApp("app", "org.example.player", "Player", 0))
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    0,
                    true,
                    {},
                    {},
                    {},
                    {},
                    contentEntries = entries,
                )
            }
        }
        compose.onNodeWithTag("app:app").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("content:one").assertIsFocused()
        compose.runOnIdle { entries = emptyList() }
        compose.onNodeWithTag("watch-next-row").assertDoesNotExist()
        compose.onNodeWithTag("app:app").assertIsFocused()
    }
}
