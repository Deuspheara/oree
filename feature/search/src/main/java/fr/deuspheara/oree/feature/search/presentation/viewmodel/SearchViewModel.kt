package fr.deuspheara.oree.feature.search.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.domain.searchLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class SearchRequest(
    val query: String,
    val apps: List<TvApp>,
    val folders: List<AppFolder>,
    val actions: List<SearchItem.Action>,
    val content: List<ContentEntry>,
    val favorites: List<String>,
    val photos: List<LocalPhoto>,
)

data class SearchUiState(val results: List<SearchItem> = emptyList(), val request: SearchRequest? = null)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(private val dispatchers: AppDispatchers) : ViewModel() {
    private val request = MutableStateFlow<SearchRequest?>(null)
    val state =
        request
            .filterNotNull()
            .mapLatest { input ->
                withContext(dispatchers.default) {
                    SearchUiState(
                        searchLocal(
                            input.query,
                            input.apps,
                            input.folders,
                            input.actions,
                            input.content,
                            input.favorites,
                            input.photos,
                        ),
                        request = input,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), SearchUiState())

    fun search(value: SearchRequest) {
        request.value = value
    }
}
