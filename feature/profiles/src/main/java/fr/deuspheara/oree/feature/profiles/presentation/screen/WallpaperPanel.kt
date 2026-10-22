package fr.deuspheara.oree.feature.profiles.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.resource.contentStatus
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.RecentContent
import fr.deuspheara.oree.core.model.WallpaperMode

@Composable
fun WallpaperPanel(
    profile: LocalProfile,
    content: RecentContent,
    reduced: Boolean,
    saving: Boolean,
    failed: Boolean,
    onMode: (WallpaperMode) -> Unit,
    onLandscape: () -> Unit,
    onImport: () -> Unit,
    onContent: () -> Unit,
    onClose: () -> Unit,
) {
    val motion = rememberPanelTransition(reduced)
    val first = remember { FocusRequester() }
    GlassDialog(onDismissRequest = { if (!saving) motion.close(onClose) }) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopEnd) {
            Column(
                Modifier.width(440.dp)
                    .heightIn(max = 490.dp)
                    .panelMotion(motion, reduced, 32f)
                    .menuSurface()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .testTag("wallpaper-panel"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.wallpaper),
                    color = DesignTokens.foreground,
                    fontSize = 24.sp,
                )
                SettingsRow(
                    stringResource(R.string.wallpaper_landscape),
                    { onMode(WallpaperMode.LANDSCAPE) },
                    Modifier.fillMaxWidth().focusRequester(first).testTag("wallpaper:LANDSCAPE"),
                    selected = profile.wallpaper == WallpaperMode.LANDSCAPE,
                    enabled = !saving,
                )
                if (profile.wallpaper == WallpaperMode.LANDSCAPE) {
                    SettingsRow(
                        stringResource(R.string.landscape_palette),
                        onLandscape,
                        Modifier.fillMaxWidth(),
                        value = "${profile.landscape + 1}",
                        enabled = !saving,
                    )
                }
                SettingsRow(
                    stringResource(R.string.wallpaper_personal),
                    onImport,
                    Modifier.fillMaxWidth().testTag("import-wallpaper"),
                    selected = profile.wallpaper == WallpaperMode.PERSONAL,
                    enabled = !saving,
                )
                if (profile.image != null && profile.wallpaper != WallpaperMode.PERSONAL) {
                    SettingsRow(
                        stringResource(R.string.use_saved_photo),
                        { onMode(WallpaperMode.PERSONAL) },
                        Modifier.fillMaxWidth(),
                        enabled = !saving,
                    )
                }
                SettingsRow(
                    stringResource(R.string.wallpaper_application),
                    { onMode(WallpaperMode.APPLICATION) },
                    Modifier.fillMaxWidth().testTag("wallpaper:APPLICATION"),
                    selected = profile.wallpaper == WallpaperMode.APPLICATION,
                    enabled = !saving,
                )
                var more by remember { mutableStateOf(profile.wallpaper == WallpaperMode.CONTENT) }
                SettingsRow(
                    stringResource(R.string.wallpaper_more),
                    { more = !more },
                    Modifier.fillMaxWidth().testTag("wallpaper-more"),
                )
                if (more) {
                    SettingsRow(
                        stringResource(R.string.wallpaper_content),
                        { onMode(WallpaperMode.CONTENT) },
                        Modifier.fillMaxWidth().testTag("wallpaper:CONTENT"),
                        selected = profile.wallpaper == WallpaperMode.CONTENT,
                        enabled = !saving,
                    )
                    SettingsRow(
                        stringResource(R.string.content_source),
                        onContent,
                        Modifier.fillMaxWidth().testTag("content-source"),
                        value =
                            stringResource(
                                if (profile.contentEnabled) R.string.enabled else R.string.disabled,
                            ),
                        enabled = !saving,
                    )
                    Text(
                        stringResource(R.string.wallpaper_content_help),
                        color = DesignTokens.secondary,
                        fontSize = 13.sp,
                    )
                    Text(
                        stringResource(R.string.shared_tv_content),
                        color = DesignTokens.secondary,
                        fontSize = 13.sp,
                    )
                    content.title?.let {
                        Text(it, color = DesignTokens.foreground, fontSize = 14.sp)
                    }
                    Text(
                        stringResource(contentStatus(content.availability)),
                        color = DesignTokens.secondary,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("content-status"),
                    )
                }
                if (failed) {
                    Text(stringResource(R.string.wallpaper_error), color = DesignTokens.foreground)
                }
                SettingsRow(
                    stringResource(R.string.close),
                    { motion.close(onClose) },
                    enabled = !saving,
                )
            }
        }
        LaunchedEffect(Unit) {
            withFrameNanos {}
            first.requestFocus()
        }
    }
}

fun wallpaperLabel(mode: WallpaperMode): Int = when (mode) {
    WallpaperMode.LANDSCAPE -> R.string.wallpaper_landscape
    WallpaperMode.PERSONAL -> R.string.wallpaper_personal
    WallpaperMode.APPLICATION -> R.string.wallpaper_application
    WallpaperMode.CONTENT -> R.string.wallpaper_content
}
