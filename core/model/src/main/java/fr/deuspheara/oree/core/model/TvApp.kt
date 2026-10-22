package fr.deuspheara.oree.core.model

enum class ArtworkKind {
    BANNER,
    ICON,
}

// Resource IDs and component strings only; no Activity, Drawable or Bitmap in state.
data class TvApp(
    val id: String,
    val packageName: String,
    val label: String,
    val artwork: Int,
    val artworkVersion: Long = 0L,
    val artworkKind: ArtworkKind = ArtworkKind.BANNER,
) {
    val artworkCacheKey: String
        get() = "$packageName/$artwork@$artworkVersion"
}

fun restoreAppId(previous: String?, previousIndex: Int, apps: List<TvApp>): String? = apps.firstOrNull { it.id == previous }?.id
    ?: apps.getOrNull(previousIndex.coerceIn(0, (apps.size - 1).coerceAtLeast(0)))?.id
