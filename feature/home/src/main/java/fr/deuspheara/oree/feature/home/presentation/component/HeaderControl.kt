package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.designsystem.avatar.ProfileAvatar
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.glass.glassControlForeground
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.feature.home.presentation.component.ArtworkPhase
import fr.deuspheara.oree.feature.home.presentation.component.FolderTile
import fr.deuspheara.oree.feature.home.presentation.component.HomeArtworkPlaceholder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HeaderControl(
    label: String,
    symbol: LauncherSymbol,
    modifier: Modifier,
    reduced: Boolean,
    onFocus: () -> Unit,
    end: Boolean = false,
    avatar: String? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val visualScale =
        androidx.compose.animation.core.animateFloatAsState(
            if (reduced) 1f else (if (focused) 1.10f else .82f) * (if (pressed) .975f else 1f),
            androidx.compose.animation.core.spring(dampingRatio = .82f, stiffness = 550f),
            label = "header-size",
        )
    val focusRing = reduced || LocalHighContrast.current
    val ringColor = D.accent
    Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            interactionSource = interactions,
            modifier =
                modifier
                    .size(40.dp)
                    .drawWithContent {
                        scale(visualScale.value, visualScale.value) {
                            this@drawWithContent.drawContent()
                        }
                        if (focused && focusRing) {
                            drawCircle(
                                ringColor,
                                radius = size.minDimension / 2 - 1.dp.toPx(),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                            )
                        }
                    }
                    .glassSurface(radius = 20.dp)
                    .focusProperties { if (end) this.end = FocusRequester.Cancel }
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onFocus()
                    }
                    .semantics { contentDescription = label },
            border =
                ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None,
                    pressedBorder = Border.None,
                ),
            shape = ButtonDefaults.shape(shape = CircleShape),
            contentPadding = PaddingValues(if (avatar != null) 0.dp else 8.dp),
            colors =
                ButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = D.foreground,
                    focusedContainerColor = D.foreground,
                    focusedContentColor = D.background,
                ),
            scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        ) {
            if (avatar != null) {
                ProfileAvatar(avatar, Modifier.size(40.dp))
            } else {
                LauncherIcon(symbol, if (focused) D.background else glassControlForeground())
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}
