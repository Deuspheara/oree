package fr.deuspheara.oree.core.domain.rendering

import fr.deuspheara.oree.core.model.ReflectionColors

data class ReflectionKey(val artwork: String, val width: Int, val height: Int, val crop: Boolean)

fun interface ArtworkPalette {
    fun colors(key: ReflectionKey): ReflectionColors?

    /** Optional decoration may finish after the image; cancelling a waiter never cancels preparation. */
    suspend fun awaitColors(key: ReflectionKey): ReflectionColors? = colors(key)
}
