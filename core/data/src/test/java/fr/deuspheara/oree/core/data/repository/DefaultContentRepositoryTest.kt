package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.source.ContentSource
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentFeed
import fr.deuspheara.oree.core.model.ContentSourceId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultContentRepositoryTest {
    private class Source(override val id: ContentSourceId) : ContentSource {
        var active = 0
        var loads = 0
        var feed = ContentFeed(ContentAvailability.EMPTY)
        val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val changes = flow {
            active++
            try {
                emit(Unit)
                invalidations.collect { emit(Unit) }
            } finally {
                active--
            }
        }

        override suspend fun loadSnapshot(): ContentFeed {
            loads++
            return feed
        }
    }

    @Test
    fun allSourceToggleCombinationsRegisterOnlyEnabledSourcesAndStopImmediately() = runTest {
        repeat(8) { mask ->
            val next = Source(ContentSourceId.WATCH_NEXT)
            val preview = Source(ContentSourceId.PREVIEW)
            val repository = DefaultContentRepository(next, preview, backgroundScope)
            val subscriber = backgroundScope.launch {
                repository
                    .observe(
                        mask and 1 != 0,
                        mask and 2 != 0,
                        mask and 4 != 0,
                        MutableStateFlow(null),
                    )
                    .collect()
            }
            runCurrent()
            assertEquals(if (mask and 5 != 0) 1 else 0, next.loads)
            assertEquals(if (mask and 2 != 0) 1 else 0, preview.loads)
            subscriber.cancel()
            runCurrent()
            assertEquals(0, next.active)
            assertEquals(0, preview.active)
        }
    }

    @Test
    fun concurrentConsumersShareQueryAndFocusFilteringPrecedesPresentationLimit() = runTest {
        val next =
            Source(ContentSourceId.WATCH_NEXT).apply {
                feed =
                    ContentFeed(
                        ContentAvailability.AVAILABLE,
                        (0..40).map { index ->
                            ContentEntry(
                                "$index",
                                "Title $index",
                                if (index == 40) "target" else "other",
                                "content://art/$index",
                                "",
                                1L,
                            )
                        },
                    )
            }
        val preview = Source(ContentSourceId.PREVIEW)
        val repository = DefaultContentRepository(next, preview, backgroundScope)
        val selected = MutableStateFlow<String?>("target")
        var artwork: String? = null
        val first = backgroundScope.launch {
            repository.observe(true, false, true, selected).collect {
                artwork = it.wallpaper.artwork
            }
        }
        val second = backgroundScope.launch {
            repository.observe(true, false, false, selected).collect()
        }
        runCurrent()
        assertEquals(1, next.loads)
        assertEquals("content://art/40", artwork)
        selected.value = "other"
        runCurrent()
        assertEquals("content://art/0", artwork)
        assertEquals(1, next.loads)
        first.cancel()
        runCurrent()
        assertEquals(1, next.active)
        second.cancel()
        runCurrent()
        assertEquals(0, next.active)
        val resumed = backgroundScope.launch {
            repository.observe(true, false, false, selected).collect()
        }
        runCurrent()
        assertEquals(2, next.loads)
        resumed.cancel()
    }

    @Test
    fun failedRefreshRetainsMetadataAndPublishesSourceFailure() = runTest {
        val next =
            Source(ContentSourceId.WATCH_NEXT).apply {
                feed =
                    ContentFeed(
                        ContentAvailability.AVAILABLE,
                        listOf(ContentEntry("one", "One", "source", null, "", 1L)),
                    )
            }
        val repository =
            DefaultContentRepository(next, Source(ContentSourceId.PREVIEW), backgroundScope)
        var last = ContentFeed(ContentAvailability.LOADING)
        var wallpaperStatus = ContentAvailability.LOADING
        val subscriber = backgroundScope.launch {
            repository.observe(true, false, true, MutableStateFlow(null)).collect {
                last = it.feed
                wallpaperStatus = it.wallpaper.availability
            }
        }
        runCurrent()
        next.feed = ContentFeed(ContentAvailability.ERROR)
        next.invalidations.emit(Unit)
        runCurrent()
        assertEquals("one", last.entries.single().id)
        assertEquals(ContentAvailability.ERROR, last.sources[ContentSourceId.WATCH_NEXT])
        assertEquals(ContentAvailability.ERROR, wallpaperStatus)
        subscriber.cancel()
        runCurrent()
        assertTrue(next.active == 0)
    }

    @Test
    fun explicitRetryRefreshesSharedSnapshotWithoutAddingAnObserver() = runTest {
        val source = Source(ContentSourceId.WATCH_NEXT)
        val repository = DefaultContentRepository(source, Source(ContentSourceId.PREVIEW), backgroundScope)
        val subscriber = backgroundScope.launch {
            repository.observe(true, false, true, MutableStateFlow(null)).collect()
        }
        runCurrent()
        assertEquals(1, source.loads)
        repository.refresh()
        runCurrent()
        assertEquals(2, source.loads)
        assertEquals(1, source.active)
        subscriber.cancel()
        runCurrent()
        repository.refresh()
        runCurrent()
        assertEquals(2, source.loads)
        assertEquals(0, source.active)
    }
}
