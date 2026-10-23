package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.glass.glassControlForeground
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.platform.status.AudioOutput
import fr.deuspheara.oree.core.platform.status.ControlStatus

/** tvOS-style horizontal media-volume panel; platform settings own output switching. */
@Composable
internal fun ControlVolumeSlider(status: ControlStatus, reduced: Boolean, onAdjust: ((Int) -> Unit)?, modifier: Modifier = Modifier) {
    var entered by remember { mutableStateOf(reduced) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(if (entered) 1f else 0f, if (reduced) snap() else tween(180), label = "control-audio-reveal")
    val max = status.maxVolume?.takeIf { it > 0 }
    val volume = status.volume
    val available = !status.volumeFixed && volume != null && max != null && onAdjust != null
    val target = if (status.muted) {
        0f
    } else if (volume != null && max != null) {
        (volume.toFloat() / max).coerceIn(0f, 1f)
    } else {
        0f
    }
    val fraction by animateFloatAsState(target, if (reduced) snap() else tween(140), label = "control-volume-fill")
    var focused by remember { mutableStateOf(false) }
    val emphasis by animateFloatAsState(if (focused) 1f else 0f, if (reduced) snap() else tween(120), label = "control-volume-focus")
    val interactions = remember { MutableInteractionSource() }
    val description = stringResource(R.string.control_sound)
    val value = when {
        status.volumeFixed -> stringResource(R.string.control_volume_fixed)
        status.muted -> stringResource(R.string.control_muted)
        volume != null && max != null -> stringResource(R.string.control_volume_percent, volume * 100 / max)
        else -> stringResource(R.string.status_unavailable)
    }
    val foreground = glassControlForeground()
    val direction = LocalLayoutDirection.current
    val outputs = status.audio.sortedBy { it.ordinal }.map {
        stringResource(
            when (it) {
                AudioOutput.SPEAKER -> R.string.audio_speakers
                AudioOutput.HDMI -> R.string.audio_hdmi
                AudioOutput.HEADPHONES -> R.string.audio_headphones
                AudioOutput.BLUETOOTH -> R.string.audio_bluetooth
                AudioOutput.USB -> R.string.audio_usb
                AudioOutput.OTHER -> R.string.audio_other
            },
        )
    }
    Column(
        Modifier.fillMaxWidth().graphicsLayer {
            alpha = reveal
            scaleX = .96f + .04f * reveal
            scaleY = scaleX
            transformOrigin = TransformOrigin(1f, 0f)
        }.glassSurface(
            radius = 28.dp,
            progressive = remember { dev.chrisbanes.haze.HazeProgressive.verticalGradient(startIntensity = .65f, endIntensity = 1f) },
        )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LauncherIcon(LauncherSymbol.AUDIO, foreground)
            Text(value, color = foreground, fontSize = 20.sp)
        }
        if (outputs.isNotEmpty()) {
            Text(
                stringResource(R.string.control_available_outputs, outputs.joinToString(" · ")),
                color = foreground.copy(alpha = .78f),
                fontSize = 13.sp,
            )
        }
        Canvas(
            modifier.fillMaxWidth().height(52.dp)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.DirectionLeft || event.key == Key.DirectionRight) {
                        if (event.type == KeyEventType.KeyDown && available) {
                            val increase = (event.key == Key.DirectionRight) == (direction == LayoutDirection.Ltr)
                            if ((increase && (volume ?: 0) < (max ?: 0)) ||
                                (!increase && (volume ?: 0) > 0)
                            ) {
                                onAdjust?.invoke(if (increase) 1 else -1)
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
                .semantics {
                    contentDescription = description
                    stateDescription = value
                    if (max != null &&
                        volume != null
                    ) {
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(volume.toFloat(), 0f..max.toFloat(), (max - 1).coerceAtLeast(0))
                    }
                }
                .testTag("control-volume-slider")
                .combinedClickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = {
                    if (available) onAdjust?.invoke(0)
                }),
        ) {
            val inset = 12.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(0f)
            val y = size.height / 2
            val height = (8.dp.toPx() + emphasis * 3.dp.toPx())
            val radius = CornerRadius(height / 2)
            drawRoundRect(foreground.copy(alpha = .22f), Offset(inset, y - height / 2), Size(width, height), radius)
            val fill = width * fraction
            val start = if (direction == LayoutDirection.Rtl) inset + width - fill else inset
            drawRoundRect(foreground.copy(alpha = if (available) .95f else .4f), Offset(start, y - height / 2), Size(fill, height), radius)
            val x = inset + width * (if (direction == LayoutDirection.Rtl) 1f - fraction else fraction)
            val thumb = (14.dp.toPx() + emphasis * 6.dp.toPx())
            drawRoundRect(foreground, Offset(x - thumb / 2, y - thumb / 2), Size(thumb, thumb), CornerRadius(thumb / 2))
        }
        Text(stringResource(R.string.control_volume_remote_hint), color = foreground.copy(alpha = .75f), fontSize = 13.sp)
    }
}
