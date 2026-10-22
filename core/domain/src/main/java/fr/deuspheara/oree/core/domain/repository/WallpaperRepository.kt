package fr.deuspheara.oree.core.domain.repository

interface WallpaperRepository {
    suspend fun importImage(uri: String): String

    suspend fun discard(name: String)
}
