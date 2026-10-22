package fr.deuspheara.oree.core.domain.source

import fr.deuspheara.oree.core.model.LocalPhotos

interface PhotoDataSource {
    suspend fun load(): LocalPhotos
}
