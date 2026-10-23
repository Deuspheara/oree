package fr.deuspheara.oree.core.model

// Storage/import bound stays compatible with older profiles and folder membership.
const val MAX_FAVORITE_APPS = 500
const val MAX_DOCK_FAVORITES = 6

/** Ordered component identities only. Never stores labels, artwork or an Activity. */
fun setFavoriteApp(current: List<String>, id: String, favorite: Boolean): List<String> {
    require(id.isNotBlank() && id.length <= 512 && id.none { it.isISOControl() })
    val unique = current.distinct()
    if (!favorite) return unique.filterNot { it == id }
    if (id in unique) return unique
    require(unique.size < MAX_DOCK_FAVORITES) { "Favorite limit reached" }
    return unique + id
}

/** Missing apps stay in storage so a temporary package loss does not erase layout. */
fun orderFavoriteApps(apps: List<TvApp>, favorites: List<String>): List<TvApp> {
    val byId = apps.associateBy { it.id }
    return favorites.distinct().mapNotNull(byId::get)
}

/** Move among visible neighbors without erasing or shifting temporarily missing IDs. */
fun moveFavoriteApp(current: List<String>, id: String, direction: Int, available: Set<String>): List<String> {
    require(direction != 0 && direction in -MAX_FAVORITE_APPS..MAX_FAVORITE_APPS)
    val visible = current.filter { it in available }.toMutableList()
    val index = visible.indexOf(id)
    if (index < 0) return current
    visible.add((index + direction).coerceIn(0, visible.lastIndex), visible.removeAt(index))
    val next = visible.iterator()
    return current.map { if (it in available) next.next() else it }
}
