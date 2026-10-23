package fr.deuspheara.oree.core.domain.source

import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentFeed
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.RecentContent
import kotlinx.coroutines.flow.Flow

interface ContentSource {
    val id: ContentSourceId
    val changes: Flow<Unit>

    suspend fun loadSnapshot(): ContentFeed

    suspend fun loadFeed(enabled: Boolean, packageName: String? = null): ContentFeed {
        if (!enabled) return ContentFeed(ContentAvailability.DISABLED)
        return loadSnapshot().forPackage(packageName, 20)
    }

    suspend fun load(enabled: Boolean, packageName: String? = null): RecentContent {
        val feed = loadFeed(enabled, packageName)
        val entry = feed.entries.firstOrNull() ?: return RecentContent(feed.availability)
        return RecentContent(
            if (entry.artwork != null) {
                ContentAvailability.AVAILABLE
            } else {
                ContentAvailability.NO_LOCAL_ART
            },
            entry.artwork,
            entry.title,
            entry.source,
            entry.engagedAt,
        )
    }
}

fun ContentFeed.forPackage(packageName: String?, limit: Int): ContentFeed {
    val selected = entries.filter { packageName == null || it.source == packageName }.take(limit)
    return copy(
        entries = selected,
        availability =
            if (selected.isNotEmpty()) {
                availability
            } else if (availability == ContentAvailability.AVAILABLE) {
                ContentAvailability.EMPTY
            } else {
                availability
            },
    )
}

data class ContentSelection(
    val feed: ContentFeed = ContentFeed(ContentAvailability.DISABLED),
    val wallpaper: RecentContent = RecentContent(ContentAvailability.DISABLED),
    val homeEntries: List<fr.deuspheara.oree.core.model.ContentEntry> = feed.entries,
)
