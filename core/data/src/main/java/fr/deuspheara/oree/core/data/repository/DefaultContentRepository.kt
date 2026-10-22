package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.repository.ContentRepository
import fr.deuspheara.oree.core.domain.source.ContentSelection
import fr.deuspheara.oree.core.domain.source.ContentSource
import fr.deuspheara.oree.core.domain.source.forPackage
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentFeed
import fr.deuspheara.oree.core.model.RecentContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take

/**
 * Ref-counted source snapshots: one observer/query pipeline per enabled source, no stop grace
 * period.
 */
class DefaultContentRepository(
    private val watchNext: ContentSource,
    private val preview: ContentSource,
    private val scope: CoroutineScope,
) : ContentRepository {
    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun refresh() {
        refreshes.tryEmit(Unit)
    }

    private fun observe(source: ContentSource): kotlinx.coroutines.flow.SharedFlow<ContentFeed> {
        var cached = ContentFeed(ContentAvailability.LOADING)
        return merge(source.changes, refreshes)
            .conflate()
            .mapLatest {
                val next = source.loadSnapshot()
                val result =
                    if (
                        next.availability in
                        setOf(
                            ContentAvailability.ERROR,
                            ContentAvailability.DENIED,
                            ContentAvailability.UNSUPPORTED,
                        )
                    ) {
                        next.copy(entries = cached.entries)
                    } else {
                        next
                    }
                cached = result
                result
            }
            .shareIn(scope, SharingStarted.WhileSubscribed(0), replay = 1)
    }

    private val recent = observe(watchNext)
    private val previews = observe(preview)

    override fun observe(
        watchNextFeed: Boolean,
        appPreviews: Boolean,
        wallpaper: Boolean,
        packageName: Flow<String?>,
    ): Flow<ContentSelection> {
        val disabled = ContentFeed(ContentAvailability.DISABLED)
        val recentFlow =
            if (watchNextFeed || wallpaper) {
                recent.onStart { emit(ContentFeed(ContentAvailability.LOADING)) }
            } else {
                flowOf(disabled)
            }
        val previewFlow =
            if (appPreviews) {
                previews.onStart { emit(ContentFeed(ContentAvailability.LOADING)) }
            } else {
                flowOf(disabled)
            }
        return combine(recentFlow, previewFlow, packageName) { next, programs, selectedPackage ->
            val feeds = buildMap {
                if (watchNextFeed) put(watchNext.id, next.forPackage(null, 20))
                if (appPreviews) put(preview.id, programs.forPackage(null, 20))
            }
            val entries = feeds.values.flatMap { it.entries }.distinctBy { it.id }.take(40)
            val availability =
                if (entries.isNotEmpty()) {
                    ContentAvailability.AVAILABLE
                } else {
                    listOf(
                        ContentAvailability.LOADING,
                        ContentAvailability.DENIED,
                        ContentAvailability.ERROR,
                        ContentAvailability.UNSUPPORTED,
                        ContentAvailability.STALE,
                    )
                        .firstOrNull { status ->
                            feeds.values.any { it.availability == status }
                        }
                        ?: if (feeds.isEmpty()) {
                            ContentAvailability.DISABLED
                        } else {
                            ContentAvailability.EMPTY
                        }
                }
            val background =
                if (wallpaper) {
                    val filtered = next.forPackage(selectedPackage, 1)
                    filtered.entries.firstOrNull()?.let { entry ->
                        RecentContent(
                            if (next.availability in
                                setOf(ContentAvailability.ERROR, ContentAvailability.DENIED, ContentAvailability.UNSUPPORTED)
                            ) {
                                next.availability
                            } else if (entry.artwork != null) {
                                ContentAvailability.AVAILABLE
                            } else {
                                ContentAvailability.NO_LOCAL_ART
                            },
                            entry.artwork,
                            entry.title,
                            entry.source,
                            entry.engagedAt,
                        )
                    } ?: RecentContent(filtered.availability)
                } else {
                    RecentContent(ContentAvailability.DISABLED)
                }
            ContentSelection(
                ContentFeed(availability, entries, feeds.mapValues { it.value.availability }),
                background,
            )
        }
            .distinctUntilChanged()
    }

    // Explicit one-shot API for existing provider fixtures; production uses observe().
    suspend fun load(enabled: Boolean, previews: Boolean): ContentFeed {
        if (!enabled) return ContentFeed(ContentAvailability.DISABLED)
        val feeds =
            listOf(watchNext.loadFeed(true)) +
                if (previews) listOf(preview.loadFeed(true)) else emptyList()
        val entries = feeds.flatMap { it.entries }.distinctBy { it.id }.take(40)
        return ContentFeed(
            if (entries.isNotEmpty()) ContentAvailability.AVAILABLE else feeds.first().availability,
            entries,
        )
    }
}
