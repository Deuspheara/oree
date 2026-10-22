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

data class ContentEntry(
    val id: String,
    val title: String,
    val source: String,
    val artwork: String?,
    val intentUri: String,
    val engagedAt: Long?,
    val provider: ContentSourceId = ContentSourceId.WATCH_NEXT,
)

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
