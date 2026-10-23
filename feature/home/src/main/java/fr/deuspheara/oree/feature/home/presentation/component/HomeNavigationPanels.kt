package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.folderApps
import fr.deuspheara.oree.feature.home.presentation.state.HomeFocusState
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot

internal enum class HomeSection(val title: Int) {
    CONTINUATION(R.string.continue_watching),
    FAVORITES(R.string.favorites),
    FOLDERS(R.string.folders),
    APPS(R.string.apps),
}

/** Compact section map; focus never leaves the native modal before an explicit selection. */
@Composable
internal fun HomeOverview(
    sections: Map<HomeSection, Int>,
    reduced: Boolean,
    sourceKey: Any?,
    onSelect: (HomeSection) -> Unit,
    onClose: () -> Unit,
) {
    val motion = rememberPanelTransition(reduced)
    val first = remember { FocusRequester() }
    FolderDialog({ motion.close(onClose) }) {
        Box(Modifier.fillMaxWidth().padding(52.dp).semantics { testTagsAsResourceId = true }, contentAlignment = Alignment.Center) {
            Box(Modifier.width(560.dp).panelMotion(motion, reduced).testTag("home-overview")) {
                RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.home_overview), color = DesignTokens.foreground, fontSize = 28.sp)
                    sections.entries.forEachIndexed { index, (section, count) ->
                        androidx.compose.runtime.key(section) {
                            SettingsRow(
                                stringResource(section.title),
                                { motion.close { onSelect(section) } },
                                Modifier.fillMaxWidth().then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                                    .testTag("overview:${section.name}"),
                                value = count.toString(),
                                reduced = reduced,
                            )
                        }
                    }
                    SettingsRow(
                        stringResource(R.string.close),
                        { motion.close(onClose) },
                        Modifier.then(if (sections.isEmpty()) Modifier.focusRequester(first) else Modifier),
                        reduced = reduced,
                    )
                }
            }
        }
        val ready = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                first.requestFocus()
            }
        }
    }
}

