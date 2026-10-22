package fr.deuspheara.oree.core.model

enum class ProbeAvailability {
    AVAILABLE,
    EMPTY,
    DENIED,
    UNSUPPORTED,
    ERROR,
}

data class ContentCapability(
    val source: ContentSourceId,
    val availability: ProbeAvailability,
    val rows: Int = 0,
    val metadata: Int = 0,
    val localArtwork: Int = 0,
    val artworkReadable: Boolean? = null,
    val launchable: Int = 0,
    val crossPublisher: Int = 0,
    val bounded: Boolean = false,
)

data class ContentCapabilities(
    val providers: List<ContentCapability>,
    val listingsPermission: Boolean,
    val systemSearch: Boolean,
    val searchableCandidates: Int,
    val exportedSearchCandidates: Int,
    val searchableAvailability: ProbeAvailability,
)
