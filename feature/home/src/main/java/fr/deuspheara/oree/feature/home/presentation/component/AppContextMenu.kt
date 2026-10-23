@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.DestructiveRow
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.platform.image.WallpaperFrame

/** Dialog owns focus and Back; anchor is a transient rectangle, never persisted. */
@Composable
fun AppContextMenu(
    label: String,
    anchor: Rect,
    favorite: Boolean,
    saving: Boolean,
    failed: Boolean,
    onOpen: () -> Unit,
    onFavorite: (Boolean) -> Unit,
    onClose: () -> Unit,
    awaitingOpeningRelease: Boolean = false,
    reduceMotion: Boolean = true,
    dismissRequested: Boolean = false,
    reduceTransparency: Boolean = false,
    backdrop: WallpaperFrame? = null,
    onUninstall: (() -> Unit)? = null,
    onReorder: (() -> Unit)? = null,
    onAppInfo: (() -> Unit)? = null,
    onMoveToFolder: (() -> Unit)? = null,
    onMoveHome: (() -> Unit)? = null,
    reorderLabel: String? = null,
    canAddFavorite: Boolean = true,
) {
    var confirmUninstall by remember { mutableStateOf(false) }
    val cancelUninstall = remember { FocusRequester() }
    val uninstall = remember { FocusRequester() }
    var restoreUninstall by remember { mutableStateOf(false) }
    var awaitingRelease by remember { mutableStateOf(awaitingOpeningRelease) }
    val motion = rememberPanelTransition(reduceMotion)
    fun close(action: () -> Unit = onClose) = motion.close(action)
    LaunchedEffect(dismissRequested) { if (dismissRequested) close() }
    val first = remember { FocusRequester() }
    GlassDialog(
        onDismissRequest = {
            if (!saving) {
                if (confirmUninstall) confirmUninstall = false else close()
            }
        },
    ) {
        val menuSource = rememberHazeState()
        CompositionLocalProvider(
            LocalGlassSource provides
                (if (backdrop != null) menuSource else LocalGlassSource.current),
        ) {
            Box(Modifier.fillMaxSize()) {
                if (backdrop != null && !reduceTransparency && LocalLiquidGlass.current) {
                    androidx.compose.foundation.Image(
                        backdrop.frost.asImageBitmap(),
                        null,
                        Modifier.fillMaxSize()
                            .drawWithContent {
                                // Record the one-shot scene for Haze, but never paint its
                                // low-resolution pixels
                                // over the real Activity. The inner Haze layer records on its own
                                // canvas.
                                clipRect(0f, 0f, 0f, 0f) { this@drawWithContent.drawContent() }
                            }
                            .hazeSource(menuSource),
                        contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                    )
                }
                BoxWithConstraints(
                    Modifier.fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            val native = event.nativeKeyEvent
                            if (
                                awaitingRelease &&
                                native.keyCode in
                                listOf(
                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                    android.view.KeyEvent.KEYCODE_ENTER,
                                    android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                                )
                            ) {
                                if (native.action == android.view.KeyEvent.ACTION_UP) {
                                    awaitingRelease = false
                                    true
                                } else if (native.repeatCount > 0) {
                                    true
                                } else {
                                    awaitingRelease = false
                                    false
                                } // A fresh press after a semantic long-click.
                            } else {
                                false
                            }
                        }
                        .padding(28.dp),
                ) {
                    val density = LocalDensity.current
                    var menuHeight by remember { mutableIntStateOf(0) }
                    val height = with(density) { menuHeight.toDp() }
                    val width = minOf(320.dp, maxWidth)
                    // Prefer beside the source; clamp to safe bounds for edge/offscreen tiles.
                    val direction = LocalLayoutDirection.current
                    val left =
                        (
                            if (direction == LayoutDirection.Ltr) {
                                with(density) { anchor.right.toDp() } + 12.dp
                            } else {
                                with(density) { anchor.left.toDp() } - width - 12.dp
                            }
                            ) - 28.dp
                    val top = with(density) { anchor.top.toDp() } - 28.dp
                    Column(
                        Modifier.absoluteOffset(
                            x = left.coerceIn(0.dp, maxWidth - width),
                            y = top.coerceIn(0.dp, (maxHeight - height).coerceAtLeast(0.dp)),
                        )
                            .panelMotion(motion, reduceMotion)
                            .width(width)
                            .heightIn(max = maxHeight)
                            .onSizeChanged { menuHeight = it.height }
                            .menuSurface(opaque = reduceTransparency)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                            .semantics { testTagsAsResourceId = true }
                            .testTag("app-menu"),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = D.foreground,
                            fontSize = 22.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (confirmUninstall) {
                            Text(
                                stringResource(R.string.uninstall_confirmation, label),
                                color = D.foreground,
                                modifier = Modifier.padding(12.dp),
                            )
                            SettingsRow(
                                stringResource(R.string.cancel),
                                { confirmUninstall = false },
                                Modifier.fillMaxWidth()
                                    .focusRequester(cancelUninstall)
                                    .testTag("uninstall-cancel"),
                                compact = true,
                            )
                            DestructiveRow(
                                stringResource(R.string.uninstall_app),
                                { close { onUninstall?.invoke() } },
                                Modifier.fillMaxWidth().testTag("uninstall-confirm"),
                                !saving && !motion.closing,
                            )
                        } else {
                            SettingsRow(
                                stringResource(R.string.open_app),
                                { close(onOpen) },
                                Modifier.fillMaxWidth().focusRequester(first).testTag("menu-open"),
                                symbol = LauncherSymbol.OPEN,
                                enabled = !saving && !motion.closing,
                                reduced = reduceMotion,
                                compact = true,
                            )
                            SettingsRow(
                                stringResource(
                                    if (favorite) {
                                        R.string.remove_favorite
                                    } else {
                                        R.string.add_favorite
                                    },
                                ),
                                { onFavorite(!favorite) },
                                Modifier.fillMaxWidth()
                                    .testTag(
                                        if (favorite) {
                                            "menu-favorite-remove"
                                        } else {
                                            "menu-favorite-add"
                                        },
                                    ),
                                symbol = LauncherSymbol.FAVORITE,
                                enabled = !saving && !motion.closing && (favorite || canAddFavorite),
                                reduced = reduceMotion,
                                compact = true,
                            )
                            if (!favorite && !canAddFavorite) {
                                Text(stringResource(R.string.dock_full), color = D.secondary, modifier = Modifier.padding(12.dp))
                            }
                            if (onReorder != null) {
                                SettingsRow(
                                    reorderLabel ?: stringResource(R.string.move_favorite),
                                    { close(onReorder) },
                                    Modifier.fillMaxWidth().testTag("menu-reorder"),
                                    enabled = !saving && !motion.closing,
                                    reduced = reduceMotion,
                                    compact = true,
                                )
                            }
                            if (onMoveToFolder != null) {
                                SettingsRow(
                                    stringResource(R.string.move_to_folder),
                                    { close(onMoveToFolder) },
                                    Modifier.fillMaxWidth().testTag("menu-folder"),
                                    enabled = !saving && !motion.closing,
                                    reduced = reduceMotion,
                                    compact = true,
                                )
                            }
                            if (onMoveHome != null) {
                                SettingsRow(
                                    stringResource(R.string.move_to_home),
                                    { close(onMoveHome) },
                                    Modifier.fillMaxWidth().testTag("menu-home"),
                                    enabled = !saving && !motion.closing,
                                    reduced = reduceMotion,
                                    compact = true,
                                )
                            }
                            if (onAppInfo != null) {
                                SettingsRow(
                                    stringResource(R.string.app_info),
                                    { close(onAppInfo) },
                                    Modifier.fillMaxWidth().testTag("menu-app-info"),
                                    enabled = !saving && !motion.closing,
                                    reduced = reduceMotion,
                                    compact = true,
                                )
                            }
                            if (onUninstall != null) {
                                SettingsRow(
                                    stringResource(R.string.uninstall_app),
                                    {
                                        restoreUninstall = true
                                        confirmUninstall = true
                                    },
                                    Modifier.fillMaxWidth().focusRequester(uninstall).testTag("menu-uninstall"),
                                    enabled = !saving && !motion.closing,
                                    reduced = reduceMotion,
                                    compact = true,
                                )
                            }
                            if (failed) {
                                Text(
                                    stringResource(R.string.organization_write_error),
                                    color = D.foreground,
                                )
                            }
                            SettingsRow(
                                stringResource(R.string.close),
                                { close() },
                                Modifier.fillMaxWidth().testTag("menu-close"),
                                symbol = LauncherSymbol.CLOSE,
                                enabled = !saving && !motion.closing,
                                reduced = reduceMotion,
                                compact = true,
                            )
                        }
                    }
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(confirmUninstall, ready) {
            if (ready) {
                withFrameNanos {}
                when {
                    confirmUninstall -> cancelUninstall.requestFocus()
                    restoreUninstall && onUninstall != null -> uninstall.requestFocus()
                    else -> first.requestFocus()
                }
            }
        }
    }
}
