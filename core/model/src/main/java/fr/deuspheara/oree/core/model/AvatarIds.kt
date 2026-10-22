package fr.deuspheara.oree.core.model

/** Durable identities: adding/reordering artwork never changes a saved profile. */
object AvatarIds {
    const val DEFAULT = "avatar_01"
    val all =
        listOf(
            "avatar_01",
            "avatar_02",
            "avatar_03",
            "avatar_04",
            "avatar_05",
            "avatar_06",
            "avatar_07",
            "avatar_08",
        )

    fun isKnown(id: String) = id in all

    fun fromLegacy(index: Int): String? = all.take(6).getOrNull(index)
}
