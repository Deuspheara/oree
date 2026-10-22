package fr.deuspheara.oree.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.model.TvApp
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeUiState(val apps: List<TvApp> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

enum class OrganizationWriteStatus { SAVING, COMPLETED, FAILED }

data class OrganizationWriteState(val profileId: String, val status: OrganizationWriteStatus)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: AppRepository, private val organization: OrganizationRepository) : ViewModel() {
    private val refresh =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private var cached = HomeUiState()
    val state: StateFlow<HomeUiState> =
        merge(repository.changes, refresh)
            .conflate()
            .mapLatest {
                try {
                    HomeUiState(repository.load(), loading = false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    cached.copy(loading = false, failed = true)
                }
                    .also { cached = it }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), cached)

    fun reload() {
        refresh.tryEmit(Unit)
    }

    private val mutations = Mutex()
    private val mutableWrite = MutableStateFlow<OrganizationWriteState?>(null)
    val writing = mutableWrite.asStateFlow()

    fun acknowledgeWrite() {
        if (mutableWrite.value?.status != OrganizationWriteStatus.SAVING) mutableWrite.value = null
    }

    // Accepted writes belong to the ViewModel; cancelling a route waiter does not cancel a commit.
    // async is intentional here: await delivers failure to the caller while the owner survives it.
    private suspend fun persist(profileId: String, action: suspend () -> Unit) = viewModelScope.async {
        mutations.withLock {
            mutableWrite.value = OrganizationWriteState(profileId, OrganizationWriteStatus.SAVING)
            try {
                action()
                mutableWrite.value = OrganizationWriteState(profileId, OrganizationWriteStatus.COMPLETED)
            } catch (failure: IOException) {
                mutableWrite.value = OrganizationWriteState(profileId, OrganizationWriteStatus.FAILED)
                throw failure
            } catch (failure: SecurityException) {
                mutableWrite.value = OrganizationWriteState(profileId, OrganizationWriteStatus.FAILED)
                throw failure
            }
        }
    }.await()

    suspend fun setFavorite(id: String, component: String, favorite: Boolean) = persist(id) {
        organization.setFavorite(id, component, favorite)
    }

    suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>) = persist(id) {
        organization.reorderFavorites(id, expected, order)
    }

    suspend fun createFolder(id: String, name: String, appId: String) = persist(id) {
        organization.createFolder(id, name, appId)
    }

    suspend fun renameFolder(id: String, folderId: String, name: String) = persist(id) {
        organization.renameFolder(id, folderId, name)
    }

    suspend fun deleteFolder(id: String, folderId: String) = persist(id) {
        organization.deleteFolder(id, folderId)
    }

    suspend fun moveToFolder(id: String, appId: String, folderId: String?) = persist(id) {
        organization.moveToFolder(id, appId, folderId)
    }

    suspend fun reorderFolder(id: String, folderId: String, expected: List<String>, order: List<String>) =
        persist(id) { organization.reorderFolder(id, folderId, expected, order) }
}
