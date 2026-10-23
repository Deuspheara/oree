package fr.deuspheara.oree.feature.search.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.platform.image.LocalPhotoThumbnail
import fr.deuspheara.oree.feature.search.domain.SearchItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
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
    sourceStatus: String? = null,
    sourceKey: Any? = null,
) {
    val results = remember(resultState) { resultState.orEmpty().sortedBy { it.groupOrder() } }
    val cacheWindow = remember { LazyLayoutCacheWindow(ahead = 240.dp, behind = 0.dp) }
    val list = rememberLazyGridState(cacheWindow = cacheWindow)
    val input = remember { FocusRequester() }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var queryFocused by rememberSaveable { mutableStateOf(true) }
    var restoring by remember { mutableStateOf(true) }
    var restoredResume by remember { mutableIntStateOf(resume) }
    var resultsFocused by remember { mutableStateOf(false) }
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
        list.scrollToItem(position + results.take(position + 1).distinctBy { it.groupOrder() }.size)
        withFrameNanos {}
        val requester = awaitRegisteredFocus { requesters[target.id] }
        if (requester != null) requester.requestFocus() else input.requestFocus()
    }
    GlassDialog(onDismissRequest = ::close) {
        val windowIme = WindowInsets.isImeVisible
        SideEffect { ime = windowIme }
        val ready = LocalWindowInfo.current.isWindowFocused
        Box(
            Modifier.fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp)
                .imePadding()
                // Keep sampling bounds stationary: scaling invalidates retained glass on every exit frame.
                .graphicsLayer { alpha = motion.progress.value }
                .clip(RoundedCornerShape(26.dp)),
        ) {
            // The wallpaper source is stationary while results focus, scroll and update.
            // Retain only its exact glass pixels; input and results remain live above it.
            RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
            Column(
                Modifier.fillMaxSize()
                    .padding(28.dp)
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
                            .onFocusChanged {
                                if (it.isFocused && !restoring) {
                                    queryFocused = true
                                    resultsFocused = false
                                }
                            }
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
                        Modifier.width(100.dp).onFocusChanged {
                            if (it.isFocused) {
                                resultsFocused = false
                                queryFocused = false
                            }
                        }.testTag("search-close"),
                        compact = true,
                    )
                }
                if (sourceStatus != null) {
                    Text(
                        sourceStatus,
                        color = DesignTokens.secondary,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("search-source-status"),
                    )
                }
                if (results.isEmpty()) {
                    Text(
                        stringResource(if (resultState == null) R.string.diagnostics_loading else R.string.search_no_results),
                        color = DesignTokens.secondary,
                        modifier = Modifier.testTag("search-status"),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(340.dp),
                    state = list,
                    modifier = Modifier.weight(1f).testTag("search-results"),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    results.forEachIndexed { position, item ->
                        if (position == 0 || results[position - 1].groupOrder() != item.groupOrder()) {
                            item(key = "group:${item.groupOrder()}", span = { GridItemSpan(maxLineSpan) }, contentType = "heading") {
                                Text(
                                    stringResource(
                                        when (item) {
                                            is SearchItem.App -> R.string.search_apps_group
                                            is SearchItem.Folder -> R.string.search_folders_group
                                            is SearchItem.Photo -> R.string.search_photos_group
                                            is SearchItem.Action -> R.string.search_action
                                            is SearchItem.Content -> R.string.search_content
                                        },
                                    ),
                                    color = DesignTokens.secondary,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                        }
                        item(key = item.id, contentType = item::class) {
                            val requester = remember(item.id) { FocusRequester() }
                            var focused by remember { mutableStateOf(false) }
                            val interactions = remember { MutableInteractionSource() }
                            val pressed by interactions.collectIsPressedAsState()
                            var bounds by remember { mutableStateOf(Rect.Zero) }
                            DisposableEffect(item.id) {
                                requesters[item.id] = requester
                                onDispose { requesters.remove(item.id) }
                            }
                            val resultForeground = if (focused || pressed) DesignTokens.background else DesignTokens.foreground
                            val resultSecondary = if (focused || pressed) {
                                DesignTokens.background.copy(alpha = .75f)
                            } else {
                                DesignTokens.secondary
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
                                        DesignTokens.foreground.copy(
                                            alpha = if (pressed) {
                                                .78f
                                            } else if (focused) {
                                                1f
                                            } else {
                                                .035f
                                            },
                                        ),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .focusRequester(requester)
                                    .onFocusChanged {
                                        focused = it.isFocused
                                        if (focused) {
                                            selected = item.id
                                            index = position
                                            queryFocused = false
                                            resultsFocused = true
                                        }
                                    }
                                    .onGloballyPositioned { bounds = it.boundsInWindow() }
                                    .onPreviewKeyEvent {
                                        if (
                                            position < list.layoutInfo.maxSpan &&
                                            item.groupOrder() == results.firstOrNull()?.groupOrder() &&
                                            it.key == Key.DirectionUp
                                        ) {
                                            if (it.type == KeyEventType.KeyDown) input.requestFocus()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    .semantics { contentDescription = "${item.title}, $kind" }
                                    .testTag("search-result:${item.id}")
                                    .combinedClickable(
                                        interactionSource = interactions,
                                        indication = null,
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
                                SearchArtwork(item)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        item.title,
                                        color = resultForeground,
                                        fontSize = 18.sp,
                                        maxLines = 2,
                                    )
                                    val detail = when (item) {
                                        is SearchItem.Content -> item.entry.source
                                        is SearchItem.Folder -> stringResource(R.string.folder_apps_count, item.folder.apps.size)
                                        else -> kind
                                    }
                                    Text(detail, color = resultSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                if (onSystemSearch != null && query.isNotBlank()) {
                    SettingsRow(
                        stringResource(R.string.search_android),
                        { onSystemSearch(query) },
                        Modifier.fillMaxWidth().onFocusChanged {
                            if (it.isFocused) {
                                resultsFocused = false
                                queryFocused = false
                            }
                        }.testTag("search-android"),
                        compact = true,
                    )
                }
            }
        }
        LaunchedEffect(ready, resume, results.map { it.id }, resultState != null) {
            if (!ready) {
                restoring = true
                return@LaunchedEffect
            }
            val missingFocusedResult = resultsFocused && selected != null && results.none { it.id == selected }
            if (!restoring && restoredResume == resume && !missingFocusedResult) return@LaunchedEffect
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
                restoredResume = resume
            }
        }
    }
}

private fun SearchItem.groupOrder(): Int = when (this) {
    is SearchItem.App -> 0
    is SearchItem.Folder -> 1
    is SearchItem.Action -> 2
    is SearchItem.Content -> 3
    is SearchItem.Photo -> 4
}

@Composable
private fun SearchArtwork(item: SearchItem) {
    val context = LocalContext.current
    val request = remember(item) {
        val data = when (item) {
            is SearchItem.App -> item.app.takeIf { it.artwork != 0 }?.let {
                "android.resource://${it.packageName}/${it.artwork}"
            }

            is SearchItem.Content -> item.entry.artwork

            is SearchItem.Photo -> LocalPhotoThumbnail(item.photo.uri)

            else -> null
        }
        data?.let {
            ImageRequest.Builder(context).data(it).size(256, 144)
                .apply {
                    if (item is SearchItem.App) memoryCacheKey(item.app.artworkCacheKey)
                    if (item is SearchItem.Photo) memoryCacheKey(LocalPhotoThumbnail(item.photo.uri).cacheKey)
                }.build()
        }
    }
    Box(
        Modifier.width(128.dp).height(72.dp).background(DesignTokens.surface, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        LauncherIcon(
            when (item) {
                is SearchItem.App -> LauncherSymbol.OPEN
                is SearchItem.Folder -> LauncherSymbol.HOME
                is SearchItem.Action -> LauncherSymbol.SETTINGS
                is SearchItem.Content -> LauncherSymbol.OPEN
                is SearchItem.Photo -> LauncherSymbol.WALLPAPER
            },
            DesignTokens.secondary,
        )
        if (request != null) {
            AsyncImage(
                request,
                null,
                Modifier.fillMaxSize(),
                contentScale = if (item is SearchItem.App) ContentScale.Fit else ContentScale.Crop,
            )
        }
    }
}
