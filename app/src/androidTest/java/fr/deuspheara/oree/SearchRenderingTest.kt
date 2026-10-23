package fr.deuspheara.oree

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.presentation.screen.SearchPanel
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchRenderingTest {
    @get:Rule val compose = createComposeRule()

    private fun awaitRetainedBackdrop() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("retained-menu-image").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun retainedBackdropKeepsQueryResultsAndFocusLiveAcrossSourceChanges() {
        val alpha = SearchItem.App(TvApp("alpha", "fixture", "Alpha", 0))
        val beta = SearchItem.App(TvApp("beta", "fixture", "Beta", 0))
        var query by mutableStateOf("")
        var revision by mutableIntStateOf(0)
        var selected: SearchItem? = null
        compose.setContent {
            HazeTestTheme(LauncherPreferences()) {
                SearchPanel(
                    query = query,
                    onQuery = { query = it },
                    resume = 0,
                    reduced = true,
                    onSelect = { selected = it },
                    onAppMenu = { _, _, _ -> },
                    onClose = {},
                    resultState = listOf(alpha, beta).filter { it.title.contains(query, ignoreCase = true) },
                    sourceKey = revision,
                )
            }
        }
        awaitRetainedBackdrop()
        compose.onNodeWithTag("search-query").assertIsFocused().performTextInput("Beta")
        compose.onNodeWithTag("search-result:${alpha.id}").assertDoesNotExist()
        compose.onNodeWithTag("search-result:${beta.id}").assertExists()
        compose.onNodeWithTag("search-query").performKeyInput { pressKey(Key.DirectionDown) }
        compose.onNodeWithTag("search-result:${beta.id}").assertIsFocused()
        compose.runOnIdle { revision++ }
        awaitRetainedBackdrop()
        compose.onNodeWithTag("search-query").assertTextEquals("Beta")
        compose.onNodeWithTag("search-result:${beta.id}").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionCenter)
        }
        compose.runOnIdle { assertEquals(beta, selected) }
    }

    @Test
    fun nearbyOffscreenArtworkPrefetchIsBoundedAndDoesNotMoveQueryFocus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalLoader = SingletonImageLoader.get(context)
        val requested = ConcurrentHashMap.newKeySet<String>()
        val apps = (0..39).map { TvApp("fixture-$it", "search-$it", "Result $it", 1) }
        val loader = ImageLoader.Builder(context).components {
            add(
                Interceptor { chain ->
                    requested.add(chain.request.data.toString())
                    ErrorResult(image = null, request = chain.request, throwable = IOException("Synthetic unavailable artwork"))
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            compose.setContent {
                HazeTestTheme(LauncherPreferences()) {
                    SearchPanel("", {}, 0, true, {}, { _, _, _ -> }, {}, apps.map { SearchItem.App(it) })
                }
            }
            var prefetched: TvApp? = null
            compose.waitUntil(10_000) {
                prefetched = apps.firstOrNull { app ->
                    "android.resource://${app.packageName}/${app.artwork}" in requested &&
                        !compose.onNodeWithTag("search-result:app:${app.id}").isDisplayed()
                }
                prefetched != null
            }
            compose.onNodeWithTag("search-result:app:${prefetched!!.id}").assertIsNotDisplayed()
            compose.onNodeWithTag("search-query").assertIsFocused()
            assertTrue("Prefetch must not request the whole result set", requested.size < apps.size)
        } finally {
            SingletonImageLoader.setUnsafe(originalLoader)
            loader.shutdown()
        }
    }
}
