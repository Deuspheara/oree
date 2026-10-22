package fr.deuspheara.oree.feature.profiles.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.avatar.ProfileAvatar
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.panelSurface
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.GlassSurfaceRole
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.glass.glassControlForeground
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.AvatarIds
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.feature.profiles.presentation.component.AvatarPicker

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfilePanel(
    snapshot: ProfileSnapshot,
    reduced: Boolean,
    opaque: Boolean,
    saving: Boolean,
    failed: Boolean,
    onSelect: (String) -> Unit,
    onSave: (String?, String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    sourceKey: Any? = null,
) {
    val addLabel = stringResource(R.string.add_profile)
    val editLabel = stringResource(R.string.edit_profile)
    var awaitingRelease by remember { mutableStateOf(false) }
    val motion = rememberPanelTransition(reduced)
    var editing by rememberSaveable { mutableStateOf(false) }
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(AvatarIds.DEFAULT) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val editorFirst = remember { FocusRequester() }
    val keepVisible = remember {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
                offset < 0f -> offset
                offset + size > containerSize -> offset + size - containerSize
                else -> 0f
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides keepVisible) {
        GlassDialog(
            onDismissRequest = {
                if (!saving) {
                    if (editing) {
                        editing = false
                        confirmDelete = false
                    } else {
                        motion.close(onClose)
                    }
                }
            },
        ) {
            Box(
                Modifier.fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        val key = event.nativeKeyEvent
                        if (
                            awaitingRelease &&
                            key.keyCode in
                            listOf(
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER,
                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            )
                        ) {
                            if (key.action == android.view.KeyEvent.ACTION_UP) {
                                awaitingRelease = false
                                true
                            } else if (key.repeatCount > 0) {
                                true
                            } else {
                                awaitingRelease = false
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .padding(top = 82.dp, end = 32.dp, start = 32.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    Modifier.width(if (editing) 420.dp else 280.dp)
                        .heightIn(max = 440.dp)
                        .panelMotion(motion, reduced, 32f)
                        .testTag("profile-panel"),
                ) {
                    if (editing) {
                        CompositionLocalProvider(
                            LocalOpaquePanels provides (opaque || LocalOpaquePanels.current),
                        ) {
                            RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
                        }
                    } else if (opaque) {
                        Box(Modifier.matchParentSize().panelSurface(true))
                    }
                    Column(
                        Modifier.verticalScroll(rememberScrollState())
                            .padding(if (editing || opaque) 24.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (editing) {
                            Text(
                                stringResource(R.string.edit_profile),
                                color = DesignTokens.foreground,
                                fontSize = 24.sp,
                            )
                        }
                        if (!editing) {
                            LazyVerticalGrid(
                                GridCells.Fixed(2),
                                Modifier.heightIn(max = 300.dp),
                                contentPadding = PaddingValues(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                itemsIndexed(snapshot.profiles, key = { _, p -> p.id }) { index, p ->
                                    var focused by remember(p.id) { mutableStateOf(false) }
                                    val interactions = remember(p.id) { MutableInteractionSource() }
                                    val pressed by interactions.collectIsPressedAsState()
                                    Column(
                                        Modifier.zIndex(if (focused) 1f else 0f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Box(
                                            Modifier.size(96.dp)
                                                .then(
                                                    if (p.id == (target ?: snapshot.activeId)) {
                                                        Modifier.focusRequester(first)
                                                    } else {
                                                        Modifier
                                                    },
                                                )
                                                .onPreviewKeyEvent { event ->
                                                    if (index < 2 && event.key == Key.DirectionUp) {
                                                        if (
                                                            event.type == KeyEventType.KeyDown &&
                                                            !saving
                                                        ) {
                                                            motion.close(onClose)
                                                        }
                                                        true
                                                    } else {
                                                        false
                                                    }
                                                }
                                                .testTag("profile:${p.id}")
                                                .onFocusChanged { focused = it.isFocused }
                                                .semantics {
                                                    selected = p.id == snapshot.activeId
                                                    contentDescription = p.name
                                                }
                                                .combinedClickable(
                                                    enabled = !saving,
                                                    interactionSource = interactions,
                                                    indication = null,
                                                    role = Role.Button,
                                                    onClick = { onSelect(p.id) },
                                                    onLongClickLabel = editLabel,
                                                    onLongClick = {
                                                        target = p.id
                                                        name = p.name
                                                        avatar = p.avatar
                                                        confirmDelete = false
                                                        awaitingRelease = true
                                                        editing = true
                                                    },
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            FocusArtwork(
                                                focused,
                                                pressed,
                                                reduced,
                                                Offset.Zero,
                                                Modifier.fillMaxSize(),
                                                shape = CircleShape,
                                                edgeHighlight = false,
                                            ) {
                                                ProfileAvatar(
                                                    p.avatar,
                                                    Modifier.fillMaxSize(),
                                                    clipToCircle = false,
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            p.name,
                                            color =
                                                if (opaque || LocalOpaquePanels.current) {
                                                    DesignTokens.foreground
                                                } else {
                                                    Color.White
                                                },
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                item(key = "add") {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Button(
                                            onClick = {
                                                editing = true
                                                target = null
                                                name = ""
                                                avatar = AvatarIds.DEFAULT
                                            },
                                            enabled = !saving && snapshot.profiles.size < 12,
                                            modifier =
                                                Modifier.size(96.dp)
                                                    .glassSurface(radius = 48.dp)
                                                    .onPreviewKeyEvent { event ->
                                                        if (
                                                            snapshot.profiles.size < 2 &&
                                                            event.key == Key.DirectionUp
                                                        ) {
                                                            if (
                                                                event.type ==
                                                                KeyEventType.KeyDown && !saving
                                                            ) {
                                                                motion.close(onClose)
                                                            }
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }
                                                    .testTag("add-profile")
                                                    .semantics { contentDescription = addLabel },
                                            shape = ButtonDefaults.shape(shape = CircleShape),
                                            colors =
                                                ButtonDefaults.colors(
                                                    containerColor = Color.Transparent,
                                                    contentColor = glassControlForeground(),
                                                ),
                                            contentPadding = PaddingValues(12.dp),
                                        ) {
                                            Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("+", fontSize = 40.sp)
                                            }
                                        }
                                        Text(
                                            stringResource(R.string.add_profile),
                                            color =
                                                if (opaque || LocalOpaquePanels.current) {
                                                    DesignTokens.foreground
                                                } else {
                                                    Color.White
                                                },
                                            fontSize = 15.sp,
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                stringResource(R.string.profile_name),
                                color = DesignTokens.secondary,
                            )
                            BasicTextField(
                                name,
                                {
                                    if (!saving && it.length <= 40 && it.none(Char::isISOControl)) {
                                        name = it
                                    }
                                },
                                Modifier.fillMaxWidth()
                                    .glassSurface(
                                        opaque = opaque,
                                        radius = 1000.dp,
                                        role = GlassSurfaceRole.MENU,
                                    )
                                    .background(
                                        DesignTokens.foreground.copy(alpha = .08f),
                                        CircleShape,
                                    )
                                    .padding(16.dp)
                                    .testTag("profile-name"),
                                textStyle =
                                    TextStyle(color = DesignTokens.foreground, fontSize = 18.sp),
                                singleLine = true,
                                cursorBrush = SolidColor(DesignTokens.foreground),
                            )
                            AvatarPicker(avatar, reduced, !saving, editorFirst, { avatar = it })
                            SettingsRow(
                                stringResource(R.string.apply),
                                { onSave(target, name, avatar) },
                                Modifier.testTag("save-profile"),
                                enabled = !saving && name.isNotBlank(),
                            )
                            if (target != null && snapshot.profiles.size > 1) {
                                SettingsRow(
                                    stringResource(
                                        if (confirmDelete) {
                                            R.string.confirm_delete_profile
                                        } else {
                                            R.string.delete_profile
                                        },
                                    ),
                                    {
                                        if (confirmDelete) {
                                            onDelete(target!!)
                                        } else {
                                            confirmDelete = true
                                        }
                                    },
                                    Modifier.testTag("delete-profile"),
                                    enabled = !saving,
                                )
                            }
                        }
                        if (failed) {
                            Text(
                                stringResource(R.string.preferences_write_error),
                                color = DesignTokens.foreground,
                                modifier = Modifier.testTag("profile-error"),
                            )
                        }
                        if (editing) {
                            Text(
                                stringResource(R.string.profile_account_notice),
                                color = DesignTokens.secondary,
                                fontSize = 12.sp,
                            )
                        }
                        if (editing) {
                            SettingsRow(
                                stringResource(R.string.cancel),
                                {
                                    if (editing) {
                                        editing = false
                                        confirmDelete = false
                                    } else {
                                        motion.close(onClose)
                                    }
                                },
                                enabled = !saving,
                            )
                        }
                    }
                }
            }
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(editing, windowFocused) {
                if (windowFocused) {
                    withFrameNanos {}
                    if (editing) editorFirst.requestFocus() else first.requestFocus()
                }
            }
        }
    }
}