/** Explicit lightweight entry to a folder, with the full-folder destination still available. */
@Composable
internal fun FolderPeek(
    folder: AppFolder,
    apps: List<TvApp>,
    focus: HomeFocusState,
    reduced: Boolean,
    sourceKey: Any?,
    onSelect: (TvApp) -> Unit,
    onOpenFolder: () -> Unit,
    onManage: () -> Unit,
    onClose: () -> Unit,
) {
    val members = remember(folder, apps) { folderApps(folder, apps) }
    val motion = rememberPanelTransition(reduced)
    val open = remember { FocusRequester() }
    val enter = remember { FocusRequester() }
    val manage = remember { FocusRequester() }
    var headerTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var memberTarget by rememberSaveable(folder.id) { mutableStateOf(focus.peekSelection[folder.id]) }
    var entered by rememberSaveable { mutableStateOf(false) }
    val unfold = remember { Animatable(if (reduced) 1f else 0f) }
    val unfoldStart = if (androidx.compose.ui.platform.LocalLayoutDirection.current ==
        androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        1f
    } else {
        0f
    }
    LaunchedEffect(entered, reduced) {
        if (reduced) unfold.snapTo(1f) else unfold.animateTo(if (entered) 1f else 0f, tween(180))
    }
    var awaitingRelease by remember { mutableStateOf(true) }
    val generation = remember(entered, members) { entered to members.map { it.id } }
    var restoredGeneration by remember { mutableStateOf<Pair<Boolean, List<String>>?>(null) }
    FolderDialog({ motion.close(onClose) }) {
        val ready = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
        Box(
            Modifier.fillMaxWidth().padding(52.dp).semantics { testTagsAsResourceId = true }.onPreviewKeyEvent { event ->
                if (event.key == Key.Menu) return@onPreviewKeyEvent true
                if (awaitingRelease && event.key in listOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)) {
                    if (event.type == KeyEventType.KeyUp) {
                        awaitingRelease = false
                        true
                    } else if (event.nativeKeyEvent.repeatCount > 0) {
                        true
                    } else {
                        awaitingRelease = false
                        false
                    }
                } else {
                    false
                }
            },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(Modifier.fillMaxWidth().panelMotion(motion, reduced).testTag("folder-peek")) {
                RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
                Column(
                    Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(folder.name, color = DesignTokens.foreground, fontSize = 26.sp)
                    if (members.isEmpty()) Text(stringResource(R.string.folder_empty), color = DesignTokens.secondary)
                    if (!entered) {
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            members.take(4).forEach { app -> Box(Modifier.size(40.dp)) { FolderPreview(app) } }
                        }
                        SettingsRow(
                            stringResource(R.string.folder_peek),
                            {
                                restoredGeneration = null
                                headerTarget = null
                                entered = true
                            },
                            Modifier.focusRequester(enter).onFocusChanged {
                                if (it.isFocused && ready && restoredGeneration == generation) headerTarget = "enter"
                            }.testTag("peek-enter"),
                            reduced = reduced,
                            enabled = members.isNotEmpty(),
                        )
                    } else {
                        LazyRow(
                            state = focus.peekRow,
                            modifier = Modifier.graphicsLayer {
                                val amount = if (reduced) 1f else unfold.value * motion.progress.value
                                alpha = amount
                                scaleX = .75f + .25f * amount
                                scaleY = scaleX
                                transformOrigin = TransformOrigin(unfoldStart, .5f)
                            },
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            itemsIndexed(members, key = { _, app -> app.id }, contentType = { _, _ -> "peek-app" }) { index, app ->
                                val requester = remember(app.id) { FocusRequester() }
                                DisposableEffect(app.id) {
                                    focus.peekRequesters[app.id] = requester
                                    onDispose { focus.peekRequesters.remove(app.id) }
                                }
                                Box(Modifier.width(156.dp)) {
                                    AppTile(
                                        app,
                                        requester,
                                        reduced,
                                        HomeGridSlot(index, index == 0, index == members.lastIndex),
                                        focus.navigation,
                                        onFocus = {
                                            if (ready && restoredGeneration == generation) {
                                                headerTarget = null
                                                memberTarget = app.id
                                                focus.peekSelection[folder.id] = app.id
                                            }
                                        },
                                        onClick = { onSelect(app) },
                                        onMenu = null,
                                    )
                                }
                            }
                        }
                    }
                    SettingsRow(
                        stringResource(R.string.folder_options),
                        { motion.close(onManage) },
                        Modifier.focusRequester(manage).onFocusChanged {
                            if (it.isFocused && ready &&
                                restoredGeneration == generation
                            ) {
                                headerTarget = "manage"
                            }
                        },
                        reduced = reduced,
                    )
                    SettingsRow(
                        stringResource(R.string.folder_peek_open),
                        { motion.close(onOpenFolder) },
                        Modifier.focusRequester(open).onFocusChanged {
                            if (it.isFocused && ready && restoredGeneration == generation) headerTarget = "open"
                        }.testTag("peek-open-folder"),
                        reduced = reduced,
                    )
                }
            }
        }
        LaunchedEffect(ready, generation) {
            restoredGeneration = null
            if (!ready) return@LaunchedEffect
            val index = members.indexOfFirst { it.id == memberTarget }.coerceAtLeast(0)
            if (headerTarget == "open" || headerTarget == "manage") {
                withFrameNanos {}
                if (headerTarget == "open") open.requestFocus() else manage.requestFocus()
            } else if (!entered && members.isNotEmpty()) {
                withFrameNanos {}
                enter.requestFocus()
                headerTarget = "enter"
            } else if (members.isNotEmpty()) {
                focus.peekRow.scrollToItem(index)
                withFrameNanos {}
                val target = awaitRegisteredFocus { focus.peekRequesters[members[index].id] }
                if (target != null) {
                    target.requestFocus()
                    memberTarget = members[index].id
                    focus.peekSelection[folder.id] = members[index].id
                    headerTarget = null
                } else {
                    open.requestFocus()
                    headerTarget = "open"
                }
            } else {
                withFrameNanos {}
                open.requestFocus()
                headerTarget = "open"
            }
            restoredGeneration = generation
        }
    }
}

/** Focus reveals a bounded, non-interactive preview; Held Select or Menu offers an explicit Peek action. */
@Composable
internal fun FolderQuickPreview(folder: AppFolder, apps: List<TvApp>, reduced: Boolean, modifier: Modifier = Modifier) {
    val members = remember(folder, apps) { folderApps(folder, apps).take(4) }
    val motion = rememberPanelTransition(reduced)
    androidx.compose.foundation.layout.Row(
        modifier.panelMotion(motion, reduced)
            .background(DesignTokens.background.copy(alpha = .92f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(12.dp).testTag("folder-peek-preview"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        members.forEach { app ->
            Box(Modifier.size(32.dp)) { FolderPreview(app) }
        }
    }
}
