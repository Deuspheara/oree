package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.repository.DataStoreProfileRepository
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.AvatarIds
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.moveFavoriteApp
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FavoriteReorderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun remoteMovesThroughOverflowAndCommitsWithoutLaunching() {
        val apps = (1..9).map { TvApp("app$it", "app$it", "App $it", 0) }
        var stored = apps.map { it.id }
        var opens = 0
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var draft by remember { mutableStateOf(stored) }
                var moving by remember { mutableStateOf<String?>(apps.first().id) }
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = draft,
                    movingFavorite = moving,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.OpenApp -> {
                                val it = action.app
                                opens++
                            }

                            is HomeAction.MoveFavorite -> {
                                val it = action.direction
                                draft =
                                    moveFavoriteApp(
                                        draft,
                                        moving!!,
                                        it,
                                        apps.mapTo(hashSetOf()) { app -> app.id },
                                    )
                            }

                            HomeAction.CommitMove -> {
                                stored = draft
                                moving = null
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        repeat(8) {
            compose.onNodeWithTag("app:app1").assertIsFocused().performKeyInput {
                pressKey(Key.DirectionRight)
            }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("app:app1").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("favorite-move").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("app1", stored.last())
            assertEquals(0, opens)
        }
        compose.onNodeWithTag("app:app1").assertIsFocused()
    }

    @Test
    fun backCancelsPreviewAndRetainsTrigger() {
        val apps = listOf("A", "B", "C").map { TvApp(it, it, it, 0) }
        val original = apps.map { it.id }
        var committed = false
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var draft by remember { mutableStateOf(original) }
                var moving by remember { mutableStateOf<String?>("A") }
                HomeScreen(
                    state = HomeUiState(apps, false),
                    resume = 0,
                    reduceMotion = true,
                    favorites = draft,
                    movingFavorite = moving,
                    onAction = { action ->
                        when (action) {
                            is HomeAction.MoveFavorite -> {
                                val it = action.direction
                                draft = moveFavoriteApp(draft, "A", it, original.toSet())
                            }

                            HomeAction.CommitMove -> {
                                committed = true
                            }

                            HomeAction.CancelMove -> {
                                draft = original
                                moving = null
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:A").performKeyInput { pressKey(Key.DirectionRight) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("favorite-move").assertDoesNotExist()
        compose.onNodeWithTag("app:A").assertIsFocused()
        compose.runOnIdle { assertFalse(committed) }
    }

    @Test
    fun profileOrderPersistsAndStaleOrInactiveWritesAreRejected() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "reorder-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            val repo = DataStoreProfileRepository(store)
            repo.initialize(LauncherPreferences(), AppOrganization(listOf("A", "missing", "B")))
            val original = repo.snapshots.first()!!.active!!
            val order = listOf("B", "missing", "A")
            repo.reorderFavorites(original.id, original.favorites, order)
            assertEquals(
                order,
                DataStoreProfileRepository(store).snapshots.first()!!.active!!.favorites,
            )
            try {
                repo.reorderFavorites(original.id, original.favorites, original.favorites)
                fail("Stale preview overwrote layout")
            } catch (_: IOException) {}
            try {
                repo.reorderFavorites(original.id, order, listOf("A", "B"))
                fail("Missing favorite discarded")
            } catch (_: IOException) {}
            repo.create("Second", AvatarIds.DEFAULT)
            try {
                repo.reorderFavorites(original.id, order, original.favorites)
                fail("Inactive preview committed")
            } catch (_: IOException) {}
            assertTrue(repo.snapshots.first()!!.active!!.favorites.isEmpty())
            repo.select(original.id)
            assertEquals(order, repo.snapshots.first()!!.active!!.favorites)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
