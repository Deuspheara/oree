package fr.deuspheara.oree.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens

/** One interruptible modal timeline: closing cancels entry and dispatches only once. */
@Stable
class PanelTransition constructor(reduced: Boolean) {
    val progress = Animatable(if (reduced) 1f else 0f)
    private var action by mutableStateOf<(() -> Unit)?>(null)
    private var delivered = false
    val closing: Boolean
        get() = action != null

    fun close(onClosed: () -> Unit) {
        if (action == null) action = onClosed
    }

    fun dispatch() {
        if (!delivered) {
            delivered = true
            action?.invoke()
        }
    }
}

@Composable
fun rememberPanelTransition(reduced: Boolean): PanelTransition {
    val transition = remember { PanelTransition(reduced) }
    LaunchedEffect(transition.closing, reduced) {
        transition.progress.animateTo(
            if (transition.closing) 0f else 1f,
            tween(if (reduced) 0 else MotionTokens.PANEL_MILLIS, easing = FastOutSlowInEasing),
        )
        if (transition.closing) transition.dispatch()
    }
    return transition
}

fun Modifier.panelMotion(transition: PanelTransition, reduced: Boolean, horizontal: Float = 0f, minimumAlpha: Float = 0f): Modifier =
    graphicsLayer {
        val p = transition.progress.value
        alpha = minimumAlpha + (1f - minimumAlpha) * p
        if (!reduced) {
            scaleX = .96f + .04f * p
            scaleY = scaleX
            translationX = (1f - p) * horizontal * density
        }
    }

@Composable fun Modifier.panelSurface(opaque: Boolean): Modifier = this.glassSurface(opaque)

enum class LauncherSymbol {
    SETTINGS,
    ANDROID,
    HOME,
    NAVIGATION,
    ACCESSIBILITY,
    PRIVACY,
    STORAGE,
    ABOUT,
    OPEN,
    FAVORITE,
    CLOSE,
    EDIT,
    CONTROLS,
    PROFILE,
    WALLPAPER,
    NETWORK,
    AUDIO,
    SEARCH,
}

/** A small offline subset of Lucide, licensed under ISC (see THIRD_PARTY.md). */
@Composable
fun LauncherIcon(symbol: LauncherSymbol, color: Color, modifier: Modifier = Modifier) {
    val resource =
        when (symbol) {
            LauncherSymbol.SEARCH -> R.drawable.ic_search
            LauncherSymbol.SETTINGS -> R.drawable.ic_lucide_settings
            LauncherSymbol.ANDROID -> R.drawable.ic_lucide_tv
            LauncherSymbol.HOME -> R.drawable.ic_lucide_house
            LauncherSymbol.NAVIGATION -> R.drawable.ic_lucide_move
            LauncherSymbol.ACCESSIBILITY -> R.drawable.ic_lucide_accessibility
            LauncherSymbol.PRIVACY -> R.drawable.ic_lucide_shield_check
            LauncherSymbol.STORAGE -> R.drawable.ic_lucide_hard_drive
            LauncherSymbol.ABOUT -> R.drawable.ic_lucide_info
            LauncherSymbol.OPEN -> R.drawable.ic_lucide_arrow_up_right
            LauncherSymbol.FAVORITE -> R.drawable.ic_lucide_star
            LauncherSymbol.CLOSE -> R.drawable.ic_lucide_x
            LauncherSymbol.EDIT -> R.drawable.ic_lucide_pencil
            LauncherSymbol.CONTROLS -> R.drawable.ic_lucide_sliders_horizontal
            LauncherSymbol.PROFILE -> R.drawable.ic_lucide_user_round
            LauncherSymbol.WALLPAPER -> R.drawable.ic_lucide_image
            LauncherSymbol.NETWORK -> R.drawable.ic_lucide_wifi
            LauncherSymbol.AUDIO -> R.drawable.ic_lucide_volume_2
        }
    androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(resource),
        null,
        modifier.size(24.dp),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
    )
}

/** Makes long read-only reports reachable and scrollable with the TV remote. */
@Composable
fun ReadableBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .semantics(mergeDescendants = true) {}
            .border(
                2.dp,
                if (focused) DesignTokens.accent else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/** Text rows stay spatially stable; selected category and keyboard focus are distinct. */
@Composable
fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    symbol: LauncherSymbol? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    reduced: Boolean = false,
    labelSize: TextUnit = 16.sp,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val foreground =
        if (focused && enabled && !compact) DesignTokens.background else DesignTokens.foreground
    val secondary =
        if (focused && enabled && !compact) {
            DesignTokens.background.copy(alpha = .75f)
        } else {
            DesignTokens.secondary
        }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .onFocusChanged { focused = it.isFocused }
                .semantics { this.selected = selected },
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale =
            ButtonDefaults.scale(
                focusedScale = 1f,
                pressedScale = if (reduced) 1f else .99f,
                focusedDisabledScale = 1f,
            ),
        border =
            ButtonDefaults.border(
                border = Border.None,
                focusedBorder = Border.None,
                pressedBorder = Border.None,
                disabledBorder = Border.None,
                focusedDisabledBorder = Border.None,
            ),
        colors =
            ButtonDefaults.colors(
                containerColor =
                    if (compact) {
                        Color.Transparent
                    } else {
                        DesignTokens.foreground.copy(alpha = if (selected) .15f else .055f)
                    },
                contentColor = DesignTokens.foreground,
                focusedContainerColor =
                    DesignTokens.foreground.copy(alpha = if (compact) .14f else 1f),
                focusedContentColor = DesignTokens.background,
                disabledContainerColor =
                    DesignTokens.foreground.copy(alpha = if (focused) .08f else .035f),
                disabledContentColor = DesignTokens.foreground.copy(alpha = .35f),
            ),
        contentPadding =
            PaddingValues(
                horizontal = if (compact) 12.dp else 16.dp,
                vertical = if (compact) 10.dp else 13.dp,
            ),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (symbol != null) LauncherIcon(symbol, if (enabled) foreground else secondary)
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontSize = labelSize,
                color = if (enabled) foreground else DesignTokens.foreground.copy(alpha = .35f),
            )
            if (value != null) {
                Text(
                    value,
                    modifier = Modifier.widthIn(max = 180.dp),
                    color = secondary,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
fun DestructiveRow(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val red = Color(0xFFB3261E)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
        border =
            ButtonDefaults.border(
                border = Border.None,
                focusedBorder = Border.None,
                pressedBorder = Border.None,
            ),
        colors =
            ButtonDefaults.colors(
                containerColor = red.copy(alpha = .35f),
                contentColor = Color.White,
                focusedContainerColor = red,
                focusedContentColor = Color.White,
            ),
    ) {
        Text(label, color = Color.White)
    }
}
