package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.repository.DataStoreProfileRepository
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.AvatarIds
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.FolderPanel
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import fr.deuspheara.oree.feature.search.domain.SearchAction
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.presentation.screen.SearchPanel
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
class FolderSearchTest {
    @get:Rule val compose = createComposeRule()
    private val apps =
        listOf("Alpha", "Bravo", "Charlie").map {
            TvApp("org.example.${it.lowercase()}/.TV", "org.example.${it.lowercase()}", it, 0)
        }
    private val folder = AppFolder("fixture", "Cinema", apps.take(2).map { it.id })

    @Test
    fun homeFolderEntryBackAndChildRemovalRestoreLogicalFocus() {
        val catalog = mutableStateOf(apps)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var opened by remember { mutableStateOf(false) }
                var resume by remember { mutableIntStateOf(0) }
                HomeScreen(
                    HomeUiState(catalog.value, false),
                    resume,
                    true,
                    {},
                    {},
                    {},
                    {},
                    folders = listOf(folder),
                    onFolder = { opened = true },
                )
                if (opened) {
                    FolderPanel(
                        folder,
                        catalog.value,
                        resume,
                        true,
                        false,
                        false,
                        null,
                        {},
                        { _, _, _ -> },
                        {},
                        {},
                        onClose = {
                            opened = false
                            resume++
                        },
                        onReorder = { _, _ -> },
                        onCancelMove = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("folder:fixture").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[0].id}").assertIsFocused()
        compose.runOnIdle { catalog.value = apps.drop(1) }
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[1].id}").assertIsFocused()
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("folder-panel").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("folder-panel").assertDoesNotExist()
        compose.onNodeWithTag("folder:fixture").assertIsFocused()
    }

    @Test
    fun folderReorderBackCancelsAndEmptyFolderRetainsAnAction() {
        val catalog = mutableStateOf(apps)
        var committed = false
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var moving by remember { mutableStateOf<String?>(apps[0].id) }
                FolderPanel(
                    folder,
                    catalog.value,
                    0,
                    true,
                    false,
                    false,
                    moving,
                    {},
                    { _, _, _ -> },
                    {},
                    {},
                    {},
                    onReorder = { _, _ -> committed = true },
                    onCancelMove = { moving = null },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[0].id}").performKeyInput { pressKey(Key.DirectionRight) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(committed)
            catalog.value = emptyList()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("folder-rename").assertIsFocused()
        compose.onNodeWithTag("folder-close").assertExists()
    }

    @Test
    fun searchUsesDpadAndReplacesRemovedResultsWithoutLosingInput() {
        val catalog = mutableStateOf(apps)
        var opened: SearchItem? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                var query by remember { mutableStateOf("") }
                SearchPanel(
                    query,
                    { query = it },
                    0,
                    true,
                    { opened = it },
                    { _, _, _ -> },
                    {},
                    resultState = fr.deuspheara.oree.feature.search.domain.searchLocal(
                        query,
                        catalog.value,
                        listOf(folder),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").assertIsFocused().performTextInput("bravo")
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("search-result:app:${apps[1].id}").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals(SearchItem.App(apps[1]), opened) }
        compose.onNodeWithTag("search-result:app:${apps[1].id}").performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("search-query").assertIsFocused()
        compose.runOnIdle { catalog.value = apps.filterNot { it.id == apps[1].id } }
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").assertIsFocused()
        compose.onNodeWithTag("search-result:app:${apps[1].id}").assertDoesNotExist()
    }

    @Test
    fun profileFoldersMigratePersistAndRejectCorruptionAtomically() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "folders-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val key = stringPreferencesKey("profiles_v1")
        try {
            val repo = DataStoreProfileRepository(store)
            repo.initialize(LauncherPreferences(), AppOrganization(apps.map { it.id }))
            val first = repo.snapshots.first()!!.activeId
            store.edit { values ->
                val json = org.json.JSONObject(values[key]!!).put("version", 2)
                json.getJSONArray("profiles").getJSONObject(0).remove("folders")
                values[key] = json.toString()
            }
            assertTrue(repo.snapshots.first()!!.active!!.folders.isEmpty())
            repo.createFolder(first, "Cinema", apps[0].id)
            val folderId = repo.snapshots.first()!!.active!!.folders.single().id
            assertFalse(apps[0].id in repo.snapshots.first()!!.active!!.favorites)
            repo.moveToFolder(first, apps[1].id, folderId)
            val order = apps.take(2).map { it.id }
            repo.reorderFolder(first, folderId, order, order.reversed())
            repo.renameFolder(first, folderId, "Films")
            assertEquals(
                order.reversed(),
                DataStoreProfileRepository(store)
                    .snapshots
                    .first()!!
                    .active!!
                    .folders
                    .single()
                    .apps,
            )
            repo.create("Second", AvatarIds.DEFAULT)
            assertTrue(repo.snapshots.first()!!.active!!.folders.isEmpty())
            repo.select(first)
            repo.moveToFolder(first, apps[0].id, null)
            assertEquals(
                listOf(apps[1].id),
                repo.snapshots.first()!!.active!!.folders.single().apps,
            )
            val before = store.data.first()[key]
            try {
                repo.moveToFolder(first, "file:///outside", folderId)
                fail("Invalid component accepted")
            } catch (_: IOException) {}
            assertEquals(before, store.data.first()[key])
            repo.deleteFolder(first, folderId)
            assertTrue(repo.snapshots.first()!!.active!!.folders.isEmpty())
            assertEquals(listOf(apps[2].id), repo.snapshots.first()!!.active!!.favorites)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }

    @Test
    fun searchRestoresSelectedResultAfterVisitingAnotherScreen() {
        val shown = mutableStateOf(true)
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
                var query by
                    androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
                if (shown.value) {
                    holder.SaveableStateProvider("search") {
                        SearchPanel(
                            query,
                            { query = it },
                            0,
                            true,
                            { shown.value = false },
                            { _, _, _ -> },
                            {},
                            resultState = fr.deuspheara.oree.feature.search.domain.searchLocal(
                                query,
                                emptyList(),
                                emptyList(),
                                listOf(SearchItem.Action(SearchAction.PRIVACY, "Privacy")),
                                emptyList(),
                                emptyList(),
                                emptyList(),
                            ),
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").performTextInput("privacy")
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("search-result:action:PRIVACY").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.onNodeWithTag("search-query").assertDoesNotExist()
        compose.runOnIdle { shown.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("search-query").assertTextEquals("privacy")
        compose.onNodeWithTag("search-result:action:PRIVACY").assertIsFocused()
    }
}
