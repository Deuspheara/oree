package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job

/** Transient focus/scroll ownership for one composed Home, never a ViewModel or profile store. */
class HomeFocusState internal constructor(
    val grid: LazyGridState,
    val dock: LazyListState,
    val contentRow: LazyListState,
    val peekRow: LazyListState,
) {
    val navigation = HomeNavigationMotion()
    internal val pageReturnMotion = HomePageReturnMotion()
    val requesters = mutableStateMapOf<String, FocusRequester>()
    val contentRequesters = mutableStateMapOf<String, FocusRequester>()
    val commands = FocusRequester()
    val search = FocusRequester()
    val avatar = FocusRequester()
    val retry = FocusRequester()
    val add = FocusRequester()
    val overview = FocusRequester()
    val peekRequesters = mutableStateMapOf<String, FocusRequester>()
    val peekSelection = mutableMapOf<String, String>()

    // Job identity is coordination, not rendered state; replacing it must not recompose Home.
    var restoration: Job? = null
    var navigationSettlement: Job? = null
    var enteringContinuationFromDock = false
    var contextHeightPx: Int = 0
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberHomeFocusState(): HomeFocusState {
    // Prepare nearby rows while idle, including the first catalog row beyond a content-preview row.
    // Keep recently visited rows ready when returning to the header, so the next Down
    // does not rebuild the first grid row. Both directions have a fixed, bounded window.
    val cacheWindow = remember { LazyLayoutCacheWindow(ahead = 320.dp, behind = 320.dp) }
    val grid = rememberLazyGridState(cacheWindow = cacheWindow)
    val dock = rememberLazyListState()
    val content = rememberLazyListState()
    val peek = rememberLazyListState()
    return remember(grid, dock, content, peek) { HomeFocusState(grid, dock, content, peek) }
}
