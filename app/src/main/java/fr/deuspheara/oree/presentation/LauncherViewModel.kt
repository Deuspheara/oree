package fr.deuspheara.oree.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.repository.CapabilityRepository
import fr.deuspheara.oree.core.domain.repository.ContentRepository
import fr.deuspheara.oree.core.domain.repository.InputRepository
import fr.deuspheara.oree.core.domain.repository.PhotoRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.source.ContentSelection
import fr.deuspheara.oree.core.domain.usecase.ImportWallpaperUseCase
import fr.deuspheara.oree.core.domain.usecase.InitializeLauncherUseCase
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.WallpaperMode
import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WallpaperImportState(
    val profileId: String,
    val saving: Boolean = true,
    val failed: Boolean = false,
    val completed: Boolean = false,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LauncherViewModel(
    private val store: ProfileRepository,
    val localPhotos: PhotoRepository,
    val tvInputs: InputRepository,
    val contentCapabilities: CapabilityRepository,
    private val catalog: ContentRepository,
    private val initialize: InitializeLauncherUseCase,
    private val importWallpaper: ImportWallpaperUseCase,
) : ViewModel() {
    val snapshots = store.snapshots.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), null)
    suspend fun readSnapshot() = store.readSnapshot()

    val canUndoRestore = store.canUndoRestore
    private val mutableImport = MutableStateFlow<WallpaperImportState?>(null)
    val importing = mutableImport.asStateFlow()

    fun importPhoto(id: String, uri: String) {
        if (mutableImport.value?.saving == true) return
        mutableImport.value = WallpaperImportState(id)
        viewModelScope.launch {
            try {
                importWallpaper(id, uri)
                mutableImport.value = WallpaperImportState(id, saving = false, completed = true)
            } catch (_: IOException) {
                mutableImport.value = WallpaperImportState(id, saving = false, failed = true)
            } catch (_: SecurityException) {
                mutableImport.value = WallpaperImportState(id, saving = false, failed = true)
            }
        }
    }

    fun clearImportResult() {
        if (mutableImport.value?.saving != true) mutableImport.value = null
    }

    private val mutableInitializationFailed = MutableStateFlow(false)
    val initializationFailed = mutableInitializationFailed.asStateFlow()
    private val selectedPackage = MutableStateFlow<String?>(null)

    private data class ContentOptions(val watchNext: Boolean, val previews: Boolean, val wallpaper: Boolean)

    private val options =
        snapshots
            .map { snapshot ->
                snapshot?.active.let { profile ->
                    ContentOptions(
                        profile?.watchNextEnabled == true,
                        profile?.appPreviewsEnabled == true,
                        profile?.contentEnabled == true &&
                            profile.wallpaper == WallpaperMode.CONTENT,
                    )
                }
            }
            .distinctUntilChanged()
    val content =
        options
            .flatMapLatest { options ->
                catalog.observe(
                    options.watchNext,
                    options.previews,
                    options.wallpaper,
                    selectedPackage,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ContentSelection())

    init {
        retryInitialization()
    }

    fun retryInitialization() {
        viewModelScope.launch {
            try {
                initialize()
                mutableInitializationFailed.value = false
            } catch (_: IOException) {
                mutableInitializationFailed.value = true
            } catch (_: SecurityException) {
                mutableInitializationFailed.value = true
            }
        }
    }

    fun selectArtwork(app: TvApp?) {
        selectedPackage.value = app?.packageName
    }

    fun retryContent() {
        catalog.refresh()
    }

    /** Accepted mutations outlive route recreation, while Android effects remain in the route. */
    private suspend fun persist(action: suspend () -> Unit) = viewModelScope.async { action() }.await()

    suspend fun setWallpaper(id: String, mode: WallpaperMode) = persist {
        store.setWallpaper(id, mode)
    }

    suspend fun setLandscape(id: String, landscape: Int) = persist {
        store.setLandscape(id, landscape)
    }

    suspend fun setContentEnabled(id: String, enabled: Boolean) = persist {
        store.setContentEnabled(id, enabled)
    }

    suspend fun setWatchNextEnabled(id: String, enabled: Boolean) = persist {
        store.setWatchNextEnabled(id, enabled)
    }

    suspend fun setAppPreviewsEnabled(id: String, enabled: Boolean) = persist {
        store.setAppPreviewsEnabled(id, enabled)
    }

    suspend fun setAmbientPhotos(id: String, enabled: Boolean) = persist {
        store.setAmbientPhotos(id, enabled)
    }

    suspend fun setAmbientClock(id: String, enabled: Boolean) = persist {
        store.setAmbientClock(id, enabled)
    }
}
