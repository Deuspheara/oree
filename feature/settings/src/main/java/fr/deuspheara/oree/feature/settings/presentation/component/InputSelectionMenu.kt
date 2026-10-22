package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputConnection
import fr.deuspheara.oree.core.model.TvInputs

/** Native modal chooser; only discovered public inputs can be dispatched. */
@Composable
fun InputSelectionMenu(inputs: TvInputs, reduced: Boolean, onSelect: (TvInput) -> Unit, onClose: () -> Unit) {
    val motion = rememberPanelTransition(reduced)
    val first = remember { FocusRequester() }
    var focusedId by remember { mutableStateOf<String?>(null) }
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    GlassDialog(onDismissRequest = { motion.close(onClose) }) {
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(windowFocused, inputs.entries.map { it.id }) {
            if (windowFocused) {
                withFrameNanos {}
                val target =
                    focusedId?.let { id -> requesters[id] }
                        ?: inputs.entries.firstOrNull()?.id?.let { requesters[it] }
                (target ?: first).requestFocus()
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(28.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                Modifier.width(minOf(360.dp, maxWidth))
                    .heightIn(max = maxHeight)
                    .panelMotion(motion, reduced)
                    .menuSurface()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
                    .testTag("input-menu"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    stringResource(R.string.inputs),
                    Modifier.padding(12.dp),
                    color = DesignTokens.foreground,
                    fontSize = 22.sp,
                )
                if (inputs.unavailable || inputs.entries.isEmpty()) {
                    Text(
                        stringResource(
                            if (inputs.unavailable) {
                                R.string.input_unavailable
                            } else {
                                R.string.inputs_empty
                            },
                        ),
                        Modifier.padding(12.dp),
                        color = DesignTokens.secondary,
                        fontSize = 15.sp,
                    )
                }
                inputs.entries.forEach { input ->
                    key(input.id) {
                        val requester = remember { FocusRequester() }
                        DisposableEffect(input.id) {
                            requesters[input.id] = requester
                            onDispose { requesters.remove(input.id) }
                        }
                        SettingsRow(
                            input.label,
                            {
                                motion.close {
                                    onSelect(input)
                                    onClose()
                                }
                            },
                            Modifier.fillMaxWidth()
                                .focusRequester(requester)
                                .onFocusChanged { if (it.isFocused) focusedId = input.id }
                                .testTag("input:${input.id}"),
                            value =
                                stringResource(
                                    when (input.connection) {
                                        TvInputConnection.CONNECTED -> R.string.input_connected

                                        TvInputConnection.STANDBY -> R.string.input_standby

                                        TvInputConnection.DISCONNECTED ->
                                            R.string.input_disconnected

                                        TvInputConnection.UNKNOWN -> R.string.input_unavailable
                                    },
                                ),
                            symbol = LauncherSymbol.ANDROID,
                            compact = true,
                            reduced = reduced,
                            enabled = !motion.closing,
                        )
                    }
                }
                SettingsRow(
                    stringResource(R.string.close),
                    { motion.close(onClose) },
                    Modifier.fillMaxWidth().focusRequester(first).testTag("input-menu-close"),
                    symbol = LauncherSymbol.CLOSE,
                    compact = true,
                    reduced = reduced,
                    enabled = !motion.closing,
                )
            }
        }
    }
}
