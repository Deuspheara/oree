package fr.deuspheara.oree

import android.content.ComponentName
import android.content.Intent
import android.database.MatrixCursor
import android.net.Uri
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.platform.datasource.content.PreviewProgramsContentSource
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class AppPreviewsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun previewRowsUseRealDestinationsAndNeverInventEngagement() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val link =
            Intent(Intent.ACTION_VIEW, Uri.parse("fixture://content/1"))
                .setComponent(ComponentName(context, MainActivity::class.java))
                .toUri(Intent.URI_INTENT_SCHEME)
        val source =
            PreviewProgramsContentSource(context) { fields, _ ->
                MatrixCursor(fields).apply {
                    repeat(250) { index ->
                        addRow(
                            arrayOf<Any>(
                                index,
                                "Item $index",
                                context.packageName,
                                "https://example.org/poster.jpg",
                                link,
                                index,
                            ),
                        )
                    }
                }
            }
        val feed = source.loadFeed(true)
        assertEquals(ContentAvailability.AVAILABLE, feed.availability)
        assertEquals(20, feed.entries.size)
        assertTrue(feed.entries.first().id.endsWith(":199"))
        assertTrue(
            feed.entries.all {
                it.engagedAt == null && it.artwork == null && it.provider == ContentSourceId.PREVIEW
            },
        )
        assertEquals(ContentAvailability.DISABLED, source.loadFeed(false).availability)
        assertEquals(
            ContentAvailability.DENIED,
            PreviewProgramsContentSource(context) { _, _ ->
                throw SecurityException()
            }
                .loadFeed(true)
                .availability,
        )
    }

    @Test
    fun settledAppPreviewsRetainNativeDockFocusAndExactReturn() {
        val apps =
            listOf("alpha", "bravo", "charlie").map {
                TvApp("org.example.$it/.TV", "org.example.$it", it, 0)
            }
        val entries =
            apps.take(2).map {
                ContentEntry(
                    it.packageName,
                    it.label,
                    it.packageName,
                    null,
                    "fixture://content",
                    1L,
                )
            }
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
                    favorites = apps.take(2).map { it.id },
                    contentEntries = entries,
                    appPreviewsEnabled = true,
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[0].id}").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionRight)
        }
        compose.mainClock.advanceTimeBy(320)
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[1].id}").assertIsFocused().performKeyInput {
            pressKey(Key.DirectionDown)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("content:${apps[1].packageName}").assertIsFocused()
        compose.onNodeWithTag("content:${apps[0].packageName}").assertDoesNotExist()
        compose.onNodeWithTag("content:${apps[1].packageName}").performKeyInput {
            pressKey(Key.DirectionUp)
        }
        compose.onNodeWithTag("app:${apps[1].id}").assertIsFocused()
        compose.onNodeWithTag("app:${apps[1].id}").performKeyInput { pressKey(Key.DirectionDown) }
        InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithTag("app:${apps[1].id}").assertIsFocused()
    }
}
