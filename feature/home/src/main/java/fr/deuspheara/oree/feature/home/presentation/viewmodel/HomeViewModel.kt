package fr.deuspheara.oree.feature.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.model.TvApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(val apps: List<TvApp> = emptyList(), val loading: Boolean = true, val failed: Boolean = false)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: AppRepository, private val organization: OrganizationRepository) : ViewModel() {
    private val refresh =
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    private var last = HomeUiState()
    val state =
        merge(repository.changes, refresh)
            .conflate()
            .mapLatest {
                try {
                    HomeUiState(repository.load(), loading = false)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    last.copy(loading = false, failed = true)
                }
                    .also { last = it }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), last)

    fun reload() {
        refresh.tryEmit(Unit)
    }

    private suspend fun persist(action: suspend () -> Unit) = viewModelScope.async { action() }.await()

    suspend fun setFavorite(id: String, component: String, favorite: Boolean) = persist {
        organization.setFavorite(id, component, favorite)
    }

    suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>) = persist {
        organization.reorderFavorites(id, expected, order)
    }

    suspend fun createFolder(id: String, name: String, appId: String) = persist {
        organization.createFolder(id, name, appId)
    }

    suspend fun renameFolder(id: String, folderId: String, name: String) = persist {
        organization.renameFolder(id, folderId, name)
    }

    suspend fun deleteFolder(id: String, folderId: String) = persist {
        organization.deleteFolder(id, folderId)
    }

    suspend fun moveToFolder(id: String, appId: String, folderId: String?) = persist {
        organization.moveToFolder(id, appId, folderId)
    }

    suspend fun reorderFolder(id: String, folderId: String, expected: List<String>, order: List<String>) =
        persist { organization.reorderFolder(id, folderId, expected, order) }
}
