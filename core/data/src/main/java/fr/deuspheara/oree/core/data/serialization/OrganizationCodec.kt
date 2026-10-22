package fr.deuspheara.oree.core.data.serialization

import fr.deuspheara.oree.core.model.MAX_FAVORITE_APPS
import java.io.IOException
import org.json.JSONArray

fun decodeFavoriteApps(value: String?): List<String> {
    if (value == null) return emptyList()
    try {
        if (value.length > 300_000) throw IOException("Favorite data too large")
        val array = JSONArray(value)
        if (array.length() > MAX_FAVORITE_APPS) throw IOException("Too many favorites")
        return buildList {
            repeat(array.length()) { index ->
                val id =
                    array.get(index) as? String ?: throw IOException("Invalid component identity")
                if (id.isBlank() || id.length > 512 || id.any { it.isISOControl() }) {
                    throw IOException("Invalid component identity")
                }
                if (id !in this) add(id)
            }
        }
    } catch (error: org.json.JSONException) {
        throw IOException("Invalid favorite data", error)
    }
}
