package fr.deuspheara.oree.feature.search.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.search.domain.SearchItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchPanel(
    query: String,
    onQuery: (String) -> Unit,
    resume: Int,
    reduced: Boolean,
    onSelect: (SearchItem) -> Unit,
    onAppMenu: (TvApp, Rect, Boolean) -> Unit,
    onClose: () -> Unit,
    resultState: List<SearchItem>?,
    onSystemSearch: ((String) -> Unit)? = null,
) {
    val results = resultState.orEmpty()
    val list = rememberLazyListState()
    val input = remember { FocusRequester() }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var queryFocused by rememberSaveable { mutableStateOf(true) }
    var restoring by remember { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var ime by remember { mutableStateOf(false) }
    val motion = rememberPanelTransition(reduced)
    fun close() {
        if (ime) keyboard?.hide() else motion.close(onClose)
    }
    suspend fun focusResult(position: Int) {
        val target = results.getOrNull(position) ?: return
        keyboard?.hide()
        list.scrollToItem(position)
        withFrameNanos {}
        val requester = awaitRegisteredFocus { requesters[target.id] }
        if (requester != null) requester.requestFocus() else input.requestFocus()
    }
    GlassDialog(onDismissRequest = ::close) {
        val windowIme = WindowInsets.isImeVisible
        SideEffect { ime = windowIme }
        val ready = LocalWindowInfo.current.isWindowFocused
        Column(
            Modifier.fillMaxSize()
                .padding(32.dp)
                .imePadding()
                .panelMotion(motion, reduced)
                .menuSurface()
                .padding(20.dp)
                .semantics { testTagsAsResourceId = true }
                .testTag("search-panel"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val searchLabel = stringResource(R.string.search)
                BasicTextField(
                    query,
                    { value ->
                        if (value.length <= 120 && value.none(Char::isISOControl)) onQuery(value)
                    },
                    Modifier.weight(1f)
                        .background(DesignTokens.surface, RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            if (queryFocused) DesignTokens.accent else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .padding(14.dp)
                        .focusRequester(input)
                        .onFocusChanged { if (!restoring) queryFocused = it.isFocused }
                        .semantics { contentDescription = searchLabel }
                        .testTag("search-query")
                        .onPreviewKeyEvent {
                            if (it.key == Key.DirectionDown && results.isNotEmpty()) {
                                if (it.type == KeyEventType.KeyDown) scope.launch { focusResult(0) }
                                true
                            } else {
                                false
                            }
                        },
                    textStyle = TextStyle(color = DesignTokens.foreground, fontSize = 22.sp),
                    singleLine = true,
                    cursorBrush = SolidColor(DesignTokens.foreground),
                    decorationBox = { field ->
                        Box {
                            if (query.isEmpty()) {
                                Text(searchLabel, color = DesignTokens.secondary, fontSize = 22.sp)
                            }
                            field()
                        }
                    },
                )
                SettingsRow(
                    stringResource(R.string.close),
                    ::close,
                    Modifier.width(100.dp).testTag("search-close"),
                    compact = true,
                )
            }
            if (results.isEmpty()) {
                Text(stringResource(R.string.search_no_results), color = DesignTokens.secondary)
            }
            LazyColumn(
                state = list,
                modifier = Modifier.weight(1f).testTag("search-results"),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    results,
                    key = { _, item -> item.id },
                    contentType = { _, item -> item::class },
                ) { position, item ->
                    val requester = remember(item.id) { FocusRequester() }
                    var focused by remember { mutableStateOf(false) }
                    var bounds by remember { mutableStateOf(Rect.Zero) }
                    DisposableEffect(item.id) {
                        requesters[item.id] = requester
                        onDispose { requesters.remove(item.id) }
                    }
                    val kind =
                        stringResource(
                            when (item) {
                                is SearchItem.App -> R.string.search_app
                                is SearchItem.Folder -> R.string.search_folder
                                is SearchItem.Action -> R.string.search_action
                                is SearchItem.Content -> R.string.search_content
                                is SearchItem.Photo -> R.string.search_photo
                            },
                        )
                    Row(
                        Modifier.fillMaxWidth()
                            .background(
                                DesignTokens.foreground.copy(alpha = if (focused) .14f else 0f),
                                RoundedCornerShape(12.dp),
                            )
                            .focusRequester(requester)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (focused) {
                                    selected = item.id
                                    index = position
                                }
                            }
                            .onGloballyPositioned { bounds = it.boundsInWindow() }
                            .onPreviewKeyEvent {
                                if (position == 0 && it.key == Key.DirectionUp) {
                                    if (it.type == KeyEventType.KeyDown) input.requestFocus()
                                    true
                                } else {
                                    false
                                }
                            }
                            .semantics { contentDescription = "${item.title}, $kind" }
                            .testTag("search-result:${item.id}")
                            .combinedClickable(
                                role = Role.Button,
                                onClick = { onSelect(item) },
                                onLongClick =
                                    if (item is SearchItem.App) {
                                        ({ onAppMenu(item.app, bounds, true) })
                                    } else {
                                        null
                                    },
                                onLongClickLabel =
                                    if (item is SearchItem.App) {
                                        stringResource(R.string.edit_apps)
                                    } else {
                                        null
                                    },
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            item.title,
                            color = DesignTokens.foreground,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                        Text(kind, color = DesignTokens.secondary, fontSize = 13.sp)
                    }
                }
            }
            if (onSystemSearch != null && query.isNotBlank()) {
                SettingsRow(
                    stringResource(R.string.search_android),
                    { onSystemSearch(query) },
                    Modifier.fillMaxWidth().testTag("search-android"),
                    compact = true,
                )
            }
        }
        LaunchedEffect(ready, resume, results.map { it.id }, resultState != null) {
            // A restored result must wait for the worker's first result set. Focusing the
            // query during that empty loading frame would overwrite the saved target.
            if (ready && (selected == null || resultState != null)) {
                if (queryFocused || selected == null || results.isEmpty()) {
                    withFrameNanos {}
                    input.requestFocus()
                } else {
                    focusResult(
                        results.indexOfFirst { it.id == selected }.takeIf { it >= 0 }
                            ?: index.coerceIn(0, (results.size - 1).coerceAtLeast(0)),
                    )
                }
                restoring = false
            }
        }
    }
}
