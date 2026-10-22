package fr.deuspheara.oree

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.repository.DefaultCapabilityRepository
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.platform.datasource.content.AndroidContentCapabilitySource
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import fr.deuspheara.oree.feature.settings.presentation.component.BackupReviewPanel
import fr.deuspheara.oree.feature.settings.presentation.route.ContentCapabilitiesPanel
import fr.deuspheara.oree.feature.settings.presentation.screen.AmbientScreen
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ExtensionPanelsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun ambientConsumesExitGestureBeforeRestoringTheHeader() {
        val app = TvApp("org.example.app/.TV", "org.example.app", "Example", 0)
        var opened = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var ambient by remember { mutableStateOf(false) }
                var resume by remember { mutableIntStateOf(0) }
                HomeScreen(
                    HomeUiState(listOf(app), false),
                    resume,
                    true,
                    {
                        opened++
                    },
                    {},
                    {},
                    {},
                    onCommands = { ambient = true },
                    favorites = listOf(app.id),
                )
                if (ambient) {
                    AmbientScreen(
                        null,
                        false,
                        false,
                        true,
                        { LocalPhotos(PhotoAvailability.EMPTY) },
                        {
                            ambient = false
                            resume++
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:${app.id}").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("commands").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("ambient").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("ambient").assertDoesNotExist()
        compose.onNodeWithTag("commands").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.runOnIdle { assertEquals(0, opened) }
        compose.onNodeWithTag("app:${app.id}").performKeyInput { pressKey(Key.DirectionCenter) }
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun ambientPhotoWorkCancelsAtStopAndAtDismissal() {
        lateinit var owner: LifecycleOwner
        lateinit var registry: LifecycleRegistry
        var queries = 0
        var cancellations = 0
        compose.runOnUiThread {
            owner =
                object : LifecycleOwner {
                    override val lifecycle
                        get() = registry
                }
            registry = LifecycleRegistry(owner).apply { currentState = Lifecycle.State.STARTED }
        }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var shown by remember { mutableStateOf(true) }
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    if (shown) {
                        AmbientScreen(
                            null,
                            true,
                            false,
                            true,
                            {
                                queries++
                                try {
                                    awaitCancellation()
                                } finally {
                                    cancellations++
                                }
                            },
                            { shown = false },
                        )
                    }
                }
            }
        }
        compose.waitUntil(5000) { queries == 1 }
        compose.runOnIdle { registry.currentState = Lifecycle.State.CREATED }
        compose.waitUntil(5000) { cancellations == 1 }
        compose.runOnIdle { registry.currentState = Lifecycle.State.STARTED }
        compose.waitUntil(5000) { queries == 2 }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5000) { cancellations == 2 }
        compose.runOnIdle { assertEquals(2, queries) }
    }

    @Test
    fun largeTextBackupReviewStartsAtCancelAndBackNeverRestores() {
        var restores = 0
        var closes = 0
        val snapshot = ProfileSnapshot("fixture", listOf(LocalProfile("fixture", "Example")))
        compose.setContent {
            HazeTestTheme(
                LauncherPreferences(textScale = 2f, highContrast = true, reduceMotion = true),
            ) {
                BackupReviewPanel(
                    BackupReview(BackupCandidate(snapshot, 1), snapshot),
                    false,
                    false,
                    { restores++ },
                    { closes++ },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("backup-cancel").assertIsDisplayed().assertIsFocused()
        compose.onNodeWithTag("backup-cancel").performKeyInput { pressKey(Key.DirectionUp) }
        compose.onNodeWithTag("backup-profile:fixture").assertIsFocused().assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.runOnIdle {
            assertEquals(0, restores)
            assertEquals(1, closes)
        }
    }

    @Test
    fun diagnosticsSourcesRemainReachableThroughTheScrollableReport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val probe =
            AndroidContentCapabilitySource(context) { _, fields, _ ->
                android.database.MatrixCursor(fields).apply {
                    repeat(40) { index ->
                        val row = arrayOfNulls<Any>(fields.size)
                        fields.forEachIndexed { column, name ->
                            row[column] =
                                when (name) {
                                    "_id" -> index

                                    "package_name" -> "org.example.publisher"

                                    "title",
                                    "display_name",
                                    -> "Example"

                                    else -> null
                                }
                        }
                        addRow(row)
                    }
                }
            }
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                ContentCapabilitiesPanel(DefaultCapabilityRepository(probe), true, {})
            }
        }
        compose.waitUntil(5000) {
            compose
                .onAllNodesWithTag("diagnostics-source:CHANNELS")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithTag("diagnostics-close").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionUp)
        }
        for (
        (index, source) in
        listOf("CHANNELS", "PREVIEW", "WATCH_NEXT", "PROGRAMS").withIndex()
        ) {
            if (index > 0) {
                compose
                    .onNodeWithTag(
                        "diagnostics-source:${listOf("CHANNELS","PREVIEW","WATCH_NEXT","PROGRAMS")[index - 1]}",
                    )
                    .performKeyInput { pressKey(Key.DirectionDown) }
            }
            compose
                .onNodeWithTag("diagnostics-source:$source")
                .assertIsFocused()
                .assertIsDisplayed()
        }
    }
}
