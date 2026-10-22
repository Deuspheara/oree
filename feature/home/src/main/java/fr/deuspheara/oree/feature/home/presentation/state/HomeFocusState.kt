package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.Job

/** Transient focus/scroll ownership for one composed Home, never a ViewModel or profile store. */
class HomeFocusState internal constructor(val grid: LazyGridState, val dock: LazyListState, val contentRow: LazyListState) {
    val navigation = HomeNavigationMotion()
    val requesters = mutableStateMapOf<String, FocusRequester>()
    val contentRequesters = mutableStateMapOf<String, FocusRequester>()
    val commands = FocusRequester()
    val search = FocusRequester()
    val avatar = FocusRequester()
    val retry = FocusRequester()
    val add = FocusRequester()

    // Job identity is coordination, not rendered state; replacing it must not recompose Home.
    var restoration: Job? = null
}

@Composable
fun rememberHomeFocusState(): HomeFocusState {
    val grid = rememberLazyGridState()
    val dock = rememberLazyListState()
    val content = rememberLazyListState()
    return remember(grid, dock, content) { HomeFocusState(grid, dock, content) }
}
