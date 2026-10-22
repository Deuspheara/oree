package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImportWallpaperUseCase(private val profiles: ProfileRepository, private val images: WallpaperRepository) {
    suspend operator fun invoke(profileId: String, uri: String) {
        val previous =
            profiles.readSnapshot()?.profiles?.firstOrNull { it.id == profileId }?.image
        val imported = images.importImage(uri)
        var committed = false
        try {
            profiles.setImage(profileId, imported)
            committed = true
        } finally {
            // Cancellation may race an atomic commit. Never delete a referenced image.
            withContext(NonCancellable) {
                if (!committed && !profiles.referencesImage(imported)) images.discard(imported)
            }
        }
        if (previous != null && !profiles.referencesImage(previous)) images.discard(previous)
    }
}
