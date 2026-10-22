package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.WallpaperMode
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val snapshots: Flow<ProfileSnapshot?>
    val canUndoRestore: Flow<Boolean>

    /** Committed data for ordered workflows; observation snapshots may briefly lag a write. */
    suspend fun readSnapshot(): ProfileSnapshot?

    suspend fun initialize(preferences: LauncherPreferences, organization: AppOrganization)

    suspend fun previousRestore(): ProfileSnapshot

    suspend fun referencesImage(name: String): Boolean

    suspend fun restoreSnapshot(expected: ProfileSnapshot, replacement: ProfileSnapshot, undo: Boolean = false)

    suspend fun setPreferences(id: String, value: LauncherPreferences)

    suspend fun setWallpaper(id: String, mode: WallpaperMode)

    suspend fun setLandscape(id: String, landscape: Int)

    suspend fun setImage(id: String, name: String)

    suspend fun setContentEnabled(id: String, enabled: Boolean)

    suspend fun setWatchNextEnabled(id: String, enabled: Boolean)

    suspend fun setAppPreviewsEnabled(id: String, enabled: Boolean)

    suspend fun setAmbientPhotos(id: String, enabled: Boolean)

    suspend fun setAmbientClock(id: String, enabled: Boolean)

    suspend fun select(id: String)

    suspend fun create(name: String, avatar: String)

    suspend fun rename(id: String, name: String, avatar: String)

    suspend fun delete(id: String)
}
