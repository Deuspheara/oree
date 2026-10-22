package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import fr.deuspheara.oree.core.designsystem.component.DestructiveRow
import fr.deuspheara.oree.core.designsystem.component.ReadableBlock
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.BackupReview

@Composable
fun BackupReviewPanel(review: BackupReview, saving: Boolean, failed: Boolean, onRestore: () -> Unit, onClose: () -> Unit) {
    val cancel = remember { FocusRequester() }
    val firstProfile = remember { FocusRequester() }
    GlassDialog(onDismissRequest = { if (!saving) onClose() }) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier.width(440.dp)
                    .heightIn(max = 440.dp)
                    .menuSurface()
                    .padding(20.dp)
                    .semantics { testTagsAsResourceId = true }
                    .testTag("backup-review"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(
                        if (review.undo) R.string.backup_undo else R.string.backup_restore,
                    ),
                    color = DesignTokens.foreground,
                    fontSize = 24.sp,
                )
                Column(
                    Modifier.weight(1f, false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.backup_review_help),
                        color = DesignTokens.secondary,
                    )
                    review.candidate.snapshot.profiles.forEachIndexed { index, profile ->
                        ReadableBlock(
                            Modifier.fillMaxWidth()
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(firstProfile)
                                    } else {
                                        Modifier
                                    },
                                )
                                .testTag("backup-profile:${profile.id}"),
                        ) {
                            Text(
                                stringResource(
                                    R.string.backup_profile_summary,
                                    profile.name,
                                    profile.favorites.size,
                                    profile.folders.size,
                                ),
                                color = DesignTokens.foreground,
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.backup_assets_help),
                        color = DesignTokens.secondary,
                    )
                    if (review.candidate.missingImages > 0) {
                        Text(
                            stringResource(
                                R.string.backup_missing_images,
                                review.candidate.missingImages,
                            ),
                            color = DesignTokens.foreground,
                        )
                    }
                    if (failed) {
                        Text(stringResource(R.string.backup_error), color = DesignTokens.foreground)
                    }
                }
                SettingsRow(
                    stringResource(R.string.cancel),
                    onClose,
                    Modifier.fillMaxWidth()
                        .focusRequester(cancel)
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.DirectionUp && !saving) {
                                if (event.type == KeyEventType.KeyDown) firstProfile.requestFocus()
                                true
                            } else {
                                false
                            }
                        }
                        .testTag("backup-cancel"),
                    enabled = !saving,
                    compact = true,
                )
                DestructiveRow(
                    stringResource(if (saving) R.string.saving else R.string.backup_restore),
                    onRestore,
                    Modifier.fillMaxWidth().testTag("backup-confirm"),
                    enabled = !saving,
                )
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                cancel.requestFocus()
            }
        }
    }
}
