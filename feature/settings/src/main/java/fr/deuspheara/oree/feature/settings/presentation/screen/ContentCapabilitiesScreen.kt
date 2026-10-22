package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rows
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.ReadableBlock
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.ContentCapabilities
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.ProbeAvailability

@Composable
fun ContentCapabilitiesScreen(report: ContentCapabilities?, reduced: Boolean, onRetry: () -> Unit, onClose: () -> Unit) {
    val close = remember { FocusRequester() }
    val firstSource = remember { FocusRequester() }
    val motion = rememberPanelTransition(reduced)
    GlassDialog(onDismissRequest = { motion.close(onClose) }) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 680.dp)
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .panelMotion(motion, reduced)
                    .menuSurface()
                    .padding(20.dp)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("content-diagnostics"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.content_diagnostics),
                    color = DesignTokens.foreground,
                    fontSize = 24.sp,
                )
                Column(
                    Modifier.weight(1f, false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.diagnostics_help),
                        color = DesignTokens.secondary,
                        fontSize = 14.sp,
                    )
                    val data = report
                    if (data == null) {
                        Text(
                            stringResource(R.string.diagnostics_loading),
                            color = DesignTokens.foreground,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (data.listingsPermission) {
                                    R.string.diagnostics_permission_granted
                                } else {
                                    R.string.diagnostics_permission_absent
                                },
                            ),
                            color = DesignTokens.foreground,
                        )
                        data.providers.forEachIndexed { index, provider ->
                            ReadableBlock(
                                Modifier.fillMaxWidth()
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(firstSource)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .testTag("diagnostics-source:${provider.source.name}"),
                            ) {
                                val name =
                                    stringResource(
                                        when (provider.source) {
                                            ContentSourceId.CHANNELS ->
                                                R.string.diagnostics_channels

                                            ContentSourceId.PREVIEW -> R.string.diagnostics_preview

                                            ContentSourceId.WATCH_NEXT -> R.string.watch_next_title

                                            ContentSourceId.PROGRAMS ->
                                                R.string.diagnostics_programs
                                        },
                                    )
                                Text(
                                    "$name · ${stringResource(probeStatus(provider.availability))}",
                                    color = DesignTokens.foreground,
                                    fontSize = 18.sp,
                                )
                                if (provider.availability == ProbeAvailability.AVAILABLE) {
                                    Text(
                                        stringResource(
                                            R.string.diagnostics_counts,
                                            provider.rows,
                                            provider.metadata,
                                            provider.localArtwork,
                                            provider.launchable,
                                            provider.crossPublisher,
                                        ),
                                        color = DesignTokens.secondary,
                                        fontSize = 14.sp,
                                    )
                                    Text(
                                        stringResource(
                                            when (provider.artworkReadable) {
                                                true -> R.string.diagnostics_art_readable
                                                false -> R.string.diagnostics_art_denied
                                                null -> R.string.diagnostics_art_absent
                                            },
                                        ),
                                        color = DesignTokens.secondary,
                                        fontSize = 14.sp,
                                    )
                                    if (provider.bounded) {
                                        Text(
                                            stringResource(R.string.diagnostics_bounded),
                                            color = DesignTokens.secondary,
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            stringResource(
                                if (data.systemSearch) {
                                    R.string.diagnostics_system_search
                                } else {
                                    R.string.diagnostics_no_system_search
                                },
                            ),
                            color = DesignTokens.foreground,
                        )
                        Text(
                            stringResource(
                                R.string.diagnostics_search_candidates,
                                data.searchableCandidates,
                                data.exportedSearchCandidates,
                            ) + " · " + stringResource(probeStatus(data.searchableAvailability)),
                            color = DesignTokens.secondary,
                            fontSize = 14.sp,
                        )
                    }
                }
                Row(
                    Modifier.onPreviewKeyEvent { event ->
                        if (
                            event.key == Key.DirectionUp && report?.providers?.isNotEmpty() == true
                        ) {
                            if (event.type == KeyEventType.KeyDown) firstSource.requestFocus()
                            true
                        } else {
                            false
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SettingsRow(
                        stringResource(R.string.retry),
                        onRetry,
                        Modifier.weight(1f),
                        enabled = report != null,
                        compact = true,
                    )
                    SettingsRow(
                        stringResource(R.string.close),
                        { motion.close(onClose) },
                        Modifier.weight(1f).focusRequester(close).testTag("diagnostics-close"),
                        compact = true,
                    )
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                close.requestFocus()
            }
        }
    }
}

private fun probeStatus(state: ProbeAvailability) = when (state) {
    ProbeAvailability.AVAILABLE -> R.string.diagnostics_available
    ProbeAvailability.EMPTY -> R.string.diagnostics_empty
    ProbeAvailability.DENIED -> R.string.diagnostics_denied
    ProbeAvailability.UNSUPPORTED -> R.string.diagnostics_unsupported
    ProbeAvailability.ERROR -> R.string.diagnostics_error
}
