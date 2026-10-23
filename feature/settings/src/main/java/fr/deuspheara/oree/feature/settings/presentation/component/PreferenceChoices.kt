package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalWindowInfo
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
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D

/** Focus is a browsing state. Only Select accepts a new preference. Back discards browsing. */
@Composable
internal fun PreferenceChoices(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    tag: String,
    reduced: Boolean,
    enabled: Boolean = true,
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
    if (open) {
        val current = remember { FocusRequester() }
        PreferencePanel(title, reduced, current, {
            open = false
            restore = true
        }) { close ->
            options.forEachIndexed { index, label ->
                SettingsRow(
                    label,
                    {
                        close {
                            onSelect(index)
                            open = false
                            restore = true
                        }
                    },
                    Modifier.fillMaxWidth()
                        .then(if (index == selectedIndex) Modifier.focusRequester(current) else Modifier)
                        .testTag("settings-$tag-choice:$index"),
                    value = if (index == selectedIndex) "✓" else null,
                    selected = index == selectedIndex,
                    reduced = reduced,
                )
            }
        }
    }
}

@Composable
internal fun ResetPreferencesConfirmation(reduced: Boolean, onConfirm: () -> Unit, onClose: () -> Unit) {
    val cancel = remember { FocusRequester() }
    PreferencePanel(stringResource(R.string.defaults), reduced, cancel, onClose) { close ->
        Text(stringResource(R.string.defaults_scope), color = D.secondary)
        SettingsRow(
            stringResource(R.string.cancel),
            { close(onClose) },
            Modifier.fillMaxWidth().focusRequester(cancel).testTag("settings-defaults-cancel"),
            reduced = reduced,
        )
        SettingsRow(
            stringResource(R.string.defaults),
            { close(onConfirm) },
            Modifier.fillMaxWidth().testTag("settings-defaults-confirm"),
            reduced = reduced,
        )
    }
}

@Composable
private fun PreferencePanel(
    title: String,
    reduced: Boolean,
    initial: FocusRequester,
    onClose: () -> Unit,
    content: @Composable ((() -> Unit) -> Unit) -> Unit,
) {
    val motion = rememberPanelTransition(reduced)
    GlassDialog(onDismissRequest = { motion.close(onClose) }) {
        val windowFocused = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(windowFocused) {
            if (windowFocused) {
                withFrameNanos {}
                initial.requestFocus()
            }
        }
        BoxWithConstraints(
            Modifier.fillMaxSize().background(D.background).semantics {
                testTagsAsResourceId = true
            }.padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().heightIn(max = maxHeight)
                    .panelMotion(motion, reduced)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, color = D.foreground, fontSize = 26.sp)
                content { action -> if (!motion.closing) motion.close(action) }
            }
        }
    }
}
