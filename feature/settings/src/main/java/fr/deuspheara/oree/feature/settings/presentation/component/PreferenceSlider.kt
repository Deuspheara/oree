package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import kotlin.math.roundToInt

/** D-pad edits a local draft; Select commits once, Back discards. No writes while browsing. */
@Composable
internal fun PreferenceSlider(
    title: String,
    description: String,
    options: List<String>,
    selectedIndex: Int,
    tag: String,
    reduced: Boolean,
    enabled: Boolean = true,
    textSize: Boolean = false,
    onSelect: (Int) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var restore by remember { mutableStateOf(false) }
    val trigger = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(open, windowFocused) {
        if (!open && restore && windowFocused) {
            withFrameNanos {}
            trigger.requestFocus()
            restore = false
        }
    }
    SettingsRow(
        title,
        { open = true },
        Modifier.fillMaxWidth().focusRequester(trigger).settingsFocus("settings-$tag"),
        value = options[selectedIndex],
        enabled = enabled,
        reduced = reduced,
    )
    if (!open) return
    val density = LocalDensity.current
    val resources = LocalResources.current
    val direction = LocalLayoutDirection.current
    val motion = rememberPanelTransition(reduced)
    var draft by rememberSaveable { mutableIntStateOf(selectedIndex) }
    val rail = remember { FocusRequester() }
    fun close(commit: Boolean) {
        motion.close {
            if (commit) onSelect(draft)
            open = false
            restore = true
        }
    }
    GlassDialog(onDismissRequest = { close(false) }) {
        CompositionLocalProvider(
            LocalDensity provides density,
            LocalResources provides resources,
            LocalLayoutDirection provides direction,
        ) {
            val ready = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(ready) {
                if (ready) {
                    withFrameNanos {}
                    rail.requestFocus()
                }
            }
            var focused by remember { mutableStateOf(false) }
            val fraction = animateFloatAsState(
                draft.toFloat() / (options.size - 1).coerceAtLeast(1),
                if (reduced) snap() else tween(110),
                label = "preference-slider-thumb",
            )
            val foreground = D.foreground
            val background = D.background
            val accent = D.accent
            val valueLabel = options[draft]
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = motion.progress.value }
                    .background(background)
                    .background(Brush.radialGradient(listOf(accent.copy(alpha = .10f), Color.Transparent)))
                    .padding(horizontal = 72.dp, vertical = 32.dp).testTag("settings-$tag-slider"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text(title, color = foreground, fontSize = 34.sp, textAlign = TextAlign.Center)
                    Text(description, color = D.secondary, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    Text(valueLabel, color = foreground, fontSize = 24.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        Text(if (textSize) "A" else options.first(), color = D.secondary, fontSize = 15.sp)
                        Canvas(
                            Modifier.weight(1f).height(84.dp).focusRequester(rail)
                                .onFocusChanged { focused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    when (event.key) {
                                        Key.DirectionLeft, Key.DirectionRight -> {
                                            if (event.type == KeyEventType.KeyDown && !motion.closing) {
                                                val delta = if ((event.key == Key.DirectionRight) ==
                                                    (direction == LayoutDirection.Ltr)
                                                ) {
                                                    1
                                                } else {
                                                    -1
                                                }
                                                draft = (draft + delta).coerceIn(options.indices)
                                            }
                                            true
                                        }

                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                            if (event.type == KeyEventType.KeyUp) close(true)
                                            true
                                        }

                                        else -> false
                                    }
                                }
                                .semantics {
                                    contentDescription = title
                                    stateDescription = valueLabel
                                    progressBarRangeInfo =
                                        ProgressBarRangeInfo(
                                            draft.toFloat(),
                                            0f..options.lastIndex.toFloat(),
                                            (options.size - 2).coerceAtLeast(0),
                                        )
                                    setProgress { value ->
                                        if (motion.closing || !value.isFinite()) {
                                            false
                                        } else {
                                            draft = value.roundToInt().coerceIn(options.indices)
                                            true
                                        }
                                    }
                                }
                                .testTag("settings-$tag-rail").focusable(),
                        ) {
                            val inset = 22.dp.toPx()
                            val width = (size.width - inset * 2).coerceAtLeast(0f)
                            val y = size.height * .4f
                            val railHeight = 7.dp.toPx()
                            drawRoundRect(
                                foreground.copy(alpha = .18f),
                                Offset(inset, y - railHeight / 2),
                                Size(width, railHeight),
                                CornerRadius(railHeight),
                            )
                            val f = if (direction == LayoutDirection.Rtl) 1f - fraction.value else fraction.value
                            val x = inset + width * f
                            repeat(options.size) { index ->
                                val step = index.toFloat() / (options.size - 1).coerceAtLeast(1)
                                drawCircle(
                                    foreground.copy(
                                        alpha = if (index ==
                                            draft
                                        ) {
                                            .8f
                                        } else {
                                            .25f
                                        },
                                    ),
                                    2.dp.toPx(),
                                    Offset(inset + width * (if (direction == LayoutDirection.Rtl) 1f - step else step), y + 27.dp.toPx()),
                                )
                            }
                            val thumbWidth = 42.dp.toPx()
                            val thumbHeight = if (focused) 22.dp.toPx() else 18.dp.toPx()
                            drawRoundRect(
                                foreground.copy(alpha = if (focused) 1f else .7f),
                                Offset(x - thumbWidth / 2, y - thumbHeight / 2),
                                Size(thumbWidth, thumbHeight),
                                CornerRadius(thumbHeight / 2),
                            )
                        }
                        Text(if (textSize) "A" else options.last(), color = D.secondary, fontSize = if (textSize) 32.sp else 15.sp)
                    }
                    Text(stringResource(R.string.slider_remote_hint), color = D.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
