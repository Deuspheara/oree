package fr.deuspheara.oree.feature.home.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntSize
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
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens as M
import fr.deuspheara.oree.core.domain.usecase.ProjectHomeUseCase
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.HomeContentLayout
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.MAX_DOCK_FAVORITES
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.AppTile
import fr.deuspheara.oree.feature.home.presentation.component.DockSurface
import fr.deuspheara.oree.feature.home.presentation.component.FolderPeek
import fr.deuspheara.oree.feature.home.presentation.component.FolderQuickPreview
import fr.deuspheara.oree.feature.home.presentation.component.FolderTile
import fr.deuspheara.oree.feature.home.presentation.component.HomeContinuationRow
import fr.deuspheara.oree.feature.home.presentation.component.HomeHeader
import fr.deuspheara.oree.feature.home.presentation.component.HomeOverview
import fr.deuspheara.oree.feature.home.presentation.component.HomeSection
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot
import fr.deuspheara.oree.feature.home.presentation.state.homeArtworkSize
import fr.deuspheara.oree.feature.home.presentation.state.homeGridArtworkSize
import fr.deuspheara.oree.feature.home.presentation.state.rememberHomeFocusState
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
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
    backgroundOnly: Boolean = false,
    editRequested: Int = 0,
    contentEntries: List<ContentEntry> = emptyList(),
    sourceKey: Any? = null,
    movingFavorite: String? = null,
    moveSaving: Boolean = false,
    moveFailed: Boolean = false,
    folders: List<AppFolder> = emptyList(),
    appPreviewsEnabled: Boolean = false,
    controls: Set<HomeControl> = emptySet(),
    onBackdropChanged: (Boolean) -> Unit = {},
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
    val covered = androidx.compose.runtime.rememberUpdatedState(backgroundOnly)
    val focus = rememberHomeFocusState()
    val focusManager = LocalFocusManager.current
    var overviewReturn by remember { mutableStateOf(false) }
    var peekReturn by remember { mutableStateOf<String?>(null) }
    var pendingSection by remember { mutableStateOf<HomeSection?>(null) }
    val homeWindowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val homeInputMode = androidx.compose.ui.platform.LocalInputModeManager.current
    val homeWindowFocused = homeWindowInfo.isWindowFocused
    var overviewOpen by rememberSaveable { mutableStateOf(false) }
    var peekFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastCatalog by rememberSaveable { mutableStateOf<String?>(null) }
    var lastFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var fastNavigation by remember { mutableStateOf(false) }
    val overviewScale = androidx.compose.animation.core.animateFloatAsState(
        if (overviewOpen) .90f else 1f,
        androidx.compose.animation.core.tween(if (reduceMotion || economy || systemReduceMotion == true) 0 else 220),
        label = "home-overview-compression",
    )
    val backdropChanged by androidx.compose.runtime.rememberUpdatedState(onBackdropChanged)
    val catalogRaised by remember {
        derivedStateOf {
            focus.grid.firstVisibleItemIndex > 0 || focus.grid.firstVisibleItemScrollOffset > 96
        }
    }
    LaunchedEffect(catalogRaised) { backdropChanged(catalogRaised) }
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
            contentEntries.filter {
                it.provider == ContentSourceId.WATCH_NEXT ||
                    (appPreviewsEnabled && it.source == previewPackage)
            }.distinctBy { it.source }.take(20)
        }
    var previousIndex by rememberSaveable { mutableIntStateOf(0) }
    // layoutInfo changes every scroll frame; only a column-count change affects row edges.
    val columns by remember(focus.grid) { derivedStateOf { focus.grid.layoutInfo.maxSpan.coerceAtLeast(1) } }
    var lastContent by rememberSaveable { mutableStateOf<String?>(null) }
    var rememberedContent by rememberSaveable { mutableStateOf<String?>(null) }
    val contentIds = remember(shownContent) { shownContent.map { it.id } }
    val scope = rememberCoroutineScope()
    var control by rememberSaveable { mutableStateOf<String?>(null) }
    val pageReduced = reduceMotion || (systemReduceMotion ?: false) || backgroundOnly
    val reduced = pageReduced || economy
    val statusRow = state.loading || state.failed || state.apps.isEmpty()
    val heroContent = shownContent.isNotEmpty() && profile?.preferences?.homeContentLayout == HomeContentLayout.ABOVE_DOCK
    val rowContent = shownContent.isNotEmpty() && !heroContent
    val base = 2 + (if (rowContent) 1 else 0) + (if (statusRow) 1 else 0)
    LaunchedEffect(focus.grid, pageReduced) {
        if (pageReduced) return@LaunchedEffect
        snapshotFlow { focus.grid.isScrollInProgress }.collectLatest { scrolling ->
            if (!scrolling) return@collectLatest
            do {
                withFrameNanos { frameTime ->
                    val grid = focus.grid
                    val position = if (grid.firstVisibleItemIndex <= 1 && focus.contextHeightPx > 0) {
                        val context = if (grid.firstVisibleItemIndex == 1) {
                            focus.contextHeightPx + grid.layoutInfo.mainAxisItemSpacing
                        } else {
                            0
                        }
                        (context + grid.firstVisibleItemScrollOffset).toFloat()
                    } else {
                        null
                    }
                    focus.pageReturnMotion.sample(position, frameTime, System.nanoTime())
                }
            } while (focus.grid.isScrollInProgress)
        }
    }
    suspend fun parkDock(animated: Boolean = true) {
        if (focus.grid.firstVisibleItemIndex == 0 && focus.grid.firstVisibleItemScrollOffset == 0) return
        if (!animated || pageReduced) {
            focus.grid.scrollToItem(0)
        } else if (focus.grid.firstVisibleItemIndex <= 1 && focus.contextHeightPx > 0) {
            // The measured hero occupies one complete grid row. Animate its exact remaining
            // distance, including a partially completed return, without a held-key snap.
            val contextDistance = if (focus.grid.firstVisibleItemIndex == 1) {
                focus.contextHeightPx + focus.grid.layoutInfo.mainAxisItemSpacing
            } else {
                0
            }
            val distance = -(contextDistance + focus.grid.firstVisibleItemScrollOffset).toFloat()
            val returnEasing = focus.pageReturnMotion.easing(distance, System.nanoTime())
            if (returnEasing !== FastOutSlowInEasing && android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
                android.os.Trace.beginSection("TVL:pageReturnHandoff")
                android.os.Trace.endSection()
            }
            focus.grid.animateScrollBy(distance, tween(420, easing = returnEasing))
        } else {
            focus.grid.animateScrollToItem(0)
        }
    }
    suspend fun revealForFocus(item: Int, animated: Boolean) {
        if (!animated || pageReduced) {
            focus.grid.scrollToItem(item)
        } else if (item < focus.grid.firstVisibleItemIndex) {
            // Upward navigation only needs to place the destination before handing its
            // reveal to Foundation. Waiting for full alignment keeps focus on the old
            // card, where repeated Up restarts the same preparatory animation.
            coroutineScope {
                var destinationPlaced = false
                val reveal = launch { focus.grid.animateScrollToItem(item) }
                val handoff = launch {
                    snapshotFlow { focus.grid.layoutInfo.visibleItemsInfo.any { it.index == item } }
                        .first { it }
                    destinationPlaced = true
                    reveal.cancel()
                }
                try {
                    reveal.join()
                    if (reveal.isCancelled && !destinationPlaced) {
                        throw CancellationException("Home reveal interrupted before focus handoff")
                    }
                } finally {
                    handoff.cancel()
                }
            }
        } else {
            focus.grid.animateScrollToItem(item)
        }
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
                revealForFocus(1, animated)
            }
        } else if (focus.grid.layoutInfo.visibleItemsInfo.none { it.index == item }) {
            if (animated && !pageReduced) focus.grid.animateScrollToItem(item) else focus.grid.scrollToItem(item)
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
            revealForFocus(1, animated = true)
        }
        withFrameNanos {}
        focus.add.requestFocus()
    }
    LaunchedEffect(movingFavorite, backgroundOnly) {
        if (movingFavorite != null && !backgroundOnly) {
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
        val index = shownContent.indexOfFirst { it.id == (lastContent ?: rememberedContent) }.coerceAtLeast(0)
        val contentIndex = if (heroContent) 0 else 2
        if (focus.grid.layoutInfo.visibleItemsInfo.none { it.index == contentIndex }) {
            revealForFocus(contentIndex, animated = true)
        }
        if (focus.contentRow.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            focus.contentRow.scrollToItem(index)
        }
        withFrameNanos {}
        val target = awaitRegisteredFocus { focus.contentRequesters[shownContent[index].id] }
        if (target != null) target.requestFocus() else focus.commands.requestFocus()
    }
    fun focusContinuationFromDock() {
        focus.restoration?.cancel()
        if (!pageReduced) {
            // Native lazy focus search can place the previous row without scrolling
            // to it first. Foundation then owns the focused card's entire reveal.
            focus.enteringContinuationFromDock = true
            val moved = try {
                focusManager.moveFocus(FocusDirection.Up)
            } finally {
                focus.enteringContinuationFromDock = false
            }
            if (moved) return
        }
        // Keep the bounded, cancellable path when the remembered card is unavailable.
        focus.restoration = scope.launch { focusContent() }
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
        if (covered.value || !homeWindowInfo.isWindowFocused) return
        lastContent = null
        control = null
        selected.value = id
        previousIndex = orderedIds.indexOf(id)
        traceHomeFocus(previousIndex, id)
        if (favoriteApps.any { it.id == id }) lastDock = id
        if (remaining.any { it.id == id && it is CatalogEntry.App }) lastCatalog = id
        if (remaining.any { it.id == id && it is CatalogEntry.Folder }) lastFolder = id
    }
    fun select(app: TvApp) = selectId(app.id)
    SettledHomeSelection(selected, state.apps, backgroundOnly) { app ->
        if (appPreviewsEnabled) previewPackage = app?.packageName
        onSelected(app)
    }
    val restoration =
        remember(orderedIds, favoriteIds, state.loading, state.failed, resume, backgroundOnly, homeWindowFocused) {
            selected.value?.takeIf { it in orderedIds }
                ?: orderedIds.getOrNull(
                    previousIndex.coerceIn(0, (orderedIds.size - 1).coerceAtLeast(0)),
                )
        }
    val restoreControl =
        remember(orderedIds, favoriteIds, state.loading, state.failed, resume, backgroundOnly, homeWindowFocused) { control }
    LaunchedEffect(orderedIds, favoriteIds, state.loading, state.failed, resume, backgroundOnly, homeWindowFocused) {
        focus.restoration?.cancel()
        if (state.loading || backgroundOnly || !homeWindowFocused) return@LaunchedEffect
        if (restoreControl != null) {
            parkDock(animated = false)
            withFrameNanos {}
            when (restoreControl) {
                "overview" -> if (HomeControl.OVERVIEW in controls) focus.overview else focus.commands
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
    LaunchedEffect(contentIds, backgroundOnly) {
        // Settled app previews change below the dock without resetting native app focus.
        // Only a focused content row needs restoration when its actual entries change.
        if (restoreControl == null && control == null && lastContent != null && !state.loading && !covered.value &&
            homeWindowInfo.isWindowFocused
        ) {
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
    LaunchedEffect(homeWindowFocused, overviewOpen, peekFolderId, pendingSection, overviewReturn, peekReturn) {
        if (!homeWindowFocused || overviewOpen || peekFolderId != null) return@LaunchedEffect
        withFrameNanos {}
        val section = pendingSection
        when {
            section != null -> {
                control = null
                focus.restoration?.cancel()
                when (section) {
                    HomeSection.CONTINUATION -> {
                        focus.grid.scrollToItem(if (heroContent) 0 else 2)
                        focusContent()
                    }

                    HomeSection.FAVORITES -> {
                        focus.grid.scrollToItem(1)
                        val id = favoriteApps.firstOrNull { it.id == lastDock }?.id ?: favoriteApps.firstOrNull()?.id
                        if (id != null) focusApp(id) else focusEmptyDock()
                    }

                    HomeSection.FOLDERS, HomeSection.APPS -> {
                        val candidates = remaining.filter {
                            if (section == HomeSection.FOLDERS) it is CatalogEntry.Folder else it is CatalogEntry.App
                        }
                        val remembered = if (section == HomeSection.FOLDERS) lastFolder else lastCatalog
                        val id = candidates.firstOrNull { it.id == remembered }?.id ?: candidates.firstOrNull()?.id
                        if (id != null) {
                            focus.grid.scrollToItem(base + remaining.indexOfFirst { it.id == id }.coerceAtLeast(0))
                            focusApp(id)
                        } else {
                            focus.overview.requestFocus()
                        }
                    }
                }
                pendingSection = null
            }

            overviewReturn -> {
                focus.overview.requestFocus()
                overviewReturn = false
            }

            peekReturn != null -> {
                val id = peekReturn
                peekReturn = null
                id?.let { focusApp(it) }
            }
        }
    }
    LaunchedEffect(peekFolderId, folders) {
        val id = peekFolderId
        if (id != null && folders.none { it.id == id }) {
            peekFolderId = null
            peekReturn = selected.value ?: orderedIds.firstOrNull()
        }
    }
    BackHandler(!backgroundOnly) {}
    val contentActive by remember { derivedStateOf { lastContent != null } }
    BackHandler(contentActive && !backgroundOnly) { contentUp() }
    BackHandler(editingApps && !backgroundOnly) { editingApps = false }
    BackHandler(movingFavorite != null && !backgroundOnly) { if (!moveSaving) onCancelMove() }
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
            .focusProperties { canFocus = !backgroundOnly }
            .graphicsLayer { alpha = if (backgroundOnly) 0f else 1f }
            .then(if (backgroundOnly) Modifier.clearAndSetSemantics {} else Modifier)
            .onPreviewKeyEvent {
                traceHomeKey(it.nativeKeyEvent, homeWindowInfo.isWindowFocused, covered.value, homeInputMode)
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
                if (it.key in listOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown)) {
                    if (it.type == KeyEventType.KeyDown) {
                        fastNavigation = it.nativeKeyEvent.repeatCount > 0
                        focus.navigationSettlement?.cancel()
                        focus.navigationSettlement = scope.launch {
                            delay(180)
                            fastNavigation = false
                        }
                    } else if (it.type == KeyEventType.KeyUp) {
                        focus.navigationSettlement?.cancel()
                        fastNavigation = false
                    }
                }
                if (it.type == KeyEventType.KeyDown) {
                    val direction =
                        when (it.key) {
                            Key.DirectionLeft -> Offset(-1f, 0f)
                            Key.DirectionRight -> Offset(1f, 0f)
                            Key.DirectionUp -> Offset(0f, -1f)
                            Key.DirectionDown -> Offset(0f, 1f)
                            else -> Offset.Zero
                        }
                    if (direction.y != 0f && focus.navigation.direction.y == -direction.y) {
                        focus.pageReturnMotion.reset()
                        // Cancel both the old scroll and its deferred focus request before
                        // the newly pressed direction reaches the currently focused control.
                        focus.restoration?.cancel()
                        if (direction.y < 0f && control != null) {
                            focus.navigation.direction = direction
                            focus.restoration = scope.launch { parkDock() }
                            return@onPreviewKeyEvent true
                        }
                    }
                    focus.navigation.direction = direction
                }
                false
            }
            .semantics { testTagsAsResourceId = true },
    ) {
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(112.dp).zIndex(3f)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = .24f), Color.Transparent),
                    ),
                ),
        )
        HomeHeader(
            onOverview = {
                focus.restoration?.cancel()
                overviewOpen = true
            },
            loading = state.loading,
            profile = profile,
            reduced = reduced,
            controls = controls,
            focus = focus,
            onAction = onAction,
            onFocused = { target ->
                if (!state.loading && !covered.value && homeWindowInfo.isWindowFocused) {
                    lastContent = null
                    control = when (target) {
                        HomeControl.OVERVIEW -> "overview"
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
                // Keep the actual header identity until a card receives focus. A reversal
                // during its reveal must still be handled as header navigation.
                focus.restoration?.cancel()
                focus.restoration = scope.launch {
                    val id = favoriteApps.firstOrNull { it.id == lastDock }?.id ?: favoriteApps.firstOrNull()?.id
                    if (heroContent) {
                        focusContent()
                    } else if (id != null) {
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
        if (!backgroundOnly) {
            HomeClock(Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 24.dp).zIndex(4f))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            var dockHeight by remember { mutableStateOf(140.dp) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            val topInset = if (heroContent) 64.dp else 96.dp
            val bottomInset = if (heroContent) 20.dp else 32.dp
            val heroHeight = (maxHeight - topInset - dockHeight - bottomInset - 28.dp).coerceAtLeast(0.dp)
            val continuationViewportWidth = maxWidth
            val continuationWidth = (maxWidth - 104.dp - 56.dp - 48.dp) / 2
            val tileWidth = ((maxWidth - 104.dp - 100.dp) / MAX_DOCK_FAVORITES).coerceAtLeast(1.dp)
            // Requests have their final bounded size during lazy precomposition, before layout.
            val dockArtworkSize = with(density) {
                val width = tileWidth.roundToPx()
                homeArtworkSize(IntSize(width, (width * 9f / 16).toInt()))
            }
            val gridArtworkSize = with(density) {
                homeGridArtworkSize(
                    constraints.maxWidth - 52.dp.roundToPx() * 2,
                    tileWidth.roundToPx(),
                    20.dp.roundToPx(),
                )
            }
            val focusMargin = with(density) { 32.dp.toPx() }
            val minimumScroll =
                remember(focusMargin, pageReduced) {
                    object : BringIntoViewSpec {
                        override val scrollAnimationSpec = if (pageReduced) {
                            super.scrollAnimationSpec
                        } else {
                            spring<Float>(dampingRatio = 1f, stiffness = 180f)
                        }

                        override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                            val margin = focusMargin
                            return when {
                                offset < margin -> offset - margin

                                offset + size > containerSize - margin ->
                                    offset + size - containerSize + margin

                                else -> 0f
                            }
                        }
                    }
                }
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides minimumScroll,
                fr.deuspheara.oree.core.designsystem.artwork.LocalFastNavigation provides fastNavigation,
            ) {
                LazyVerticalGrid(
                    GridCells.Fixed(MAX_DOCK_FAVORITES),
                    state = focus.grid,
                    modifier =
                        Modifier.fillMaxSize()
                            .focusProperties {
                                onExit = {
                                    // A missing lazy target must use the existing fallback,
                                    // not let this one Up skip continuation for the header.
                                    if (focus.enteringContinuationFromDock) cancelFocusChange()
                                }
                            }
                            .graphicsLayer {
                                alpha = if (hideCards) {
                                    0f
                                } else if (overviewOpen) {
                                    .55f
                                } else {
                                    1f
                                }
                                scaleX = overviewScale.value
                                scaleY = overviewScale.value
                            }
                            .then(if (hideCards) Modifier.clearAndSetSemantics {} else Modifier)
                            .testTag("home"),
                    contentPadding =
                        PaddingValues(start = 52.dp, end = 52.dp, top = topInset, bottom = bottomInset),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    item(
                        key = "context",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = "space",
                    ) {
                        Box(Modifier.height(heroHeight).fillMaxWidth().onSizeChanged { focus.contextHeightPx = it.height }) {
                            if (heroContent) {
                                val largeWidth = minOf(
                                    continuationWidth,
                                    (heroHeight - 80.dp - 44.dp * density.fontScale).coerceAtLeast(72.dp) * (16f / 9f),
                                )
                                HomeContinuationRow(
                                    shownContent, state.apps, focus,
                                    stringResource(
                                        if (shownContent.any {
                                                it.provider == ContentSourceId.WATCH_NEXT
                                            }
                                        ) {
                                            R.string.continue_watching
                                        } else {
                                            R.string.watch_next_title
                                        },
                                    ),
                                    largeWidth, true, reduced,
                                    viewportWidth = continuationViewportWidth,
                                    onFocus = {
                                        if (!covered.value && homeWindowInfo.isWindowFocused) {
                                            traceHomeContentFocus(shownContent, it.id)
                                            control = null
                                            lastContent = it.id
                                            rememberedContent = it.id
                                        }
                                    },
                                    onOpen = onOpenContent,
                                    onUp = ::focusHeader,
                                    onDown = ::contentUp,
                                    onEnterFromBelow = {
                                        val entry = shownContent.firstOrNull { it.id == (lastContent ?: rememberedContent) }
                                            ?: shownContent.firstOrNull()
                                        entry?.let { focus.contentRequesters[it.id]?.requestFocus() } == true
                                    },
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                )
                            }

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
                            Modifier.requiredWidth(maxWidth - 64.dp)
                                .onGloballyPositioned {
                                    dockHeight = with(density) { it.size.height.toDp() }
                                }
                                .clip(RoundedCornerShape(30.dp))
                                .testTag("favorites-dock"),
                        ) {
                            DockSurface(Modifier.matchParentSize())
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
                                        userScrollEnabled = false,
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
                                                    loadingVisible = !hideCards && !backgroundOnly,
                                                    preparedArtworkSize = dockArtworkSize,
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
                                                    onUp = if (heroContent) ::focusContinuationFromDock else ::focusHeader,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rowContent) {
                        item(key = "watch-next", span = { GridItemSpan(maxLineSpan) }, contentType = "content") {
                            HomeContinuationRow(
                                shownContent, state.apps, focus,
                                stringResource(R.string.watch_next_title), tileWidth, false, reduced,
                                viewportWidth = continuationViewportWidth,
                                onFocus = {
                                    if (!covered.value && homeWindowInfo.isWindowFocused) {
                                        traceHomeContentFocus(shownContent, it.id)
                                        control = null
                                        lastContent = it.id
                                        rememberedContent = it.id
                                    }
                                },
                                onOpen = onOpenContent,
                                onUp = ::contentUp,
                                onDown = {
                                    focus.restoration?.cancel()
                                    focus.restoration = scope.launch {
                                        remaining.firstOrNull()?.let { focusApp(it.id, animated = true) }
                                    }
                                },
                            )
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
                                        if (rowContent) {
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
                                    loadingVisible = !hideCards && !backgroundOnly,
                                    preparedArtworkSize = gridArtworkSize,
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
                                    onPeek = {
                                        selectId(item.id)
                                        peekFolderId = item.folder.id
                                    },
                                    onManage = {
                                        selectId(item.id)
                                        onAction(HomeAction.OpenFolder(item.folder.id, manage = true))
                                    },
                                )
                        }
                    }
                }
            }
        }
        val selectedFolder by remember(folders) { derivedStateOf { folders.firstOrNull { it.homeId == selected.value } } }
        val focusedFolder = selectedFolder?.takeIf {
            control == null && !contentActive && !hideCards && !backgroundOnly && !overviewOpen && peekFolderId == null
        }
        if (focusedFolder != null) {
            key(focusedFolder.id) {
                FolderQuickPreview(
                    focusedFolder,
                    state.apps,
                    reduced,
                    Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 24.dp),
                )
            }
        }
        val moreBelow by remember { derivedStateOf { focus.grid.canScrollForward } }
        if (moreBelow && remaining.isNotEmpty() && !overviewOpen && peekFolderId == null && !hideCards && !backgroundOnly) {
            Text(
                stringResource(R.string.home_more_below),
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    .background(Color.Black.copy(alpha = .45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp).testTag("home-more-below"),
                fontSize = 12.sp,
            )
        }
        if (overviewOpen) {
            HomeOverview(
                buildMap {
                    if (shownContent.isNotEmpty()) put(HomeSection.CONTINUATION, shownContent.size)
                    if (favoriteApps.isNotEmpty()) put(HomeSection.FAVORITES, favoriteApps.size)
                    if (folders.isNotEmpty()) put(HomeSection.FOLDERS, folders.size)
                    if (projection.catalog.isNotEmpty()) put(HomeSection.APPS, projection.catalog.size)
                },
                reduced,
                sourceKey,
                onSelect = { section ->
                    overviewOpen = false
                    pendingSection = section
                },
                onClose = {
                    overviewOpen = false
                    overviewReturn = true
                },
            )
        }
        val peekFolder = folders.firstOrNull { it.id == peekFolderId }
        if (peekFolder != null) {
            FolderPeek(
                peekFolder,
                state.apps,
                focus,
                reduced,
                sourceKey,
                onOpen,
                onOpenFolder = {
                    peekFolderId = null
                    onFolder(peekFolder.id)
                },
                onManage = {
                    peekFolderId = null
                    onAction(HomeAction.OpenFolder(peekFolder.id, manage = true))
                },
                onClose = {
                    peekFolderId = null
                    peekReturn = peekFolder.homeId
                },
            )
        }
    }
}

/** Trace-only logical destinations; no app labels, persistent logs, or frame-loop work. */
private fun traceHomeFocus(index: Int, id: String? = null) {
    if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
        android.os.Trace.beginSection("TVL:focus:$index")
        android.os.Trace.endSection()
        if (id != null) traceHomeIdentity("app", id)
    }
}

/** Event timestamps are Android uptime; Perfetto provides their trace-clock observations. */
private fun traceHomeKey(
    event: android.view.KeyEvent,
    windowFocused: Boolean,
    covered: Boolean,
    inputMode: androidx.compose.ui.input.InputModeManager,
) {
    if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
        android.os.Trace.beginSection(
            "TVL:key:${event.eventTime}:${event.downTime}:${event.action}:${event.keyCode}:${event.repeatCount}" +
                ":flags=${event.flags}:source=${event.source}:window=$windowFocused:covered=$covered:mode=${inputMode.inputMode}",
        )
        android.os.Trace.endSection()
    }
}

private fun traceHomeContentFocus(entries: List<ContentEntry>, id: String) {
    if (android.os.Build.VERSION.SDK_INT >= 29 && android.os.Trace.isEnabled()) {
        android.os.Trace.beginSection("TVL:contentFocus:${entries.indexOfFirst { it.id == id }}")
        android.os.Trace.endSection()
        traceHomeIdentity("content", id)
    }
}

/** Raw IDs stay in private traces; long IDs use a stable digest to respect Trace's name bound. */
private fun traceHomeIdentity(kind: String, id: String) {
    val identity = if (id.length <= 96) {
        id
    } else {
        "sha256:" + android.util.Base64.encodeToString(
            java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)),
            android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING,
        )
    }
    android.os.Trace.beginSection("TVL:identity:$kind:$identity")
    android.os.Trace.endSection()
}

/** Observe only this small effect scope while focus moves; the catalog does not recompose. */
@Composable
private fun SettledHomeSelection(
    selection: androidx.compose.runtime.State<String?>,
    apps: List<TvApp>,
    covered: Boolean,
    onSettled: (TvApp?) -> Unit,
) {
    val selected = selection.value
    val latest by androidx.compose.runtime.rememberUpdatedState(onSettled)
    LaunchedEffect(selected, apps, covered) {
        if (covered) return@LaunchedEffect
        delay(M.PREVIEW_DELAY_MILLIS)
        latest(apps.firstOrNull { it.id == selected })
    }
}
