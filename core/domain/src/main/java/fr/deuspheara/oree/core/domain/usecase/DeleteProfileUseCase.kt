package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository

class DeleteProfileUseCase(private val profiles: ProfileRepository, private val images: WallpaperRepository) {
    suspend operator fun invoke(id: String) {
        val previous = profiles.readSnapshot()?.profiles?.firstOrNull { it.id == id }?.image
        profiles.delete(id)
        if (previous != null && !profiles.referencesImage(previous)) images.discard(previous)
    }
}
