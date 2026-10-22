package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.LocalPhotos

interface PhotoRepository {
    suspend fun load(): LocalPhotos
}
