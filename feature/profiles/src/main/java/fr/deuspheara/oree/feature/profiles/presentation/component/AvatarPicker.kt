package fr.deuspheara.oree.feature.profiles.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.avatar.AvatarAsset
import fr.deuspheara.oree.core.designsystem.avatar.AvatarCatalog
import fr.deuspheara.oree.core.designsystem.avatar.ProfileAvatar
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import kotlinx.coroutines.delay

@Composable
fun AvatarPicker(selectedId: String, reduced: Boolean, enabled: Boolean, entry: FocusRequester, onSelect: (String) -> Unit) {
    Text(stringResource(R.string.choose_avatar), color = DesignTokens.secondary)
    val initial = remember {
        AvatarCatalog.entries.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    }
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    LazyRow(
        state = scroll,
        modifier = Modifier.fillMaxWidth().height(96.dp).testTag("profile-avatar-row"),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        itemsIndexed(
            AvatarCatalog.entries,
            key = { _, asset -> asset.id },
            contentType = { _, _ -> "avatar" },
        ) { index, asset ->
            AvatarChoice(
                asset,
                asset.id == selectedId,
                reduced,
                enabled,
                index,
                if (asset.id == selectedId) entry else null,
                { onSelect(asset.id) },
            )
        }
    }
}

@Composable
private fun AvatarChoice(
    asset: AvatarAsset,
    selected: Boolean,
    reduced: Boolean,
    enabled: Boolean,
    column: Int,
    entry: FocusRequester?,
    onSelect: () -> Unit,
) {
    var focused by remember(asset.id) { mutableStateOf(false) }
    var tilt by remember(asset.id) { mutableStateOf(Offset.Zero) }
    val interactions = remember(asset.id) { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    LaunchedEffect(tilt, focused) {
        if (tilt != Offset.Zero) {
            delay(90)
            tilt = Offset.Zero
        }
    }
    val label = stringResource(asset.label)
    Box(
        Modifier.size(72.dp)
            .zIndex(if (focused) 1f else 0f)
            .focusProperties {
                start = if (column == 0) FocusRequester.Cancel else FocusRequester.Default
                end =
                    if (column == AvatarCatalog.entries.lastIndex) {
                        FocusRequester.Cancel
                    } else {
                        FocusRequester.Default
                    }
            }
            .then(if (entry != null) Modifier.focusRequester(entry) else Modifier)
            .onFocusChanged {
                focused = it.isFocused
                if (!it.isFocused) tilt = Offset.Zero
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !reduced) {
                    tilt =
                        when (event.key) {
                            Key.DirectionLeft -> Offset(-.65f, 0f)
                            Key.DirectionRight -> Offset(.65f, 0f)
                            Key.DirectionUp -> Offset(0f, -.65f)
                            Key.DirectionDown -> Offset(0f, .65f)
                            else -> Offset.Zero
                        }
                }
                false
            }
            .testTag("profile-avatar:${asset.id}")
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onSelect,
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        FocusArtwork(
            focused,
            pressed,
            reduced,
            tilt,
            Modifier.fillMaxSize(),
            shape = CircleShape,
            edgeHighlight = false,
            overlay = {
                if (selected) {
                    AvatarSelectionBadge(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp))
                }
            },
        ) {
            // FocusArtwork already supplies the circular clip; avoid doubled edge coverage.
            ProfileAvatar(asset.id, Modifier.fillMaxSize(), clipToCircle = false)
        }
    }
}

@Composable
internal fun AvatarSelectionBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .graphicsLayer {
                shape = CircleShape
                shadowElevation = 2.dp.toPx()
                clip = false
            }
            .background(Color(0xFF248A3D), CircleShape)
            .drawWithContent {
                drawContent()
                // Inset the highlight so no half-stroke is cut off at the circular edge.
                drawCircle(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = .35f), Color.Transparent),
                        Offset.Zero,
                        Offset(size.width, size.height),
                    ),
                    radius = size.minDimension / 2 - .5.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_hugeicons_tick_02),
            null,
            Modifier.size(14.dp),
            tint = Color.White,
        )
    }
}
