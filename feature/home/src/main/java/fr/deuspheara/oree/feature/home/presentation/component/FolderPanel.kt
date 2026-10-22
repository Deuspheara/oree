package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.visible
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
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
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val previews = remember(folder, catalog) { folderApps(folder, catalog).take(4) }
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(vertical = 8.dp)) {
        FocusArtwork(
            focused,
            pressed,
            reduced,
            Offset.Zero,
            Modifier.fillMaxSize(),
            immediateFocus = true,
        ) {
            Column(
                Modifier.fillMaxSize().background(DesignTokens.surface).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(folder.name, color = DesignTokens.foreground, fontSize = 18.sp, maxLines = 1)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    previews.forEach { app ->
                        val context = LocalContext.current
                        val request =
                            remember(app.artworkCacheKey) {
                                ImageRequest.Builder(context)
                                    .data("android.resource://${app.packageName}/${app.artwork}")
                                    .memoryCacheKey(app.artworkCacheKey)
                                    .size(128, 72)
                                    .build()
                            }
                        if (app.artwork != 0) {
                            AsyncImage(request, null, Modifier.weight(1f).fillMaxHeight())
                        } else {
                            Text(
                                app.label.take(1),
                                color = DesignTokens.foreground,
                                modifier = Modifier.weight(1f),
                            )
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
                    if (it.key == Key.DirectionUp && onUp != null) {
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
                    onClick = onOpen,
                ),
        )
    }
}

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
) {
    val grid = rememberLazyGridState()
    val columns by remember { derivedStateOf { grid.layoutInfo.maxSpan.coerceAtLeast(1) } }
    val original = remember(moving) { folder.apps }
    var draft by remember(moving) { mutableStateOf(folder.apps) }
    val visible =
        remember(folder, catalog, draft, moving) {
            folderApps(if (moving != null) folder.copy(apps = draft) else folder, catalog)
        }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var selected by rememberSaveable(folder.id) { mutableStateOf<String?>(null) }
    var previousIndex by rememberSaveable(folder.id) { mutableIntStateOf(0) }
    val rename = remember { FocusRequester() }
    val navigation = remember { HomeNavigationMotion() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val motion = rememberPanelTransition(reduced)
    fun close() {
        if (!saving) {
            if (moving != null) onCancelMove() else motion.close(onClose)
        }
    }
    GlassDialog(onDismissRequest = ::close) {
        val ready = LocalWindowInfo.current.isWindowFocused
        Box(
            Modifier.fillMaxSize().padding(32.dp).onPreviewKeyEvent { event ->
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
                                        catalog.mapTo(hashSetOf()) { it.id },
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
                Modifier.widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .panelMotion(motion, reduced)
                    .menuSurface()
                    .padding(20.dp)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("folder-panel"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(folder.name, color = DesignTokens.foreground, fontSize = 24.sp)
                if (moving != null) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsRow(
                        stringResource(R.string.rename_folder),
                        onRename,
                        Modifier.weight(1f).focusRequester(rename).testTag("folder-rename"),
                        enabled = !saving && moving == null,
                        compact = true,
                    )
                    SettingsRow(
                        stringResource(R.string.remove_folder),
                        onDelete,
                        Modifier.weight(1f).testTag("folder-delete"),
                        enabled = !saving && moving == null,
                        compact = true,
                    )
                    SettingsRow(
                        stringResource(R.string.close),
                        ::close,
                        Modifier.weight(1f).testTag("folder-close"),
                        enabled = !saving,
                        compact = true,
                    )
                }
                if (visible.isEmpty()) {
                    Text(stringResource(R.string.folder_empty), color = DesignTokens.secondary)
                } else {
                    LazyVerticalGrid(
                        GridCells.Adaptive(140.dp),
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
                                        selected = app.id
                                        previousIndex = index
                                    },
                                    onClick = { onOpen(app) },
                                    onMenu = { bounds ->
                                        selected = app.id
                                        onMenu(app, bounds, true)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(visible.map { it.id }, resume, ready, moving) {
            if (!ready) return@LaunchedEffect
            val target = moving ?: restoreAppId(selected, previousIndex, visible)
            if (target == null) {
                withFrameNanos {}
                rename.requestFocus()
            } else {
                val index = visible.indexOfFirst { it.id == target }
                if (index >= 0) {
                    grid.scrollToItem(index)
                    withFrameNanos {}
                    val requester = awaitRegisteredFocus { requesters[target] }
                    if (requester != null) requester.requestFocus() else rename.requestFocus()
                }
            }
        }
        LaunchedEffect(moving, catalog) {
            if (moving != null && catalog.none { it.id == moving }) onCancelMove()
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
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    FolderForm(stringResource(R.string.move_to_folder), saving, failed, onClose) {
        if (creating) {
            FolderNameField(name, { name = it })
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
fun FolderRenamePanel(folder: AppFolder, saving: Boolean, failed: Boolean, onSave: (String) -> Unit, onClose: () -> Unit) {
    var name by rememberSaveable(folder.id) { mutableStateOf(folder.name) }
    FolderForm(stringResource(R.string.rename_folder), saving, failed, onClose) {
        FolderNameField(name, { name = it })
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
private fun FolderForm(title: String, saving: Boolean, failed: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val close = remember { FocusRequester() }
    GlassDialog(onDismissRequest = { if (!saving) onClose() }) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(400.dp)
                    .heightIn(max = 430.dp)
                    .menuSurface()
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
                    onClose,
                    Modifier.fillMaxWidth().focusRequester(close).testTag("folder-form-cancel"),
                    enabled = !saving,
                    compact = true,
                )
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                close.requestFocus()
            }
        }
    }
}

@Composable
private fun FolderNameField(name: String, onName: (String) -> Unit) {
    val label = stringResource(R.string.folder_name)
    BasicTextField(
        name,
        { if (it.length <= 40 && it.none(Char::isISOControl)) onName(it) },
        Modifier.fillMaxWidth()
            .background(DesignTokens.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics { contentDescription = label }
            .testTag("folder-name"),
        textStyle = TextStyle(color = DesignTokens.foreground, fontSize = 18.sp),
        singleLine = true,
        cursorBrush = SolidColor(DesignTokens.foreground),
    )
}
