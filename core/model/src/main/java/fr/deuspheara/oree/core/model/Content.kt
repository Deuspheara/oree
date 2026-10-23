package fr.deuspheara.oree.core.model

enum class ContentAvailability {
    LOADING,
    DISABLED,
    AVAILABLE,
    DENIED,
    UNSUPPORTED,
    EMPTY,
    STALE,
    NO_LOCAL_ART,
    ERROR,
}

data class RecentContent(
    val availability: ContentAvailability,
    val artwork: String? = null,
    val title: String? = null,
    val source: String? = null,
    val engagedAt: Long? = null,
)

enum class ContinuationKind { CONTINUE, NEXT }

data class ContentEntry(
    val id: String,
    val title: String,
    val source: String,
    val artwork: String?,
    val intentUri: String,
    val engagedAt: Long?,
    val provider: ContentSourceId = ContentSourceId.WATCH_NEXT,
    val episodeTitle: String? = null,
    val episodeNumber: String? = null,
    val seasonNumber: String? = null,
    val durationMillis: Long? = null,
    val playbackPositionMillis: Long? = null,
    val continuationKind: ContinuationKind? = null,
) {
    val playbackProgress: Float?
        get() {
            val duration = durationMillis?.takeIf { it > 0 } ?: return null
            val position = playbackPositionMillis?.takeIf { it >= 0 && it <= duration } ?: return null
            return position.toFloat() / duration
        }
}

data class ContentFeed(
    val availability: ContentAvailability,
    val entries: List<ContentEntry> = emptyList(),
    val sources: Map<ContentSourceId, ContentAvailability> = emptyMap(),
)

data class LocalPhoto(val uri: String, val name: String)

enum class PhotoAvailability {
    AVAILABLE,
    EMPTY,
    DENIED,
    ERROR,
}

data class LocalPhotos(val availability: PhotoAvailability, val photos: List<LocalPhoto> = emptyList())

enum class ContentSourceId {
    CHANNELS,
    PREVIEW,
    WATCH_NEXT,
    PROGRAMS,
}

/** One useful continuation per publisher; genuine progress precedes upcoming/promotional entries. */
fun continuationPerApp(entries: List<ContentEntry>): List<ContentEntry> = entries
    .sortedWith(
        compareByDescending<ContentEntry> { it.provider == ContentSourceId.WATCH_NEXT }
            .thenByDescending {
                when {
                    it.playbackProgress?.let { progress -> progress > 0f && progress < .98f } == true -> 2
                    it.continuationKind == ContinuationKind.CONTINUE && (it.playbackProgress ?: 0f) < .98f -> 1
                    else -> 0
                }
            }
            .thenByDescending { it.engagedAt ?: 0L },
    )
    .distinctBy { it.source }
    .sortedByDescending { it.engagedAt ?: 0L }
