package fr.deuspheara.oree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.AppTile
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot
import fr.deuspheara.oree.feature.home.presentation.state.HomeNavigationMotion
import fr.deuspheara.oree.feature.home.presentation.state.homeArtworkSize
import fr.deuspheara.oree.feature.home.presentation.state.homeGridArtworkSize
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class HomeArtworkPrefetchTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun adaptiveRequestMatchesMeasuredGridCellsAcrossColumnChanges() {
        val availableWidth = mutableIntStateOf(400)
        val measured = ConcurrentHashMap<Int, IntSize>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.width(availableWidth.intValue.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(220.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(19.dp),
                    ) {
                        items(12) { index ->
                            Box(Modifier.aspectRatio(16f / 9f).onSizeChanged { measured[index] = it })
                        }
                    }
                }
            }
        }
        for (width in listOf(485, 458, 459, 800)) {
            compose.runOnIdle {
                measured.clear()
                availableWidth.intValue = width
            }
            compose.waitUntil(5_000) { measured.isNotEmpty() }
            compose.waitForIdle()
            val widest = measured.values.maxBy { it.width }
            assertEquals(homeArtworkSize(widest), homeGridArtworkSize(width, 220, 19))
        }
    }

    @Test
    fun homePreparesNearbyCatalogArtworkWhileFocusStaysInDock() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalLoader = SingletonImageLoader.get(context)
        val requests = ConcurrentHashMap.newKeySet<String>()
        val loader = ImageLoader.Builder(context).components {
            add(
                Interceptor { chain ->
                    requests.add(chain.request.data.toString())
                    ErrorResult(image = null, request = chain.request, throwable = IOException("Synthetic unavailable artwork"))
                },
            )
        }.build()
        val favorite = TvApp("favorite", "favorite", "Favorite", 1)
        val catalog = listOf(favorite) + (0..29).map { TvApp("grid-$it", "grid-$it", "Card $it", 1) }
        SingletonImageLoader.setUnsafe(loader)
        try {
            compose.setContent {
                HazeTestTheme(LauncherPreferences()) {
                    HomeScreen(
                        state = HomeUiState(catalog, false),
                        resume = 0,
                        reduceMotion = true,
                        favorites = listOf(favorite.id),
                        contentEntries = (0..5).map {
                            ContentEntry(
                                id = "preview-$it", title = "Preview $it", source = "fixture",
                                artwork = null, intentUri = "fixture", engagedAt = null,
                                episodeTitle = "Episode", durationMillis = 100, playbackPositionMillis = 50,
                            )
                        },
                        onAction = {},
                    )
                }
            }
            compose.waitUntil(10_000) { requests.any { it.startsWith("android.resource://grid-") } }
            compose.onNodeWithTag("app:favorite").assertIsFocused()
            compose.onNodeWithTag("app:grid-0").assertIsNotDisplayed()
        } finally {
            SingletonImageLoader.setUnsafe(originalLoader)
            loader.shutdown()
        }
    }

    @Test
    fun preparedCardStartsArtworkBeforeItsFirstMeasurement() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val originalLoader = SingletonImageLoader.get(context)
        val requested = AtomicReference<ImageRequest?>()
        val requester = FocusRequester()
        val navigation = HomeNavigationMotion()
        val loader = ImageLoader.Builder(context).components {
            add(
                Interceptor { chain ->
                    requested.set(chain.request)
                    ErrorResult(image = null, request = chain.request, throwable = IOException("Synthetic unavailable artwork"))
                },
            )
        }.build()
        SingletonImageLoader.setUnsafe(loader)
        try {
            compose.setContent {
                // Precompose a card without measuring or placing it, as lazy prefetch can do.
                Layout(content = {
                    AppTile(
                        app = TvApp("fixture", "fixture", "Fixture", artwork = 1),
                        requester = requester,
                        reduceMotion = false,
                        slot = HomeGridSlot(0, true, true),
                        navigation = navigation,
                        onFocus = {},
                        onClick = {},
                        onMenu = null,
                        preparedArtworkSize = IntSize(320, 180),
                    )
                }) { _, _ -> layout(480, 270) {} }
            }
            compose.waitUntil(5_000) { requested.get() != null }
            assertFalse("Palette sampling must not need a GPU readback", requested.get()!!.allowHardware)
            assertEquals(coil3.size.Size(320, 180), runBlocking { requested.get()!!.sizeResolver.size() })
        } finally {
            SingletonImageLoader.setUnsafe(originalLoader)
            loader.shutdown()
        }
    }
}
