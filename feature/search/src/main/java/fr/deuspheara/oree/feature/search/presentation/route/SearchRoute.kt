package fr.deuspheara.oree.feature.search.presentation.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.presentation.screen.SearchPanel
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchRequest
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchViewModel

@Composable
fun SearchRoute(
    model: SearchViewModel,
    request: SearchRequest,
    onQuery: (String) -> Unit,
    resume: Int,
    reduced: Boolean,
    onSelect: (SearchItem) -> Unit,
    onAppMenu: (TvApp, Rect, Boolean) -> Unit,
    onClose: () -> Unit,
    onSystemSearch: ((String) -> Unit)? = null,
) {
    LaunchedEffect(request) { model.search(request) }
    val state by model.state.collectAsStateWithLifecycle()
    SearchPanel(
        request.query, onQuery, resume, reduced, onSelect, onAppMenu, onClose,
        state.takeIf { it.ready }?.results, onSystemSearch,
    )
}
