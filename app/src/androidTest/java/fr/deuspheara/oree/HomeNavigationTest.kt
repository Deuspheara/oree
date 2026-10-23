package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class HomeNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val apps = (1..24).map { TvApp("app$it", "fixture$it", "App $it", 0) }
    private val folder = AppFolder("fixture", "Cinema", apps.take(3).map { it.id })

    private fun show(
        folders: List<AppFolder> = listOf(folder),
        onAction: (HomeAction) -> Unit = {},
        changingFolders: (() -> List<AppFolder>)? = null,
    ) {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    0,
                    true,
                    onAction,
                    favorites = listOf(apps[3].id),
                    folders = changingFolders?.invoke() ?: folders,
                    controls = setOf(HomeControl.OVERVIEW),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun waitFocused(tag: String) {
        try {
            compose.waitUntil(10000) {
                compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { it.config.getOrNull(SemanticsProperties.Focused) == true }
            }
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError(
                "Expected $tag; focused nodes: " +
                    compose.onAllNodes(isFocused()).fetchSemanticsNodes().map { it.config.toString() },
                error,
            )
        }
    }

    private fun selectSection(section: String) {
        compose.onNodeWithTag("overview:$section").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("overview:$section").performKeyInput { pressKey(Key.DirectionCenter) }
    }

    private fun enterPeek(enterMembers: Boolean = true) {
        compose.onNodeWithTag("folder:fixture").performKeyInput { pressKey(Key.Menu) }
        compose.waitForIdle()
        if (enterMembers) {
            waitFocused("peek-enter")
            compose.onNodeWithTag("peek-enter").performKeyInput { pressKey(Key.DirectionCenter) }
        }
    }

    private fun openOverview() {
        compose.onNodeWithTag("overview").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("overview").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.onNodeWithTag("home-overview").assertIsDisplayed()
    }

    @Test
    fun reversingHeaderDownDoesNotFinishAnObsoleteContinuationRequest() {
        val entry = fr.deuspheara.oree.core.model.ContentEntry("watch", "Sample", "fixture", null, "fixture:", 1L)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    0,
                    false,
                    {},
                    favorites = listOf(apps.first().id),
                    contentEntries = listOf(entry),
                    systemReduceMotion = false,
                    profile = fr.deuspheara.oree.core.model.LocalProfile(
                        "fixture",
                        "Sample",
                        preferences = LauncherPreferences(homeContentLayout = fr.deuspheara.oree.core.model.HomeContentLayout.ABOVE_DOCK),
                    ),
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("home").performScrollToIndex(apps.size)
        compose.waitForIdle()
        org.junit.Assert.assertTrue(
            "Continuation item must be completely outside the viewport before its reveal",
            compose.onNodeWithTag("home").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value() >= 1f,
        )
        compose.onNodeWithTag("commands").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("commands").performKeyInput { pressKey(Key.DirectionDown) }
            compose.mainClock.advanceTimeBy(64)
            compose.onNodeWithTag("commands").performKeyInput { pressKey(Key.DirectionUp) }
            compose.mainClock.advanceTimeBy(1600)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.onNodeWithTag("content:watch").assertIsNotFocused()
    }

    @Test
    fun continuationUsesScreenWidthAndKeepsFirstCardAlignedWithHeading() {
        val entries = (1..6).map {
            fr.deuspheara.oree.core.model.ContentEntry("watch$it", "Sample $it", "fixture$it", null, "fixture:", it.toLong())
        }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    0,
                    true,
                    {},
                    contentEntries = entries,
                    profile = fr.deuspheara.oree.core.model.LocalProfile(
                        "fixture",
                        "Sample",
                        preferences = LauncherPreferences(homeContentLayout = fr.deuspheara.oree.core.model.HomeContentLayout.ABOVE_DOCK),
                    ),
                )
            }
        }
        compose.onNodeWithTag("home").performScrollToIndex(0)
        val screen = compose.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        val row = compose.onNodeWithTag("watch-next-row").fetchSemanticsNode().boundsInRoot
        val heading = compose.onNodeWithTag("continuation-heading").fetchSemanticsNode().boundsInRoot
        val card = compose.onNodeWithTag("content:watch1").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.left, row.left, 1f)
        assertEquals(screen.right, row.right, 1f)
        assertEquals(heading.left, card.left, 1f)
    }

    @Test
    fun revealingHomeDoesNotRestoreContinuationOverTheFocusedHeader() {
        var covered by mutableStateOf(false)
        var resume by mutableStateOf(0)
        var entries by mutableStateOf(
            listOf(fr.deuspheara.oree.core.model.ContentEntry("watch", "Sample", "fixture", null, "fixture:", 1L)),
        )
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    HomeUiState(apps, false),
                    resume,
                    true,
                    {},
                    backgroundOnly = covered,
                    contentEntries = entries,
                    profile = fr.deuspheara.oree.core.model.LocalProfile(
                        "fixture",
                        "Sample",
                        "avatar_03",
                        preferences = LauncherPreferences(homeContentLayout = fr.deuspheara.oree.core.model.HomeContentLayout.ABOVE_DOCK),
                    ),
                    controls = setOf(HomeControl.COMMANDS),
                )
            }
        }
        compose.onNodeWithTag("home").performScrollToIndex(0)
        compose.onNodeWithTag("content:watch").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        waitFocused("content:watch")
        compose.onNodeWithTag("content:watch").performKeyInput { pressKey(Key.DirectionUp) }
        waitFocused("commands")
        compose.runOnIdle { covered = true }
        compose.waitForIdle()
        compose.runOnIdle {
            covered = false
            resume++
        }
        waitFocused("commands")
        compose.runOnIdle { entries = listOf(entries.first().copy(id = "replacement")) }
        compose.waitForIdle()
        compose.onNodeWithTag("commands").assertIsFocused()
    }

    @Test
    fun overviewJumpsToSectionsAndRemembersTheAppWithinASection() {
        show()
        openOverview()
        selectSection("APPS")
        waitFocused("app:app5")
        compose.onNodeWithTag("app:app5").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("app:app6").assertIsFocused()
        openOverview()
        selectSection("FAVORITES")
        waitFocused("app:app4")
        compose.onNodeWithTag("app:app4").assertIsFocused()
        openOverview()
        selectSection("APPS")
        waitFocused("app:app6")
        compose.onNodeWithTag("app:app6").assertIsFocused()
    }

    @Test
    fun overviewBackRestoresItsTrigger() {
        show()
        openOverview()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(1)
        compose.waitForIdle()
        compose.onNodeWithTag("home-overview").assertDoesNotExist()
        compose.onNodeWithTag("overview").assertIsFocused()
    }

    @Test
    fun folderPeekLaunchesSelectedMemberAndBackReturnsToFolder() {
        var launched: TvApp? = null
        show(onAction = { if (it is HomeAction.OpenApp) launched = it.app })
        openOverview()
        selectSection("FOLDERS")
        waitFocused("folder:fixture")
        compose.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(
                fr.deuspheara.oree.core.designsystem.R.string.folder_peek_hint,
            ),
        ).assertDoesNotExist()
        enterPeek()
        compose.onNodeWithTag("folder-peek").assertIsDisplayed()
        waitFocused("app:app1")
        compose.onNodeWithTag("app:app1").assertIsFocused().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("app:app2").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(apps[1], launched) }
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(1)
        compose.waitForIdle()
        waitFocused("folder:fixture")
        enterPeek()
        waitFocused("app:app2")
        compose.onNodeWithTag("app:app2").assertIsFocused()
    }

    @Test
    fun emptyFolderPeekStillOffersFullFolder() {
        var opened: String? = null
        show(listOf(folder.copy(apps = emptyList())), onAction = { if (it is HomeAction.OpenFolder) opened = it.id })
        openOverview()
        selectSection("FOLDERS")
        waitFocused("folder:fixture")
        enterPeek(enterMembers = false)
        waitFocused("peek-open-folder")
        compose.onNodeWithTag("peek-open-folder").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(folder.id, opened) }
    }

    @Test
    fun downContinuesThroughTheCatalogWithoutEnteringPeek() {
        show()
        openOverview()
        selectSection("FOLDERS")
        waitFocused("folder:fixture")
        compose.onNodeWithTag("folder:fixture").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("folder-peek").assertDoesNotExist()
        compose.onNodeWithTag("app:app10").assertIsFocused()
    }

    @Test
    fun heldSelectReleaseDoesNotEnterPeekUntilFreshSelect() {
        show()
        openOverview()
        selectSection("FOLDERS")
        waitFocused("folder:fixture")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
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
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val heldMillis = android.view.ViewConfiguration.getLongPressTimeout().toLong() + 150
        android.os.SystemClock.sleep(heldMillis)
        compose.mainClock.advanceTimeBy(heldMillis)
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                1,
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
        waitFocused("peek-enter")
        compose.onNodeWithTag("peek-enter").performKeyInput { pressKey(Key.DirectionCenter) }
        waitFocused("app:app1")
    }

    @Test
    fun changingMembersDoesNotStealPeekHeaderFocus() {
        val folders = androidx.compose.runtime.mutableStateOf(listOf(folder))
        show(changingFolders = { folders.value })
        openOverview()
        selectSection("FOLDERS")
        waitFocused("folder:fixture")
        enterPeek(enterMembers = false)
        waitFocused("peek-enter")
        compose.onNodeWithTag("peek-open-folder").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.runOnIdle { folders.value = listOf(folder.copy(apps = folder.apps.take(2))) }
        compose.onNodeWithTag("peek-open-folder").assertIsFocused()
    }
}
