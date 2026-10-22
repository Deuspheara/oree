package fr.deuspheara.oree.di

import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.WallpaperMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

internal class MemoryProfileFixture :
    ProfileRepository,
    OrganizationRepository {
    override val snapshots =
        MutableStateFlow<ProfileSnapshot?>(
            ProfileSnapshot("fixture", listOf(LocalProfile("fixture", "Fixture"))),
        )
    override suspend fun readSnapshot() = snapshots.value

    override val canUndoRestore = flowOf(false)

    override suspend fun initialize(preferences: LauncherPreferences, organization: AppOrganization) = Unit

    override suspend fun previousRestore(): ProfileSnapshot = snapshots.value!!

    override suspend fun referencesImage(name: String): Boolean = false

    override suspend fun restoreSnapshot(expected: ProfileSnapshot, replacement: ProfileSnapshot, undo: Boolean) =
        error("Operation is outside this construction fixture")

    override suspend fun setPreferences(id: String, value: LauncherPreferences) = error("Operation is outside this construction fixture")

    override suspend fun setWallpaper(id: String, mode: WallpaperMode) = error("Operation is outside this construction fixture")

    override suspend fun setLandscape(id: String, landscape: Int) = error("Operation is outside this construction fixture")

    override suspend fun setImage(id: String, name: String) = error("Operation is outside this construction fixture")

    override suspend fun setContentEnabled(id: String, enabled: Boolean) = error("Operation is outside this construction fixture")

    override suspend fun setWatchNextEnabled(id: String, enabled: Boolean) = error("Operation is outside this construction fixture")

    override suspend fun setAppPreviewsEnabled(id: String, enabled: Boolean) = error("Operation is outside this construction fixture")

    override suspend fun setAmbientPhotos(id: String, enabled: Boolean) = error("Operation is outside this construction fixture")

    override suspend fun setAmbientClock(id: String, enabled: Boolean) = error("Operation is outside this construction fixture")

    override suspend fun select(id: String) = error("Operation is outside this construction fixture")

    override suspend fun create(name: String, avatar: String) = error("Operation is outside this construction fixture")

    override suspend fun rename(id: String, name: String, avatar: String) = error("Operation is outside this construction fixture")

    override suspend fun delete(id: String) = error("Operation is outside this construction fixture")

    override suspend fun setFavorite(id: String, component: String, favorite: Boolean) =
        error("Operation is outside this construction fixture")

    override suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>) =
        error("Operation is outside this construction fixture")

    override suspend fun createFolder(profileId: String, name: String, appId: String) =
        error("Operation is outside this construction fixture")

    override suspend fun renameFolder(profileId: String, folderId: String, name: String) =
        error("Operation is outside this construction fixture")

    override suspend fun deleteFolder(profileId: String, folderId: String) = error("Operation is outside this construction fixture")

    override suspend fun moveToFolder(profileId: String, appId: String, folderId: String?) =
        error("Operation is outside this construction fixture")

    override suspend fun reorderFolder(profileId: String, folderId: String, expected: List<String>, order: List<String>) =
        error("Operation is outside this construction fixture")
}
