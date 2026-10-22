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
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class LauncherWriteStatus { SAVING, FAILED, COMPLETED }

data class LauncherWriteState(val profileId: String, val status: LauncherWriteStatus)

enum class WallpaperImportStatus { SAVING, FAILED, COMPLETED }

data class WallpaperImportState(val profileId: String, val status: WallpaperImportStatus = WallpaperImportStatus.SAVING)

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

    val canUndoRestore = store.canUndoRestore
    private val mutableImport = MutableStateFlow<WallpaperImportState?>(null)
    val importing = mutableImport.asStateFlow()

    fun importPhoto(id: String, uri: String) {
        if (mutableImport.value?.status == WallpaperImportStatus.SAVING) return
        mutableImport.value = WallpaperImportState(id)
        viewModelScope.launch {
            try {
                importWallpaper(id, uri)
                mutableImport.value = WallpaperImportState(id, status = WallpaperImportStatus.COMPLETED)
            } catch (_: IOException) {
                mutableImport.value = WallpaperImportState(id, status = WallpaperImportStatus.FAILED)
            } catch (_: SecurityException) {
                mutableImport.value = WallpaperImportState(id, status = WallpaperImportStatus.FAILED)
            }
        }
    }

    fun clearImportResult() {
        if (mutableImport.value?.status != WallpaperImportStatus.SAVING) mutableImport.value = null
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

    private var initialization: Job? = null

    init {
        retryInitialization()
    }

    fun retryInitialization() {
        if (initialization?.isActive == true) return
        initialization = viewModelScope.launch {
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
    private val mutations = Mutex()
    private val mutableWrite = MutableStateFlow<LauncherWriteState?>(null)
    val writing = mutableWrite.asStateFlow()

    fun acknowledgeWrite() {
        if (mutableWrite.value?.status != LauncherWriteStatus.SAVING) mutableWrite.value = null
    }

    private suspend fun persist(profileId: String, action: suspend () -> Unit) = viewModelScope.async {
        mutations.withLock {
            mutableWrite.value = LauncherWriteState(profileId, LauncherWriteStatus.SAVING)
            try {
                action()
                mutableWrite.value = LauncherWriteState(profileId, LauncherWriteStatus.COMPLETED)
            } catch (failure: IOException) {
                mutableWrite.value = LauncherWriteState(profileId, LauncherWriteStatus.FAILED)
                throw failure
            } catch (failure: SecurityException) {
                mutableWrite.value = LauncherWriteState(profileId, LauncherWriteStatus.FAILED)
                throw failure
            }
        }
    }.await()

    suspend fun setWallpaper(id: String, mode: WallpaperMode) = persist(id) {
        store.setWallpaper(id, mode)
    }

    suspend fun setLandscape(id: String, landscape: Int) = persist(id) {
        store.setLandscape(id, landscape)
    }

    suspend fun setContentEnabled(id: String, enabled: Boolean) = persist(id) {
        store.setContentEnabled(id, enabled)
    }

    suspend fun setWatchNextEnabled(id: String, enabled: Boolean) = persist(id) {
        store.setWatchNextEnabled(id, enabled)
    }

    suspend fun setAppPreviewsEnabled(id: String, enabled: Boolean) = persist(id) {
        store.setAppPreviewsEnabled(id, enabled)
    }

    suspend fun setAmbientPhotos(id: String, enabled: Boolean) = persist(id) {
        store.setAmbientPhotos(id, enabled)
    }

    suspend fun setAmbientClock(id: String, enabled: Boolean) = persist(id) {
        store.setAmbientClock(id, enabled)
    }
}
