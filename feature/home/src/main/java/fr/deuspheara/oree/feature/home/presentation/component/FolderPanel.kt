package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.folderApps
import fr.deuspheara.oree.core.model.moveFavoriteApp
import fr.deuspheara.oree.core.model.restoreAppId
import fr.deuspheara.oree.feature.home.presentation.component.AppTile
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot
import fr.deuspheara.oree.feature.home.presentation.state.HomeNavigationMotion

@Composable
fun FolderTile(
    folder: AppFolder,
    catalog: List<TvApp>,
    requester: FocusRequester,
    reduced: Boolean,
    slot: HomeGridSlot,
    onFocus: () -> Unit,
    onOpen: () -> Unit,
    onUp: (() -> Unit)?,
    onManage: (() -> Unit)? = null,
    onPeek: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val members = remember(folder, catalog) { folderApps(folder, catalog) }
    val previews = remember(members) { members.take(4) }
    Box(Modifier.zIndex(if (focused) 1f else 0f).fillMaxWidth().padding(vertical = 8.dp).aspectRatio(16f / 9f)) {
        FocusArtwork(
            focused,
            pressed,
            reduced,
            Offset.Zero,
            Modifier.fillMaxSize(),
            cacheArtwork = true,
            focusSpec = HomeCardFocusSpec,
        ) {
            Column(
                Modifier.fillMaxSize().background(DesignTokens.surface).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        folder.name,
                        color = DesignTokens.foreground,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        members.size.toString(),
                        color = DesignTokens.secondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp)
                            .background(DesignTokens.background.copy(alpha = .35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val side = minOf(maxHeight, ((maxWidth - 18.dp) / 4).coerceAtLeast(0.dp))
                    Row(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        repeat(4) { index ->
                            Box(
                                Modifier.size(side).clip(RoundedCornerShape(8.dp))
                                    .background(DesignTokens.background.copy(alpha = .45f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                previews.getOrNull(index)?.let { app ->
                                    key(app.id) { FolderPreview(app) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            Modifier.matchParentSize()
                .focusRequester(requester)
                .focusProperties {
                    start = if (slot.atStart) FocusRequester.Cancel else FocusRequester.Default
                    end = if (slot.atEnd) FocusRequester.Cancel else FocusRequester.Default
                }
                .onFocusChanged {
                    focused = it.isFocused
                    if (focused) onFocus()
                }
                .onPreviewKeyEvent {
                    if (it.key == Key.Menu && onPeek != null) {
                        if (it.type == KeyEventType.KeyDown && it.nativeKeyEvent.repeatCount == 0) onPeek()
                        true
                    } else if (it.key == Key.DirectionUp && onUp != null) {
                        if (it.type == KeyEventType.KeyDown) onUp()
                        true
                    } else {
                        false
                    }
                }
                .semantics {
                    contentDescription = folder.name
                    role = Role.Button
                }
                .testTag("folder:${folder.id}")
                .combinedClickable(
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Button,
                    onLongClickLabel = stringResource(if (onPeek != null) R.string.folder_peek else R.string.folder_options),
                    onLongClick = onPeek ?: onManage,
                    onClick = onOpen,
                ),
        )
    }
}

@Composable
internal fun FolderPreview(app: TvApp) {
    val context = LocalContext.current
    val icon = app.icon.takeIf { it != 0 } ?: app.artwork
    if (icon != 0) {
        val request = remember(context, app.packageName, icon, app.artworkVersion) {
            ImageRequest.Builder(context)
                .data("android.resource://${app.packageName}/$icon")
                .memoryCacheKey("${app.packageName}/$icon@${app.artworkVersion}/folder-icon")
                .size(128, 128)
                .build()
        }
        AsyncImage(request, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    } else {
        Text(app.label.take(1), color = DesignTokens.foreground, fontSize = 22.sp)
    }
}

@Composable
private fun FolderHeaderButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    reduced: Boolean,
    title: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(50)),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = if (reduced) 1f else .98f),
        border = ButtonDefaults.border(border = Border.None, focusedBorder = Border.None),
        colors = ButtonDefaults.colors(
            containerColor = if (title) Color.Transparent else DesignTokens.foreground.copy(alpha = .08f),
            contentColor = DesignTokens.foreground,
            focusedContainerColor = DesignTokens.foreground.copy(alpha = .18f),
            focusedContentColor = DesignTokens.foreground,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = DesignTokens.foreground.copy(alpha = .35f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            fontSize = if (title) 36.sp else 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class FolderMode { BROWSE, OPTIONS, REMOVE }
private enum class FolderFocus { APP, TITLE, OPTIONS, ADD }

@Composable
fun FolderPanel(
    folder: AppFolder,
    catalog: List<TvApp>,
    resume: Int,
    reduced: Boolean,
    saving: Boolean,
    failed: Boolean,
    moving: String?,
    onOpen: (TvApp) -> Unit,
    onMenu: (TvApp, Rect, Boolean) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    onReorder: (List<String>, List<String>) -> Unit,
    onCancelMove: () -> Unit,
    onAddApps: (() -> Unit)? = null,
    initiallyManaging: Boolean = false,
) {
    val grid = rememberLazyGridState()
    val columns by remember { derivedStateOf { grid.layoutInfo.maxSpan.coerceAtLeast(1) } }
    val installed = remember(catalog) { catalog.associateBy { it.id } }
    val original = remember(folder.id, moving) { folder.apps }
    var draft by remember(folder.id, moving) { mutableStateOf(folder.apps) }
    val visible =
        remember(folder.apps, installed, draft, moving) {
            (if (moving != null) draft else folder.apps).mapNotNull(installed::get)
        }
    val visibleIds = remember(visible) { visible.map { it.id } }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var selected by rememberSaveable(folder.id) { mutableStateOf<String?>(null) }
    var previousIndex by rememberSaveable(folder.id) { mutableIntStateOf(0) }
    val rename = remember { FocusRequester() }
    val options = remember { FocusRequester() }
    var mode by rememberSaveable(folder.id) {
        mutableStateOf(if (initiallyManaging) FolderMode.OPTIONS else FolderMode.BROWSE)
    }
    var focusOwner by rememberSaveable(folder.id) { mutableStateOf(FolderFocus.APP) }
    var focusInitialized by remember { mutableStateOf(false) }
    var returnToOptions by remember { mutableStateOf(false) }
    val add = remember { FocusRequester() }
    val remove = remember { FocusRequester() }
    val cancelRemove = remember { FocusRequester() }
    fun selectHeader(owner: FolderFocus) {
        // Native focus may temporarily fall onto the title while a removed app is disposed.
        // Preserve app ownership until its surviving logical neighbor has been restored.
        if (focusInitialized && (focusOwner != FolderFocus.APP || selected in visibleIds)) focusOwner = owner
    }
    val navigation = remember { HomeNavigationMotion() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motion = rememberPanelTransition(reduced)
    var awaitingRelease by remember { mutableStateOf(initiallyManaging) }
    fun close() {
        if (!saving) {
            when {
                moving != null -> onCancelMove()

                mode != FolderMode.BROWSE -> {
                    mode = FolderMode.BROWSE
                    returnToOptions = true
                }

                else -> motion.close(onClose)
            }
        }
    }
    FolderDialog(onDismissRequest = ::close) {
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (!ready) focusInitialized = false
        }
        Box(
            Modifier.fillMaxSize().padding(32.dp).onPreviewKeyEvent { event ->
                if (awaitingRelease && event.key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
                    if (event.type == KeyEventType.KeyUp) {
                        awaitingRelease = false
                        return@onPreviewKeyEvent true
                    }
                    if (event.nativeKeyEvent.repeatCount > 0) return@onPreviewKeyEvent true
                    awaitingRelease = false
                }
                if (
                    moving != null &&
                    event.key in
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
                    if (!saving) {
                        if (event.type == KeyEventType.KeyDown) {
                            val delta =
                                when (event.key) {
                                    Key.DirectionLeft -> if (rtl) 1 else -1
                                    Key.DirectionRight -> if (rtl) -1 else 1
                                    Key.DirectionUp -> -columns
                                    Key.DirectionDown -> columns
                                    else -> 0
                                }
                            if (delta != 0) {
                                draft =
                                    moveFavoriteApp(
                                        draft,
                                        moving,
                                        delta,
                                        installed.keys,
                                    )
                            }
                        }
                        if (
                            event.type == KeyEventType.KeyUp &&
                            event.key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)
                        ) {
                            onReorder(original, draft)
                        }
                    }
                    true
                } else {
                    false
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.fillMaxSize()
                    .panelMotion(motion, reduced)
                    .background(
                        DesignTokens.background.copy(alpha = if (LocalReducedTransparency.current) 1f else .82f),
                        RoundedCornerShape(32.dp),
                    )
                    .padding(horizontal = 40.dp, vertical = 28.dp)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("folder-panel"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val headerEnabled = !saving && moving == null && !motion.closing && mode != FolderMode.REMOVE
                FolderHeaderButton(
                    folder.name,
                    {
                        focusOwner = FolderFocus.TITLE
                        onRename()
                    },
                    Modifier.align(Alignment.CenterHorizontally).widthIn(max = 640.dp).fillMaxWidth()
                        .focusRequester(rename)
                        .focusProperties { down = if (onAddApps != null) add else options }
                        .onFocusChanged { if (it.isFocused) selectHeader(FolderFocus.TITLE) }
                        .testTag("folder-rename"),
                    enabled = headerEnabled,
                    reduced = reduced,
                    title = true,
                )
                Text(
                    pluralStringResource(R.plurals.folder_member_count, visible.size, visible.size),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = DesignTokens.secondary,
                )
                Row(
                    Modifier.align(Alignment.CenterHorizontally).widthIn(max = 560.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (onAddApps != null) {
                        FolderHeaderButton(
                            stringResource(R.string.folder_add_apps),
                            {
                                focusOwner = FolderFocus.ADD
                                onAddApps()
                            },
                            Modifier.weight(1f).focusRequester(add)
                                .focusProperties { up = rename }
                                .onFocusChanged { if (it.isFocused) selectHeader(FolderFocus.ADD) }
                                .testTag("folder-add"),
                            enabled = headerEnabled,
                            reduced = reduced,
                        )
                    }
                    FolderHeaderButton(
                        stringResource(R.string.folder_options),
                        { mode = if (mode == FolderMode.BROWSE) FolderMode.OPTIONS else FolderMode.BROWSE },
                        Modifier.weight(1f).focusRequester(options)
                            .focusProperties { up = rename }
                            .onFocusChanged { if (it.isFocused) selectHeader(FolderFocus.OPTIONS) }
                            .testTag("folder-options"),
                        enabled = headerEnabled,
                        reduced = reduced,
                    )
                }
                if (moving != null) {
                    Text(
                        stringResource(
                            R.string.move_favorite_position,
                            visible.firstOrNull { it.id == moving }?.label.orEmpty(),
                            visible.indexOfFirst { it.id == moving } + 1,
                            visible.size,
                        ),
                        color = DesignTokens.foreground,
                    )
                    Text(
                        stringResource(if (saving) R.string.saving else R.string.move_folder_hint),
                        color = DesignTokens.foreground,
                    )
                }
                if (failed) {
                    Text(
                        stringResource(R.string.organization_write_error),
                        color = DesignTokens.foreground,
                    )
                }
                if (mode == FolderMode.REMOVE) {
                    Text(stringResource(R.string.remove_folder_scope, folder.name), color = DesignTokens.foreground)
                    SettingsRow(
                        stringResource(R.string.cancel),
                        {
                            mode = FolderMode.BROWSE
                            returnToOptions = true
                        },
                        Modifier.fillMaxWidth().focusRequester(cancelRemove).testTag("folder-remove-cancel"),
                        enabled = !saving,
                        reduced = reduced,
                    )
                    SettingsRow(
                        stringResource(R.string.remove_folder),
                        onDelete,
                        Modifier.fillMaxWidth().testTag("folder-remove-confirm"),
                        enabled = !saving,
                        reduced = reduced,
                    )
                } else if (mode == FolderMode.OPTIONS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsRow(
                            stringResource(R.string.remove_folder),
                            { mode = FolderMode.REMOVE },
                            Modifier.weight(1f).focusRequester(remove).testTag("folder-delete"),
                            enabled = !saving && moving == null && !motion.closing && mode != FolderMode.REMOVE,
                            compact = true,
                            reduced = reduced,
                        )
                        SettingsRow(
                            stringResource(R.string.cancel),
                            {
                                mode = FolderMode.BROWSE
                                returnToOptions = true
                            },
                            Modifier.weight(1f).testTag("folder-options-close"),
                            enabled = !saving,
                            compact = true,
                            reduced = reduced,
                        )
                    }
                }
                if (mode == FolderMode.REMOVE) {
                    if (saving) Text(stringResource(R.string.saving), color = DesignTokens.secondary)
                } else if (visible.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.folder_empty),
                                color = DesignTokens.foreground,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                            )
                            if (onAddApps != null) {
                                Text(
                                    stringResource(R.string.folder_empty_hint),
                                    color = DesignTokens.secondary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        GridCells.Adaptive(156.dp),
                        state = grid,
                        modifier = Modifier.weight(1f, false).testTag("folder-apps"),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(
                            visible,
                            key = { _, app -> app.id },
                            contentType = { _, _ -> "app" },
                        ) { index, app ->
                            val requester = remember(app.id) { FocusRequester() }
                            DisposableEffect(app.id) {
                                requesters[app.id] = requester
                                onDispose { requesters.remove(app.id) }
                            }
                            Box(
                                Modifier.animateItem(
                                    fadeInSpec = null,
                                    fadeOutSpec = null,
                                    placementSpec =
                                        if (reduced || moving == null) {
                                            androidx.compose.animation.core.snap()
                                        } else {
                                            androidx.compose.animation.core.tween(210)
                                        },
                                ),
                            ) {
                                AppTile(
                                    app,
                                    requester,
                                    reduced,
                                    HomeGridSlot(
                                        index,
                                        index % columns == 0,
                                        index % columns == columns - 1 ||
                                            index == visible.lastIndex,
                                    ),
                                    navigation,
                                    onFocus = {
                                        if (focusInitialized || selected == null) {
                                            focusOwner = FolderFocus.APP
                                            selected = app.id
                                            previousIndex = index
                                        }
                                    },
                                    onClick = { if (!saving && !motion.closing) onOpen(app) },
                                    onUp = if (index < columns) ({ rename.requestFocus() }) else null,
                                    onMenu = { bounds ->
                                        selected = app.id
                                        if (!saving && !motion.closing) onMenu(app, bounds, true)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(mode, ready) {
            if (ready) {
                withFrameNanos {}
                when {
                    mode == FolderMode.REMOVE -> cancelRemove.requestFocus()

                    mode == FolderMode.OPTIONS -> remove.requestFocus()

                    returnToOptions -> {
                        options.requestFocus()
                        returnToOptions = false
                    }

                    else -> return@LaunchedEffect
                }
                focusInitialized = true
            }
        }
        // Read the remembered target only when restoration is necessary. A surviving app or
        // header keeps native focus when another package is added/removed in the background.
        var restoredResume by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(visibleIds, resume, ready, moving) {
            if (!ready || mode != FolderMode.BROWSE) return@LaunchedEffect
            val restore = restoredResume != resume || !focusInitialized
            restoredResume = resume
            if (moving == null && !restore &&
                (focusOwner != FolderFocus.APP || selected in visibleIds)
            ) {
                return@LaunchedEffect
            }
            withFrameNanos {}
            if (moving == null && focusOwner != FolderFocus.APP) {
                when (focusOwner) {
                    FolderFocus.TITLE -> rename.requestFocus()
                    FolderFocus.OPTIONS -> options.requestFocus()
                    FolderFocus.ADD -> if (onAddApps != null) add.requestFocus() else rename.requestFocus()
                    FolderFocus.APP -> Unit
                }
                focusInitialized = true
                return@LaunchedEffect
            }
            val target = moving ?: restoreAppId(selected, previousIndex, visible)
            val index = visibleIds.indexOf(target)
            if (index < 0) {
                focusOwner = FolderFocus.TITLE
                rename.requestFocus()
            } else {
                if (grid.layoutInfo.visibleItemsInfo.none { it.key == target }) grid.scrollToItem(index)
                val requester = awaitRegisteredFocus { requesters[target] }
                if (requester != null) {
                    selected = target
                    previousIndex = index
                    focusOwner = FolderFocus.APP
                    requester.requestFocus()
                } else {
                    focusOwner = FolderFocus.TITLE
                    rename.requestFocus()
                }
            }
            focusInitialized = true
        }
        LaunchedEffect(moving, catalog) {
            if (moving != null && catalog.none { it.id == moving }) onCancelMove()
        }
    }
}

/** Native Dialogs install their own Android density/resources; keep the caller's TV text scale. */
@Composable
internal fun FolderDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val resources = LocalResources.current
    val direction = LocalLayoutDirection.current
    GlassDialog(onDismissRequest) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalResources provides resources,
            LocalLayoutDirection provides direction,
            content = content,
        )
    }
}

@Composable
private fun FolderMenuSurface(sourceKey: Any?, reduced: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(26.dp))) {
        RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
        content()
    }
}

/** One explicit transfer at a time; accepted writes remain owned by the route/ViewModel. */
@Composable
fun FolderAddAppsPanel(
    folder: AppFolder,
    folders: List<AppFolder>,
    catalog: List<TvApp>,
    saving: Boolean,
    failed: Boolean,
    reduced: Boolean,
    onAdd: (TvApp) -> Unit,
    onClose: () -> Unit,
    sourceKey: Any? = null,
) {
    val members = remember(folder.apps) { folder.apps.toSet() }
    val candidates = remember(catalog, members) { catalog.filterNot { it.id in members } }
    val locations = remember(folders) {
        buildMap { folders.forEach { group -> group.apps.forEach { put(it, group.name) } } }
    }
    val first = remember { FocusRequester() }
    val motion = rememberPanelTransition(reduced)
    fun close() {
        if (!saving) motion.close(onClose)
    }
    FolderDialog(onDismissRequest = ::close) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            FolderMenuSurface(sourceKey, reduced, Modifier.width(480.dp).heightIn(max = 440.dp).panelMotion(motion, reduced)) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp)
                        .semantics { testTagsAsResourceId = true }.testTag("folder-add-panel"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.folder_add_to, folder.name), color = DesignTokens.foreground, fontSize = 22.sp)
                    Text(stringResource(R.string.folder_add_hint), color = DesignTokens.secondary, fontSize = 14.sp)
                    if (failed) Text(stringResource(R.string.organization_write_error), color = DesignTokens.foreground)
                    LazyColumn(
                        Modifier.weight(1f, false).focusRequester(first).focusGroup(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(candidates, key = { it.id }, contentType = { "app-destination" }) { app ->
                            SettingsRow(
                                app.label,
                                { onAdd(app) },
                                Modifier.fillMaxWidth().testTag("folder-add:${app.id}"),
                                value = locations[app.id] ?: stringResource(R.string.home_screen),
                                enabled = !saving && !motion.closing,
                                reduced = reduced,
                                compact = true,
                            )
                        }
                    }
                    if (candidates.isEmpty()) Text(stringResource(R.string.folder_all_added), color = DesignTokens.secondary)
                    SettingsRow(
                        stringResource(R.string.close),
                        ::close,
                        Modifier.fillMaxWidth().then(if (candidates.isEmpty()) Modifier.focusRequester(first) else Modifier)
                            .testTag("folder-add-close"),
                        enabled = !saving && !motion.closing,
                        reduced = reduced,
                        compact = true,
                    )
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                first.requestFocus()
            }
        }
    }
}

/** Shared local naming/transfer form; no package operation happens in this UI. */
@Composable
fun FolderDestinationPanel(
    folders: List<AppFolder>,
    current: String?,
    saving: Boolean,
    failed: Boolean,
    onMove: (String) -> Unit,
    onCreate: (String) -> Unit,
    onClose: () -> Unit,
    reduced: Boolean = true,
    sourceKey: Any? = null,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    FolderForm(
        stringResource(R.string.move_to_folder),
        saving,
        failed,
        { if (creating) creating = false else onClose() },
        editingName = creating,
        reduced = reduced,
        sourceKey = sourceKey,
    ) {
        if (creating) {
            FolderNameField(name, { name = it }, enabled = !saving)
        } else {
            folders
                .filterNot { it.id == current }
                .forEach { folder ->
                    SettingsRow(
                        folder.name,
                        { onMove(folder.id) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                        compact = true,
                    )
                }
        }
        SettingsRow(
            stringResource(R.string.create_folder),
            { if (creating) onCreate(name) else creating = true },
            Modifier.fillMaxWidth().testTag("folder-create"),
            enabled = !saving && (!creating || name.isNotBlank()),
            compact = true,
        )
    }
}

@Composable
fun FolderRenamePanel(
    folder: AppFolder,
    saving: Boolean,
    failed: Boolean,
    onSave: (String) -> Unit,
    onClose: () -> Unit,
    reduced: Boolean = true,
    sourceKey: Any? = null,
) {
    var name by rememberSaveable(folder.id) { mutableStateOf(folder.name) }
    FolderForm(stringResource(R.string.rename_folder), saving, failed, onClose, reduced = reduced, sourceKey = sourceKey) {
        FolderNameField(name, { name = it }, enabled = !saving)
        SettingsRow(
            stringResource(R.string.apply),
            { onSave(name) },
            Modifier.fillMaxWidth().testTag("folder-save"),
            enabled = !saving && name.isNotBlank(),
            compact = true,
        )
    }
}

@Composable
private fun FolderForm(
    title: String,
    saving: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    editingName: Boolean = false,
    reduced: Boolean = true,
    sourceKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    val motion = rememberPanelTransition(reduced)
    fun close() {
        if (!saving) {
            if (editingName) onClose() else motion.close(onClose)
        }
    }
    FolderDialog(onDismissRequest = ::close) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            FolderMenuSurface(sourceKey, reduced, Modifier.width(400.dp).heightIn(max = 430.dp).panelMotion(motion, reduced)) {
                Column(
                    Modifier.fillMaxWidth().focusRequester(initialFocus).focusGroup()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .semantics { testTagsAsResourceId = true }
                        .testTag("folder-form"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, color = DesignTokens.foreground, fontSize = 22.sp)
                    content()
                    if (failed) {
                        Text(
                            stringResource(R.string.organization_write_error),
                            color = DesignTokens.foreground,
                        )
                    }
                    SettingsRow(
                        stringResource(R.string.cancel),
                        ::close,
                        Modifier.fillMaxWidth().testTag("folder-form-cancel"),
                        enabled = !saving,
                        compact = true,
                    )
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready, editingName) {
            if (ready) {
                withFrameNanos {}
                initialFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun FolderNameField(name: String, onName: (String) -> Unit, enabled: Boolean) {
    val label = stringResource(R.string.folder_name)
    var focused by remember { mutableStateOf(false) }
    Text(label, color = DesignTokens.secondary, fontSize = 14.sp)
    BasicTextField(
        name,
        { if (it.length <= 40 && it.none(Char::isISOControl)) onName(it) },
        Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(2.dp, if (focused) DesignTokens.accent else DesignTokens.secondary.copy(alpha = .3f), RoundedCornerShape(12.dp))
            .background(DesignTokens.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics { contentDescription = label }
            .testTag("folder-name"),
        textStyle = TextStyle(color = DesignTokens.foreground, fontSize = 18.sp),
        singleLine = true,
        enabled = enabled,
        cursorBrush = SolidColor(DesignTokens.foreground),
    )
}
