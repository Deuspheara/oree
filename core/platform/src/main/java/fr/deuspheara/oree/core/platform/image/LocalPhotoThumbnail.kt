package fr.deuspheara.oree.core.platform.image

/** Separate cache identity: a gallery thumbnail must never satisfy a fullscreen photo request. */
data class LocalPhotoThumbnail(val uri: String) {
    val cacheKey: String get() = "local-photo-thumbnail:$uri"
}
