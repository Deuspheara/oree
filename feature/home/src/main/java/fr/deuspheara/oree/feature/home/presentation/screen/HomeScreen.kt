package fr.deuspheara.oree.feature.home.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.HomeClock
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.GlassSurfaceRole
import fr.deuspheara.oree.core.designsystem.glass.GlassTreatment
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens as M
import fr.deuspheara.oree.core.domain.usecase.ProjectHomeUseCase
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.AppTile
import fr.deuspheara.oree.feature.home.presentation.component.ContentCard
import fr.deuspheara.oree.feature.home.presentation.component.FolderTile
import fr.deuspheara.oree.feature.home.presentation.component.HomeHeader
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot
import fr.deuspheara.oree.feature.home.presentation.state.rememberHomeFocusState
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface CatalogEntry {
    val id: String

    data class App(val app: TvApp) : CatalogEntry {
        override val id
            get() = app.id
    }

    data class Folder(val folder: AppFolder) : CatalogEntry {
        override val id
            get() = folder.homeId
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    resume: Int,
    reduceMotion: Boolean,
    onAction: (HomeAction) -> Unit,
    economy: Boolean = false,
    systemReduceMotion: Boolean? = null,
    favorites: List<String> = emptyList(),
    profile: LocalProfile? = null,
    hideCards: Boolean = false,
    editRequested: Int = 0,
    contentEntries: List<ContentEntry> = emptyList(),
    retainDock: Boolean = false,
    sourceKey: Any? = null,
    movingFavorite: String? = null,
    moveSaving: Boolean = false,
    moveFailed: Boolean = false,
    folders: List<AppFolder> = emptyList(),
    appPreviewsEnabled: Boolean = false,
    controls: Set<HomeControl> = emptySet(),
) {
    val onOpen = { app: TvApp -> onAction(HomeAction.OpenApp(app)) }
    val onRetry = { onAction(HomeAction.Retry) }
    val onSelected = { app: TvApp? -> onAction(HomeAction.SelectArtwork(app)) }
    val onOpenContent = { entry: ContentEntry -> onAction(HomeAction.OpenContent(entry)) }
    val onMoveFavorite = { direction: Int -> onAction(HomeAction.MoveFavorite(direction)) }
    val onCommitMove = { onAction(HomeAction.CommitMove) }
    val onCancelMove = { onAction(HomeAction.CancelMove) }
    val onFolder = { id: String -> onAction(HomeAction.OpenFolder(id)) }
    val onAppMenu: ((TvApp, Rect, Boolean) -> Unit)? = if (HomeControl.APP_MENU in
        controls
    ) {
        { app: TvApp, bounds: Rect, held: Boolean -> onAction(HomeAction.OpenAppMenu(app, bounds, held)) }
    } else {
        null
    }
    val focus = rememberHomeFocusState()
    var editingApps by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editRequested) { if (editRequested > 0) editingApps = true }
    val projection =
        remember(state.apps, favorites, folders) {
            ProjectHomeUseCase()(state.apps, favorites, folders)
        }
    val favoriteApps = projection.favorites
    // Membership can relocate a card between lazy layouts without changing its global order.
    val favoriteIds = remember(favoriteApps) { favoriteApps.map { it.id } }
    val remaining =
        remember(projection, folders) {
            folders.map { CatalogEntry.Folder(it) } +
                projection.catalog.map { CatalogEntry.App(it) }
        }
    val orderedIds =
        remember(favoriteApps, remaining) { favoriteApps.map { it.id } + remaining.map { it.id } }
    var lastDock by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = rememberSaveable { mutableStateOf<String?>(null) }
    var previewPackage by remember { mutableStateOf<String?>(null) }
    val shownContent =
        remember(contentEntries, appPreviewsEnabled, previewPackage) {
            if (appPreviewsEnabled) {
                contentEntries.filter { it.source == previewPackage }
            } else {
                contentEntries
            }
        }
    var previousIndex by rememberSaveable { mutableIntStateOf(0) }
    // layoutInfo changes every scroll frame; only a column-count change affects row edges.
    val columns by remember(focus.grid) { derivedStateOf { focus.grid.layoutInfo.maxSpan.coerceAtLeast(1) } }
    var lastContent by rememberSaveable { mutableStateOf<String?>(null) }
    val contentIds = remember(shownContent) { shownContent.map { it.id } }
    val scope = rememberCoroutineScope()
    var control by rememberSaveable { mutableStateOf<String?>(null) }
    val reduced = reduceMotion || (systemReduceMotion ?: false) || economy
    val statusRow = state.loading || state.failed || state.apps.isEmpty()
    val base = 2 + (if (shownContent.isNotEmpty()) 1 else 0) + (if (statusRow) 1 else 0)
    suspend fun parkDock(animated: Boolean = true) {
        if (focus.grid.firstVisibleItemIndex == 0 && focus.grid.firstVisibleItemScrollOffset == 0) return
        if (animated && !reduced) focus.grid.animateScrollToItem(0) else focus.grid.scrollToItem(0)
    }
    suspend fun focusApp(id: String, animated: Boolean = false) {
        val fi = favoriteApps.indexOfFirst { it.id == id }
        val item =
            if (fi >= 0) 1 else base + remaining.indexOfFirst { it.id == id }.coerceAtLeast(0)
        if (fi >= 0) {
            if (
                focus.grid.layoutInfo.visibleItemsInfo.isNotEmpty() &&
                focus.grid.layoutInfo.visibleItemsInfo.none { it.index == 1 }
            ) {
                if (animated && !reduced) focus.grid.animateScrollToItem(1) else focus.grid.scrollToItem(1)
            }
        } else if (focus.grid.layoutInfo.visibleItemsInfo.none { it.index == item }) {
            focus.grid.scrollToItem(item)
        }
        if (fi >= 0 && focus.dock.layoutInfo.visibleItemsInfo.none { it.key == id }) focus.dock.scrollToItem(fi)
        // Item relocation (dock <-> catalog) disposes and registers a different requester.
        // Wait for that composition before resolving its current identity.
        withFrameNanos {}
        val target = awaitRegisteredFocus { focus.requesters[id] }
        if (target != null) target.requestFocus() else focus.commands.requestFocus()
    }
    suspend fun focusEmptyDock() {
        if (focus.grid.layoutInfo.visibleItemsInfo.none { it.index == 1 }) {
            if (!reduced) focus.grid.animateScrollToItem(1) else focus.grid.scrollToItem(1)
        }
        withFrameNanos {}
        focus.add.requestFocus()
    }
    LaunchedEffect(movingFavorite) {
        if (movingFavorite != null) {
            focus.restoration?.cancel()
            parkDock(animated = false)
            focusApp(movingFavorite)
        }
    }
    fun focusHeader() {
        focus.restoration?.cancel()
        focus.commands.requestFocus()
        focus.restoration = scope.launch { parkDock() }
    }
    suspend fun focusContent() {
        if (shownContent.isEmpty()) return
        val index = shownContent.indexOfFirst { it.id == lastContent }.coerceAtLeast(0)
        if (focus.grid.layoutInfo.visibleItemsInfo.none { it.key == "watch-next" }) focus.grid.scrollToItem(2)
        focus.contentRow.scrollToItem(index)
        withFrameNanos {}
        val target = awaitRegisteredFocus { focus.contentRequesters[shownContent[index].id] }
        if (target != null) target.requestFocus() else focus.commands.requestFocus()
    }
    fun contentUp() {
        focus.restoration?.cancel()
        focus.restoration = scope.launch {
            lastContent = null
            val id =
                favoriteApps.firstOrNull { it.id == lastDock }?.id ?: favoriteApps.firstOrNull()?.id
            if (id != null) focusApp(id, animated = true) else focusEmptyDock()
        }
    }
    fun selectId(id: String) {
        lastContent = null
        control = null
        selected.value = id
        previousIndex = orderedIds.indexOf(id)
        traceHomeFocus(previousIndex)
        if (favoriteApps.any { it.id == id }) lastDock = id
    }
    fun select(app: TvApp) = selectId(app.id)
    LaunchedEffect(selected.value, state.apps) {
        previewPackage = null
        delay(M.PREVIEW_DELAY_MILLIS)
        val app = state.apps.firstOrNull { it.id == selected.value }
        previewPackage = app?.packageName
        onSelected(app)
    }
    val restoration =
        remember(orderedIds, favoriteIds, state.loading, state.failed, resume) {
            selected.value?.takeIf { it in orderedIds }
                ?: orderedIds.getOrNull(
                    previousIndex.coerceIn(0, (orderedIds.size - 1).coerceAtLeast(0)),
                )
        }
    val restoreControl = remember(orderedIds, favoriteIds, state.loading, state.failed, resume) { control }
    LaunchedEffect(orderedIds, favoriteIds, state.loading, state.failed, resume) {
        focus.restoration?.cancel()
        if (state.loading) return@LaunchedEffect
        if (restoreControl != null) {
            parkDock(animated = false)
            withFrameNanos {}
            when (restoreControl) {
                "commands" -> focus.commands
                "avatar" -> focus.avatar
                "search" -> focus.search
                else -> focus.commands
            }.requestFocus()
        } else if (lastContent != null && shownContent.isNotEmpty()) {
            focusContent()
        } else if (restoration != null) {
            selected.value = restoration
            focusApp(restoration)
        } else {
            withFrameNanos {}
            focus.commands.requestFocus()
        }
    }
    LaunchedEffect(contentIds) {
        // Settled app previews change below the dock without resetting native app focus.
        // Only a focused content row needs restoration when its actual entries change.
        if (lastContent != null && !state.loading) {
            focus.restoration?.cancel()
            if (shownContent.isNotEmpty()) {
                focusContent()
            } else {
                lastContent = null
                selected.value?.takeIf { it in orderedIds }?.let { focusApp(it) }
                    ?: focus.commands.requestFocus()
            }
        }
    }
    BackHandler {}
    BackHandler(lastContent != null) { contentUp() }
    BackHandler(editingApps) { editingApps = false }
    BackHandler(movingFavorite != null) { if (!moveSaving) onCancelMove() }
    val moveDirection =
        if (
            androidx.compose.ui.platform.LocalLayoutDirection.current ==
            androidx.compose.ui.unit.LayoutDirection.Rtl
        ) {
            -1
        } else {
            1
        }
    Box(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent {
                if (
                    movingFavorite != null &&
                    it.key in
                    listOf(
                        Key.DirectionLeft,
                        Key.DirectionRight,
                        Key.DirectionUp,
                        Key.DirectionDown,
                        Key.DirectionCenter,
                        Key.Enter,
                        Key.NumPadEnter,
                    )
                ) {
                    if (!moveSaving) {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft) {
                            onMoveFavorite(-moveDirection)
                        }
                        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight) {
                            onMoveFavorite(moveDirection)
                        }
                        if (
                            it.type == KeyEventType.KeyUp &&
                            it.key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
                        ) {
                            onCommitMove()
                        }
                    }
                    return@onPreviewKeyEvent true
                }
                if (it.type == KeyEventType.KeyDown) {
                    focus.navigation.direction =
                        when (it.key) {
                            Key.DirectionLeft -> Offset(-1f, 0f)
                            Key.DirectionRight -> Offset(1f, 0f)
                            Key.DirectionUp -> Offset(0f, -1f)
                            Key.DirectionDown -> Offset(0f, 1f)
                            else -> Offset.Zero
                        }
                }
                false
            }
            .semantics { testTagsAsResourceId = true },
    ) {
        HomeHeader(
            loading = state.loading,
            profile = profile,
            reduced = reduced,
            controls = controls,
            focus = focus,
            onAction = onAction,
            onFocused = { target ->
                if (!state.loading) {
                    control = when (target) {
                        HomeControl.SEARCH -> "search"
                        HomeControl.PROFILES -> "avatar"
                        else -> "commands"
                    }
                    traceHomeFocus(
                        when (target) {
                            HomeControl.SEARCH -> -3
                            HomeControl.PROFILES -> -2
                            else -> -1
                        },
                    )
                    focus.restoration?.cancel()
                }
            },
            onDown = {
                control = null
                focus.restoration?.cancel()
                focus.restoration = scope.launch {
                    val id = favoriteApps.firstOrNull { it.id == lastDock }?.id ?: favoriteApps.firstOrNull()?.id
                    if (id != null) {
                        focusApp(id, animated = true)
                    } else if (state.apps.isEmpty()) {
                        withFrameNanos {}
                        focus.retry.requestFocus()
                    } else {
                        focusEmptyDock()
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopEnd).zIndex(4f).padding(horizontal = 32.dp, vertical = 12.dp),
        )
        HomeClock(Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 24.dp).zIndex(4f))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            var dockHeight by remember { mutableStateOf(140.dp) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val heroHeight = (maxHeight - 96.dp - dockHeight - 32.dp - 28.dp).coerceAtLeast(0.dp)
            val tileWidth = ((maxWidth - 96.dp - 100.dp) / 6).coerceAtLeast(120.dp)
            val safeTop = with(density) { 96.dp.toPx() }
            val minimumScroll =
                remember(safeTop) {
                    object : BringIntoViewSpec {
                        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                            val margin = 20f
                            return when {
                                offset < safeTop -> offset - safeTop

                                offset + size > containerSize - margin ->
                                    offset + size - containerSize + margin

                                else -> 0f
                            }
                        }
                    }
                }
            CompositionLocalProvider(LocalBringIntoViewSpec provides minimumScroll) {
                LazyVerticalGrid(
                    GridCells.Adaptive(tileWidth),
                    state = focus.grid,
                    modifier =
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = if (hideCards) 0f else 1f }
                            .then(if (hideCards) Modifier.clearAndSetSemantics {} else Modifier)
                            .testTag("home"),
                    contentPadding =
                        PaddingValues(start = 32.dp, end = 32.dp, top = 96.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    item(
                        key = "context",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "space",
                    ) {
                        Box(Modifier.height(heroHeight).fillMaxWidth()) {
                            if (movingFavorite != null) {
                                Column(
                                    Modifier.align(Alignment.BottomCenter)
                                        .padding(12.dp)
                                        .background(D.background, RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                        .testTag("favorite-move"),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.move_favorite_position,
                                            favoriteApps
                                                .firstOrNull { it.id == movingFavorite }
                                                ?.label
                                                .orEmpty(),
                                            favoriteApps.indexOfFirst { it.id == movingFavorite } +
                                                1,
                                            favoriteApps.size,
                                        ),
                                        color = D.foreground,
                                    )
                                    Text(
                                        stringResource(
                                            if (moveSaving) {
                                                R.string.saving
                                            } else {
                                                R.string.move_favorite_hint
                                            },
                                        ),
                                        color = D.foreground,
                                    )
                                    if (moveFailed) {
                                        Text(
                                            stringResource(R.string.organization_write_error),
                                            color = D.foreground,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "dock", span = { GridItemSpan(maxLineSpan) }, contentType = "dock") {
                        Box(
                            Modifier.fillMaxWidth()
                                .onGloballyPositioned {
                                    dockHeight = with(density) { it.size.height.toDp() }
                                }
                                .clip(RoundedCornerShape(30.dp))
                                .testTag("favorites-dock"),
                        ) {
                            if (retainDock) {
                                RetainedMenuSurface(
                                    sourceKey,
                                    reduced,
                                    Modifier.matchParentSize(),
                                    role = GlassSurfaceRole.CONTROL,
                                    radius = 30.dp,
                                    treatment = GlassTreatment.CLEAR,
                                    performanceMode =
                                        dev.chrisbanes.haze.HazePerformanceMode.Performance,
                                )
                            } else {
                                Box(
                                    Modifier.matchParentSize()
                                        .glassSurface(
                                            radius = 30.dp,
                                            treatment = GlassTreatment.CLEAR,
                                            performanceMode =
                                                dev.chrisbanes.haze.HazePerformanceMode.Performance,
                                        ),
                                )
                            }
                            if (favoriteApps.isEmpty()) {
                                Row(
                                    Modifier.padding(24.dp).heightIn(min = 100.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                ) {
                                    LauncherIcon(
                                        LauncherSymbol.FAVORITE,
                                        Color.White,
                                        Modifier.size(32.dp),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.favorites),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                        )
                                        Text(
                                            stringResource(R.string.choose_favorites_hint),
                                            color = Color.White.copy(alpha = .75f),
                                            fontSize = 14.sp,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            editingApps = true
                                            remaining.firstOrNull()?.let { target ->
                                                focus.restoration = scope.launch { focusApp(target.id) }
                                            }
                                        },
                                        modifier =
                                            Modifier.focusRequester(focus.add)
                                                .testTag("choose-favorites")
                                                .onPreviewKeyEvent {
                                                    if (it.key == Key.DirectionUp) {
                                                        if (
                                                            it.type == KeyEventType.KeyDown &&
                                                            it.nativeKeyEvent.repeatCount == 0
                                                        ) {
                                                            focusHeader()
                                                        }
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                },
                                    ) {
                                        Text(stringResource(R.string.choose_favorites))
                                    }
                                }
                            } else {
                                CompositionLocalProvider(
                                    LocalBringIntoViewSpec provides
                                        remember { object : BringIntoViewSpec {} },
                                ) {
                                    LazyRow(
                                        state = focus.dock,
                                        contentPadding =
                                            PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    ) {
                                        itemsIndexed(
                                            favoriteApps,
                                            key = { _, app -> app.id },
                                            contentType = { _, _ -> "app" },
                                        ) { index, app ->
                                            val requester = remember(app.id) { FocusRequester() }
                                            DisposableEffect(app.id) {
                                                focus.requesters[app.id] = requester
                                                onDispose {
                                                    if (focus.requesters[app.id] === requester) {
                                                        focus.requesters.remove(app.id)
                                                    }
                                                }
                                            }
                                            Box(
                                                Modifier.width(tileWidth)
                                                    .animateItem(
                                                        fadeInSpec = null,
                                                        fadeOutSpec = null,
                                                        placementSpec =
                                                            if (reduced || movingFavorite == null) {
                                                                androidx.compose.animation.core
                                                                    .snap()
                                                            } else {
                                                                androidx.compose.animation.core
                                                                    .tween(210)
                                                            },
                                                    ),
                                            ) {
                                                AppTile(
                                                    app,
                                                    requester,
                                                    reduced,
                                                    HomeGridSlot(
                                                        1,
                                                        index == 0,
                                                        index == favoriteApps.lastIndex,
                                                    ),
                                                    focus.navigation,
                                                    loadingVisible = !hideCards,
                                                    onFocus = { select(app) },
                                                    onClick = { bounds ->
                                                        select(app)
                                                        if (editingApps && onAppMenu != null) {
                                                            onAppMenu(app, bounds, false)
                                                        } else {
                                                            onOpen(app)
                                                        }
                                                    },
                                                    onMenu =
                                                        onAppMenu?.let { cb ->
                                                            { bounds ->
                                                                select(app)
                                                                cb(app, bounds, true)
                                                            }
                                                        },
                                                    onUp = ::focusHeader,
                                                    freshUpOnly = true,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (shownContent.isNotEmpty()) {
                        item(
                            key = "watch-next",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "content",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (appPreviewsEnabled) {
                                        stringResource(
                                            R.string.app_previews_from,
                                            state.apps
                                                .firstOrNull { it.packageName == previewPackage }
                                                ?.label
                                                .orEmpty(),
                                        )
                                    } else {
                                        stringResource(R.string.watch_next_title)
                                    },
                                    color = Color.White,
                                    fontSize = 20.sp,
                                )
                                LazyRow(
                                    state = focus.contentRow,
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                    modifier = Modifier.testTag("watch-next-row"),
                                ) {
                                    itemsIndexed(
                                        shownContent,
                                        key = { _, entry -> entry.id },
                                        contentType = { _, _ -> "content" },
                                    ) { index, entry ->
                                        val requester = remember(entry.id) { FocusRequester() }
                                        DisposableEffect(entry.id) {
                                            focus.contentRequesters[entry.id] = requester
                                            onDispose { focus.contentRequesters.remove(entry.id) }
                                        }
                                        ContentCard(
                                            entry,
                                            state.apps.firstOrNull {
                                                it.packageName == entry.source
                                            },
                                            reduced,
                                            Modifier.width(tileWidth)
                                                .focusRequester(requester)
                                                .focusProperties {
                                                    start =
                                                        if (index == 0) {
                                                            FocusRequester.Cancel
                                                        } else {
                                                            FocusRequester.Default
                                                        }
                                                    end =
                                                        if (index == shownContent.lastIndex) {
                                                            FocusRequester.Cancel
                                                        } else {
                                                            FocusRequester.Default
                                                        }
                                                },
                                            onFocus = {
                                                control = null
                                                lastContent = entry.id
                                            },
                                            onClick = { onOpenContent(entry) },
                                            onUp = ::contentUp,
                                            onDown = {
                                                focus.restoration?.cancel()
                                                focus.restoration = scope.launch {
                                                    remaining.firstOrNull()?.let { focusApp(it.id) }
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (statusRow) {
                        item(
                            key = "status",
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = "status",
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    stringResource(
                                        when {
                                            state.loading -> R.string.loading
                                            state.failed -> R.string.error
                                            else -> R.string.empty
                                        },
                                    ),
                                    color = Color.White,
                                )
                                if (!state.loading) {
                                    Button(
                                        onClick = onRetry,
                                        modifier = Modifier.focusRequester(focus.retry).testTag("retry"),
                                    ) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed(
                        remaining,
                        key = { _, item -> item.id },
                        contentType = { _, item ->
                            if (item is CatalogEntry.Folder) "folder" else "app"
                        },
                    ) { index, item ->
                        val requester = remember(item.id) { FocusRequester() }
                        DisposableEffect(item.id) {
                            focus.requesters[item.id] = requester
                            onDispose {
                                if (focus.requesters[item.id] === requester) focus.requesters.remove(item.id)
                            }
                        }
                        val slot =
                            HomeGridSlot(
                                base + index,
                                index % columns == 0,
                                index % columns == columns - 1 || index == remaining.lastIndex,
                            )
                        val up: (() -> Unit)? =
                            if (index < columns) {
                                {
                                    focus.restoration?.cancel()
                                    focus.restoration = scope.launch {
                                        if (shownContent.isNotEmpty()) {
                                            focusContent()
                                        } else if (favoriteApps.isNotEmpty()) {
                                            focusApp(
                                                favoriteApps.firstOrNull { it.id == lastDock }?.id
                                                    ?: favoriteApps.first().id,
                                                animated = true,
                                            )
                                        } else {
                                            focusEmptyDock()
                                        }
                                    }
                                }
                            } else {
                                null
                            }
                        when (item) {
                            is CatalogEntry.App ->
                                AppTile(
                                    item.app,
                                    requester,
                                    reduced,
                                    slot,
                                    focus.navigation,
                                    loadingVisible = !hideCards,
                                    onFocus = { select(item.app) },
                                    onClick = { bounds ->
                                        select(item.app)
                                        if (editingApps && onAppMenu != null) {
                                            onAppMenu(item.app, bounds, false)
                                        } else {
                                            onOpen(item.app)
                                        }
                                    },
                                    onMenu =
                                        onAppMenu?.let { cb ->
                                            { bounds ->
                                                select(item.app)
                                                cb(item.app, bounds, true)
                                            }
                                        },
                                    onUp = up,
                                )

                            is CatalogEntry.Folder ->
                                FolderTile(
                                    item.folder,
                                    state.apps,
                                    requester,
                                    reduced,
                                    slot,
                                    onFocus = { selectId(item.id) },
                                    onOpen = {
                                        selectId(item.id)
                                        onFolder(item.folder.id)
                                    },
                                    onUp = up,
                                )
                        }
                    }
                }
            }
        }
    }
}

/** Trace-only logical destinations; no app labels, persistent logs, or frame-loop work. */
private fun traceHomeFocus(index: Int) {
    if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
        android.os.Trace.beginSection("TVL:focus:$index")
        android.os.Trace.endSection()
    }
}
