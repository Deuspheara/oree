package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.repository.PhotoRepository
import fr.deuspheara.oree.core.domain.source.PhotoDataSource

class DefaultPhotoRepository(private val source: PhotoDataSource) : PhotoRepository {
    override suspend fun load() = source.load()
}
