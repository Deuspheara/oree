package fr.deuspheara.oree.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.deuspheara.oree.core.data.datastore.profileDataStore
import fr.deuspheara.oree.core.data.serialization.decodeProfiles
import fr.deuspheara.oree.core.data.serialization.encodeProfiles
import fr.deuspheara.oree.core.data.serialization.validName
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.MAX_APP_FOLDERS
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.model.setFavoriteApp
import fr.deuspheara.oree.core.model.validFolderComponent
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.none
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/** All profile edits are one versioned transaction. Legacy stores remain untouched. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DataStoreProfileRepository
constructor(
    private val store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val scope: kotlinx.coroutines.CoroutineScope? = null,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : ProfileRepository,
    OrganizationRepository {
    constructor(
        context: Context,
        scope: kotlinx.coroutines.CoroutineScope? = null,
    ) : this(profileDataStore(context), scope)

    private val key = stringPreferencesKey("profiles_v1")
    private val beforeRestoreKey = stringPreferencesKey("profiles_before_restore_v1")
    override val canUndoRestore =
        store.data
            .map { it[beforeRestoreKey] != null }
            .catch {
                if (it is IOException) emit(false) else throw it
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.io)
    private val observationEpoch = MutableStateFlow(0L)

    @Volatile private var observationFailed = false
    private val decodedSnapshots = store.data
        .map { data ->
            data[key]?.let(::decodeProfiles).also { observationFailed = false }
        }
        .catch {
            if (it is IOException || it is IllegalArgumentException) {
                observationFailed = true
                emit(ProfileSnapshot("", emptyList(), true))
            } else {
                throw it
            }
        }
    override val snapshots = observationEpoch.flatMapLatest { decodedSnapshots }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)
        .let { source ->
            if (scope == null) {
                source
            } else {
                source.shareIn(scope, SharingStarted.WhileSubscribed(0), replay = 1)
            }
        }

    override suspend fun readSnapshot(): ProfileSnapshot? = withContext(dispatchers.io) {
        val current = store.data.first()[key]?.let(::decodeProfiles)
        // A completed failed observation needs an explicit restart after a successful retry.
        if (observationFailed) observationEpoch.update { it + 1 }
        current
    }

    override suspend fun initialize(preferences: LauncherPreferences, organization: AppOrganization) = withContext(dispatchers.io) {
        store.edit { data ->
            if (data[key] == null) {
                if (organization.failed) throw IOException("Legacy favorites unavailable")
                data[key] =
                    encodeProfiles(
                        ProfileSnapshot(
                            "default",
                            listOf(
                                LocalProfile(
                                    "default",
                                    "Home",
                                    favorites = organization.favorites,
                                    preferences = preferences,
                                ),
                            ),
                        ),
                    )
            } else {
                decodeProfiles(data[key]!!)
            }
        }
        Unit
    }

    private suspend fun transaction(change: (ProfileSnapshot) -> ProfileSnapshot) = withContext(dispatchers.io) {
        store.edit { data ->
            val previous = data[key] ?: throw IOException("Profiles not initialized")
            data[key] = encodeProfiles(change(decodeProfiles(previous)))
        }
        Unit
    }

    override suspend fun previousRestore(): ProfileSnapshot = withContext(dispatchers.io) {
        decodeProfiles(
            store.data.first()[beforeRestoreKey] ?: throw IOException("No restore to undo"),
        )
    }

    /** Retain images used by either the current configuration or its one-step restore undo. */
    override suspend fun referencesImage(name: String): Boolean = withContext(dispatchers.io) {
        try {
            val data = store.data.first()
            listOfNotNull(data[key], data[beforeRestoreKey]).any { raw ->
                decodeProfiles(raw).profiles.any { it.image == name }
            }
        } catch (_: IOException) {
            true
        } // A failed read must never trigger destructive cleanup.
    }

    override suspend fun restoreSnapshot(expected: ProfileSnapshot, replacement: ProfileSnapshot, undo: Boolean) =
        withContext(dispatchers.io) {
            store.edit { data ->
                val previous = data[key] ?: throw IOException("Profiles unavailable")
                if (decodeProfiles(previous) != expected) {
                    throw IOException("Configuration changed during review")
                }
                val encoded = encodeProfiles(replacement)
                if (undo) {
                    if (data[beforeRestoreKey] == null) throw IOException("No restore to undo")
                    data.remove(beforeRestoreKey)
                } else {
                    data[beforeRestoreKey] = previous
                }
                data[key] = encoded
            }
            Unit
        }

    private suspend fun update(id: String, change: (LocalProfile) -> LocalProfile) = transaction { snapshot ->
        if (snapshot.profiles.none { it.id == id }) throw IOException("Profile removed")
        snapshot.copy(profiles = snapshot.profiles.map { if (it.id == id) change(it) else it })
    }

    override suspend fun setPreferences(id: String, value: LauncherPreferences) = update(id) { it.copy(preferences = value.normalized()) }

    override suspend fun setFavorite(id: String, component: String, favorite: Boolean) = update(id) {
        it.copy(
            favorites = setFavoriteApp(it.favorites, component, favorite),
            folders =
                if (favorite) {
                    it.folders.map { folder ->
                        folder.copy(apps = folder.apps - component)
                    }
                } else {
                    it.folders
                },
        )
    }

    override suspend fun setWallpaper(id: String, mode: WallpaperMode) = update(id) { it.copy(wallpaper = mode) }

    override suspend fun setLandscape(id: String, landscape: Int) = update(id) { it.copy(landscape = landscape) }

    override suspend fun setImage(id: String, name: String) = update(id) { it.copy(image = name, wallpaper = WallpaperMode.PERSONAL) }

    override suspend fun setContentEnabled(id: String, enabled: Boolean) = update(id) { it.copy(contentEnabled = enabled) }

    override suspend fun setWatchNextEnabled(id: String, enabled: Boolean) = update(id) { it.copy(watchNextEnabled = enabled) }

    override suspend fun setAppPreviewsEnabled(id: String, enabled: Boolean) = update(id) { it.copy(appPreviewsEnabled = enabled) }

    override suspend fun setAmbientPhotos(id: String, enabled: Boolean) = update(id) { it.copy(ambientPhotos = enabled) }

    override suspend fun setAmbientClock(id: String, enabled: Boolean) = update(id) { it.copy(ambientClock = enabled) }

    override suspend fun select(id: String) = transaction {
        if (it.profiles.none { p -> p.id == id }) throw IOException("Unknown profile")
        it.copy(activeId = id)
    }

    override suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>) = transaction { snapshot ->
        val profile =
            snapshot.profiles.firstOrNull { it.id == id }
                ?: throw IOException("Profile removed")
        if (snapshot.activeId != id || profile.favorites != expected) {
            throw IOException("Favorite layout changed")
        }
        if (
            order.size != expected.size ||
            order.distinct().size != order.size ||
            order.toSet() != expected.toSet()
        ) {
            throw IOException("Invalid favorite order")
        }
        snapshot.copy(
            profiles =
                snapshot.profiles.map { if (it.id == id) it.copy(favorites = order) else it },
        )
    }

    override suspend fun createFolder(profileId: String, name: String, appId: String) = update(profileId) { profile ->
        if (profile.folders.size >= MAX_APP_FOLDERS || !validFolderComponent(appId)) {
            throw IOException("Invalid folder change")
        }
        profile.copy(
            favorites = profile.favorites - appId,
            folders =
                profile.folders.map { it.copy(apps = it.apps - appId) } +
                    AppFolder(UUID.randomUUID().toString(), validName(name), listOf(appId)),
        )
    }

    override suspend fun renameFolder(profileId: String, folderId: String, name: String) = update(profileId) { profile ->
        if (profile.folders.none { it.id == folderId }) throw IOException("Folder removed")
        profile.copy(
            folders =
                profile.folders.map {
                    if (it.id == folderId) it.copy(name = validName(name)) else it
                },
        )
    }

    override suspend fun deleteFolder(profileId: String, folderId: String) = update(profileId) { profile ->
        if (profile.folders.none { it.id == folderId }) throw IOException("Folder removed")
        profile.copy(folders = profile.folders.filterNot { it.id == folderId })
    }

    override suspend fun moveToFolder(profileId: String, appId: String, folderId: String?) = update(profileId) { profile ->
        if (
            !validFolderComponent(appId) ||
            (folderId != null && profile.folders.none { it.id == folderId })
        ) {
            throw IOException("Invalid folder destination")
        }
        profile.copy(
            favorites = if (folderId != null) profile.favorites - appId else profile.favorites,
            folders =
                profile.folders.map { folder ->
                    folder.copy(
                        apps =
                            (folder.apps - appId) +
                                if (folder.id == folderId) listOf(appId) else emptyList(),
                    )
                },
        )
    }

    override suspend fun reorderFolder(profileId: String, folderId: String, expected: List<String>, order: List<String>) =
        transaction { snapshot ->
            val profile =
                snapshot.profiles.firstOrNull { it.id == profileId }
                    ?: throw IOException("Profile removed")
            val folder =
                profile.folders.firstOrNull { it.id == folderId } ?: throw IOException("Folder removed")
            if (
                snapshot.activeId != profileId ||
                folder.apps != expected ||
                order.size != expected.size ||
                order.distinct().size != order.size ||
                order.toSet() != expected.toSet()
            ) {
                throw IOException("Folder layout changed")
            }
            snapshot.copy(
                profiles =
                    snapshot.profiles.map {
                        if (it.id == profileId) {
                            it.copy(
                                folders =
                                    it.folders.map { f ->
                                        if (f.id == folderId) f.copy(apps = order) else f
                                    },
                            )
                        } else {
                            it
                        }
                    },
            )
        }

    override suspend fun create(name: String, avatar: String) = transaction {
        if (it.profiles.size >= 12) throw IOException("Profile limit reached")
        val profile = LocalProfile(UUID.randomUUID().toString(), validName(name), avatar)
        it.copy(activeId = profile.id, profiles = it.profiles + profile)
    }

    override suspend fun rename(id: String, name: String, avatar: String) = update(id) { it.copy(name = validName(name), avatar = avatar) }

    override suspend fun delete(id: String) = transaction {
        if (it.profiles.size == 1) throw IOException("Keep one profile")
        val remaining = it.profiles.filterNot { p -> p.id == id }
        it.copy(
            activeId = if (it.activeId == id) remaining.first().id else it.activeId,
            profiles = remaining,
        )
    }
}
