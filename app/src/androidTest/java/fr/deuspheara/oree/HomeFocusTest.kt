package fr.deuspheara.oree

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.StateRestorationTester
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.bottom
import androidx.compose.ui.test.center
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.height
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.right
import androidx.compose.ui.test.top
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class HomeFocusTest {
    @get:Rule val compose = createComposeRule()
    private val apps =
        listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot").map { TvApp(it, it, it, 0) }

    private fun show(items: State<List<TvApp>>, resume: State<Int> = mutableStateOf(0), open: (TvApp) -> Unit = {}) {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(items.value, false),
                    resume = resume.value,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.OpenApp -> {
                                open(action.app)
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun movingFirstAppBetweenCatalogAndDockKeepsFocusWhenGlobalOrderIsUnchanged() {
        val favorites = mutableStateOf(emptyList<String>())
        val resume = mutableStateOf(0)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = resume.value,
                    reduceMotion = true,
                    favorites = favorites.value,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
        // A command acknowledgement can precede the DataStore observation of its new membership.
        compose.runOnIdle { resume.value++ }
        compose.waitForIdle()
        compose.runOnIdle { favorites.value = listOf("Alpha") }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
        compose.runOnIdle { resume.value++ }
        compose.waitForIdle()
        compose.runOnIdle { favorites.value = emptyList() }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
    }

    @Test
    fun secondarySelectionCoalescesWhileNativeFocusIsImmediate() {
        val selections = mutableListOf<String?>()
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.SelectArtwork -> {
                                val it = action.app
                                selections += it?.id
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { selections.clear() }
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        compose.runOnIdle { assertEquals(emptyList<String?>(), selections) }
        compose.onNodeWithTag("app:Bravo").performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("app:Charlie").assertIsFocused()
        compose.runOnIdle { assertEquals(emptyList<String?>(), selections) }
        compose.mainClock.advanceTimeBy(240)
        compose.runOnIdle { assertEquals(listOf("Charlie"), selections) }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun settledSelectionRefreshesCatalogMetadata() {
        val items = mutableStateOf(apps)
        val delivered = mutableListOf<String>()
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(items.value, false),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.SelectArtwork -> {
                                val it = action.app
                                it?.let { app -> delivered += app.label }
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { delivered.clear() }
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(64)
        compose.mainClock.advanceTimeBy(240)
        compose.runOnIdle { assertEquals(listOf("Bravo"), delivered) }
        compose.runOnIdle {
            items.value = apps.map {
                if (it.id == "Bravo") it.copy(label = "Bravo updated") else it
            }
        }
        compose.mainClock.advanceTimeBy(320)
        compose
            .onNodeWithTag("app:Bravo")
            .assertIsFocused()
            .assertContentDescriptionEquals("Bravo updated")
        compose.runOnIdle { assertEquals(listOf("Bravo", "Bravo updated"), delivered) }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun controlEditingHidesCardsAndRestoresTheirState() {
        var hidden by mutableStateOf(false)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Alpha"),
                    hideCards = hidden,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("app:Alpha").assertIsDisplayed()
        compose.runOnIdle { hidden = true }
        compose.onNodeWithTag("app:Alpha").assertDoesNotExist()
        compose.onNodeWithTag("home-header").assertIsDisplayed()
        compose.runOnIdle { hidden = false }
        compose.onNodeWithTag("app:Alpha").assertIsDisplayed()
    }

    @Test
    fun homeHasNoHdmiSection() {
        show(mutableStateOf(apps))
        compose.onNodeWithTag("inputs").assertDoesNotExist()
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
    }

    @Test
    fun dpadMovesAndClickOpensSelectedApp() {
        var launched: String? = null
        show(mutableStateOf(apps), open = { launched = it.id })
        compose
            .onNodeWithTag("app:Alpha")
            .assertIsFocused()
            .assertContentDescriptionEquals("Alpha")
            .performKeyInput {
                pressKey(Key.DirectionRight)
            }
        compose.onNodeWithTag("app:Bravo").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals("Bravo", launched) }
    }

    @Test
    fun keyboardAndPointerActivateTheChosenApplicationOnce() {
        val opened = mutableListOf<String>()
        val resume = mutableStateOf(0)
        show(mutableStateOf(apps), resume, open = { opened += it.id })
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(listOf("Alpha"), opened) }
        compose.onNodeWithTag("app:Bravo").performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf("Alpha", "Bravo"), opened) }
        compose.runOnIdle { resume.value++ }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun reorderedCatalogKeepsComponentFocus() {
        val items = mutableStateOf(apps)
        show(items)
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { items.value = listOf(apps[1], apps[0]) + apps.drop(2) }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun removalSelectsLogicalNeighbor() {
        val items = mutableStateOf(apps)
        show(items)
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { items.value = apps.filterNot { it.id == "Bravo" } }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Charlie").assertIsFocused()
    }

    @Test
    fun resumeRestoresPreviousComponent() {
        val resume = mutableStateOf(0)
        show(mutableStateOf(apps), resume)
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { resume.value++ }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun emptyCatalogOffersFocusedSettingsAndRetry() {
        show(mutableStateOf(emptyList()))
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.onNodeWithTag("retry").assertExists()
        compose.onNodeWithTag("commands").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag("retry").assertIsFocused()
    }

    @Test
    fun savedFocusSurvivesUiRecreation() {
        val restoration = androidx.compose.ui.test.junit4.StateRestorationTester(compose)
        restoration.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun verticalNavigationReturnsToOriginalColumn() {
        show(mutableStateOf(apps + (1..6).map { TvApp("Extra$it", "fixture", "Extra$it", 0) }))
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("app:Alpha").assertIsNotFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
    }

    @Test
    fun returningFromSettingsRestoresHeaderControl() {
        val resume = mutableStateOf(0)
        show(mutableStateOf(apps), resume)
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        // The empty favorites dock is above the catalog; toolbar follows it.
        compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.runOnIdle { resume.value++ }
        compose.waitForIdle()
        compose.onNodeWithTag("commands").assertIsFocused()
    }

    @Test
    fun animatedFocusKeepsTheNativeSpatialRectangleStable() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = false,
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("app:Bravo").fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.mainClock.advanceTimeBy(75)
        val during = compose.onNodeWithTag("app:Bravo").fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(200)
        val after = compose.onNodeWithTag("app:Bravo").fetchSemanticsNode().boundsInRoot
        assertEquals(before, during)
        assertEquals(before, after)
        compose.onNodeWithTag("app:Bravo").assertIsFocused().assertContentDescriptionEquals("Bravo")
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun rapidAnimatedNavigationRetargetsAndStillActivatesOnce() {
        val opened = mutableListOf<String>()
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = false,
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.OpenApp -> {
                                val it = action.app
                                opened += it.id
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        repeat(12) {
            compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("app:Bravo").assertIsFocused()
            compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("app:Alpha").assertIsFocused()
        }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("app:Alpha").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(listOf("Alpha"), opened) }
    }

    @Test
    fun lowerRowFocusBringsTheLabelAndArtworkMarginIntoView() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = false,
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        val focused = compose.onAllNodes(isFocused()).fetchSemanticsNodes().single()
        val app =
            focused.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription]
                .single()
        val tile = compose.onNodeWithTag("app-tile:$app").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(
            "The complete tile including label must fit the viewport",
            tile.bottom <= viewport.bottom,
        )
    }

    @Test
    fun mouseHoverTransfersFocusThenDpadContinuesFromThatCard() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = false,
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").performMouseInput { moveTo(center) }
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
    }

    @Test
    fun becomingFavoriteKeepsFocusOnTheSameComponent() {
        val favorites = mutableStateOf(emptyList<String>())
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = favorites.value,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { favorites.value = listOf("Bravo") }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        // The other section may be virtualized offscreen. Verify its real D-pad
        // transition instead of assuming both sections are composed at once.
        compose.onNodeWithTag("app:Bravo").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("app:Alpha").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        compose.runOnIdle { favorites.value = emptyList() }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun longOkOffersContextWithoutAlsoLaunching() {
        var opened = 0
        var menus = 0
        var menu: String? = null
        var hostView: android.view.View? = null
        compose.setContent {
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect { hostView = view }
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    controls = setOf(HomeControl.APP_MENU),
                    onAction = { action ->
                        when (action) {
                            is HomeAction.OpenApp -> {
                                val it = action.app
                                opened++
                            }

                            is HomeAction.OpenAppMenu -> {
                                val app = action.app
                                menu = app.id
                                menus++
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.waitUntil(5_000) { hostView?.hasWindowFocus() == true }
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
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
        // Process native Down before advancing the virtual long-key timeout.
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        android.os.SystemClock.sleep(600)
        // Foundation's long-key timeout uses the Compose test dispatcher. Keep
        // its virtual clock aligned with the real native held-key interval.
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
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                down,
                android.os.SystemClock.uptimeMillis(),
                android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                0,
            ),
        )
        compose.runOnIdle {
            assertEquals("Alpha", menu)
            assertEquals(1, menus)
            assertEquals(0, opened)
        }
    }

    @Test
    fun initialFavoriteFocusKeepsContextTitleBelowTheHeader() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Bravo"),
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        compose.onNodeWithTag("context-title").assertDoesNotExist()
        val dock = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot
        val header = compose.onNodeWithTag("home-header").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Dock stays below toolbar", dock.top >= header.bottom)
    }

    @Test
    fun readyMarkerWaitsForCatalogReadinessAndSurvivesScrolling() {
        val loading = mutableStateOf(true)
        val catalog = (1..40).map { TvApp("App$it", "fixture", "App$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, loading.value),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("home-ready").assertDoesNotExist()
        compose.runOnIdle { loading.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("home-ready").assertIsDisplayed()
        compose.onNodeWithTag("app:App1").assertIsFocused()
        compose.onNodeWithTag("home").performScrollToIndex(40)
        compose.waitForIdle()
        compose.onNodeWithTag("home-ready").assertIsDisplayed()
    }

    @Test
    fun singleFavoriteStaysInItsRowAndHeaderDownRestoresTheApp() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Bravo"),
                    controls = setOf(HomeControl.APP_MENU),
                    onAction = { action ->
                        when (action) {
                            is HomeAction.OpenAppMenu -> {
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionLeft)
        }
        compose.onNodeWithTag("app:Bravo").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("commands").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionLeft)
        }
        compose.onNodeWithTag("commands").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun cachedCatalogFailureRetainsTheLastFocusedComponent() {
        val failed = mutableStateOf(false)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false, failed.value),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        compose.runOnIdle { failed.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
        compose.runOnIdle { failed.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Bravo").assertIsFocused()
    }

    @Test
    fun overflowingDockScrollsAndRestoresItsLastFavorite() {
        val catalog = (1..14).map { TvApp("Item$it", "fixture", "Item$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = catalog.take(8).map { it.id },
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Item1").assertIsFocused()
        repeat(7) { compose.onRoot().performKeyInput { pressKey(Key.DirectionRight) } }
        compose.onNodeWithTag("app:Item8").assertIsFocused().assertIsDisplayed()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("app:Item8").assertIsNotFocused()
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Item8").assertIsFocused()
    }

    @Test
    fun returningToHeaderParksDockAtItsOriginalBottomPosition() {
        val catalog = (1..40).map { TvApp("Card$it", "fixture", "Card$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Card1"),
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        val initial = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(
            "Dock is in the bottom third",
            initial.top > viewport.height * .6f,
        )
        compose.onNodeWithTag("app:Card1").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        val scrolled = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Card1").assertIsFocused()
        org.junit.Assert.assertTrue(
            "Returning to dock does not park it at bottom",
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top <=
                scrolled.top + 1f,
        )
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag("commands").assertIsFocused()
        assertEquals(
            initial,
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot,
        )
        val clock =
            compose
                .onNodeWithTag("home-clock")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        org.junit.Assert.assertTrue(
            "Time is at the left, clear of header controls",
            clock.right < viewport.center.x,
        )
        compose.onNodeWithTag("system-settings").assertDoesNotExist()
        compose.onNodeWithTag("edit-apps").assertDoesNotExist()
    }

    @Test
    fun onlyUpFromDockAnimatesItsReturnToBottom() {
        val catalog = (1..40).map { TvApp("Card$it", "fixture", "Card$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = false,
                    favorites = listOf("Card1"),
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        val initial = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("app:Card1").performKeyInput { pressKey(Key.DirectionDown) }
        compose.waitForIdle()
        val scrolled = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(scrolled < initial)
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.waitForIdle()
        compose.onNodeWithTag("app:Card1").assertIsFocused()
        val atDock = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue("Dock stays at top until another Up", atDock <= scrolled + 1f)
        compose.mainClock.autoAdvance = false
        try {
            compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
            compose.mainClock.advanceTimeBy(48)
            compose.waitForIdle()
            val during =
                compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
            org.junit.Assert.assertTrue(
                "Return has intermediate positions",
                during < initial && during >= scrolled,
            )
            compose.mainClock.advanceTimeBy(2000)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.waitForIdle()
        compose.onNodeWithTag("commands").assertIsFocused()
        assertEquals(
            initial,
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top,
            1f,
        )
    }

    @Test
    fun focusedCatalogCardsClearTheFloatingToolbarAfterScrolling() {
        val catalog = (1..40).map { TvApp("Card$it", "fixture", "Card$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = false,
                    favorites = listOf("Card1"),
                    systemReduceMotion = false,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        repeat(4) {
            compose.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
            compose.waitForIdle()
        }
        val focused = compose.onAllNodes(isFocused()).fetchSemanticsNodes().single()
        val header = compose.onNodeWithTag("home-header").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(
            "Expanded card remains clear of toolbar",
            focused.boundsInRoot.top - 12f >= header.bottom,
        )
        compose.onNodeWithTag("home").assertIsDisplayed()
    }

    @Test
    fun headerFocusAnimationKeepsItsInputBoundsFixed() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = false,
                    systemReduceMotion = false,
                    retainDock = true,
                    sourceKey = "fixture",
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        val before = compose.onNodeWithTag("commands").fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithTag("commands").performSemanticsAction(
                androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
            ) {
                it()
            }
            compose.mainClock.advanceTimeBy(80)
            assertEquals(
                before,
                compose.onNodeWithTag("commands").fetchSemanticsNode().boundsInRoot,
            )
            compose.mainClock.advanceTimeBy(1000)
        } finally {
            compose.mainClock.autoAdvance = true
        }
        compose.onNodeWithTag("commands").assertIsFocused()
    }

    @Test
    fun retainedDockPreservesNativeFocusAndOriginalScrollReturn() {
        val catalog = (1..40).map { TvApp("Card$it", "fixture", "Card$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Card1"),
                    retainDock = true,
                    sourceKey = "fixture",
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitUntil(10000) {
            compose.onAllNodesWithTag("retained-menu-image").fetchSemanticsNodes().isNotEmpty()
        }
        val before = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("app:Card1").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("app:Card1").assertIsFocused()
        org.junit.Assert.assertTrue(
            "Dock stays raised",
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top <
                before.top,
        )
        compose.onNodeWithTag("app:Card1").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.waitForIdle()
        assertEquals(
            before,
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot,
        )
    }

    @Test
    fun emptyDockAlsoWaitsForAnotherUpBeforeParking() {
        val catalog = (1..40).map { TvApp("Card$it", "fixture", "Card$it", 0) }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(catalog, false),
                    resume = 0,
                    reduceMotion = true,
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        val raised = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("app:Card1").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("choose-favorites").assertIsFocused()
        val atDock = compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(
            "Empty dock is raised, not parked",
            atDock < compose.onNodeWithTag("home").fetchSemanticsNode().boundsInRoot.height * .6f,
        )
        compose.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("commands").assertIsFocused()
        compose.waitForIdle()
        org.junit.Assert.assertTrue(
            compose.onNodeWithTag("favorites-dock").fetchSemanticsNode().boundsInRoot.top > raised,
        )
    }

    @Test
    fun heldUpRepeatDoesNotSkipTheDockIntoHeader() {
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = listOf("Alpha"),
                    onAction = { action ->
                        when (action) {
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("app:Alpha").performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("app:Alpha").assertIsFocused()
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val time = android.os.SystemClock.uptimeMillis()
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                time,
                time,
                android.view.KeyEvent.ACTION_DOWN,
                android.view.KeyEvent.KEYCODE_DPAD_UP,
                1,
            ),
        )
        instrumentation.sendKeySync(
            android.view.KeyEvent(
                time,
                time + 1,
                android.view.KeyEvent.ACTION_UP,
                android.view.KeyEvent.KEYCODE_DPAD_UP,
                0,
            ),
        )
        compose.waitForIdle()
        compose.onNodeWithTag("app:Alpha").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("commands").assertIsFocused()
    }
}
