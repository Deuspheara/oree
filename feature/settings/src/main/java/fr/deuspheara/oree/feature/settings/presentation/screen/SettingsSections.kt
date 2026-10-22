package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.resource.contentStatus
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.settings.presentation.component.AccentSelector
import fr.deuspheara.oree.feature.settings.presentation.component.GlassIntensityPreview
import fr.deuspheara.oree.feature.settings.presentation.state.AmbientSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.BackupSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.ContentSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsAction
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsControl

@Composable
internal fun AppearanceSettings(
    value: LauncherPreferences,
    reduceMotion: Boolean,
    ambient: AmbientSettingsState,
    ambientTrigger: FocusRequester,
    onChange: (LauncherPreferences) -> Unit,
    onAction: (SettingsAction) -> Unit,
    controls: Set<SettingsControl>,
) {
    val onAmbient: (() -> Unit)? =
        if (SettingsControl.AMBIENT in controls) {
            { onAction(SettingsAction.OpenAmbient) }
        } else {
            null
        }
    val onAmbientPhotos: (() -> Unit)? =
        if (SettingsControl.AMBIENT_PHOTOS in controls) {
            { onAction(SettingsAction.ToggleAmbientPhotos) }
        } else {
            null
        }
    val onAmbientClock: (() -> Unit)? =
        if (SettingsControl.AMBIENT_CLOCK in controls) {
            { onAction(SettingsAction.ToggleAmbientClock) }
        } else {
            null
        }
    val onDreamSettings: (() -> Unit)? =
        if (SettingsControl.DREAM_SETTINGS in controls) {
            { onAction(SettingsAction.DreamSettings) }
        } else {
            null
        }
    SettingsChoice(
        R.string.theme_label,
        themeLabel(value.theme),
        "theme",
        reduceMotion,
    ) {
        onChange(
            value.copy(
                theme =
                    ThemeMode.entries[
                        (value.theme.ordinal + 1) % ThemeMode.entries.size,
                    ],
            ),
        )
    }
    SettingsChoice(
        R.string.quality_label,
        if (value.quality == QualityTier.ECONOMY) {
            R.string.quality_economy
        } else {
            R.string.quality_balanced
        },
        "quality",
        reduceMotion,
    ) {
        onChange(
            value.copy(
                quality =
                    if (value.quality == QualityTier.BALANCED) {
                        QualityTier.ECONOMY
                    } else {
                        QualityTier.BALANCED
                    },
            ),
        )
    }
    Help(R.string.quality_help)
    SettingsToggle(
        R.string.liquid_glass,
        value.liquidGlass,
        "liquid-glass",
        reduceMotion,
    ) {
        onChange(value.copy(liquidGlass = !value.liquidGlass))
    }
    SettingsRow(
        stringResource(R.string.glass_intensity),
        {
            onChange(
                value.copy(
                    glassIntensity =
                        if (value.glassIntensity >= 1f) {
                            .25f
                        } else {
                            value.glassIntensity + .25f
                        },
                ),
            )
        },
        Modifier.fillMaxWidth().testTag("settings-glass-intensity"),
        value = "${(value.glassIntensity * 100).toInt()}%",
        enabled = value.liquidGlass,
        reduced = reduceMotion,
    )
    GlassIntensityPreview(value)
    Help(R.string.glass_help)
    Text(
        stringResource(R.string.accent_color),
        color = D.secondary,
        fontSize = 15.sp,
    )
    AccentSelector(value.accent) { onChange(value.copy(accent = it)) }
    onAmbient?.let { action ->
        SettingsRow(
            stringResource(R.string.ambient_open),
            {
                action()
            },
            Modifier.fillMaxWidth()
                .focusRequester(ambientTrigger)
                .testTag("settings-ambient"),
        )
        onAmbientPhotos?.let {
            SettingsRow(
                stringResource(R.string.ambient_photos),
                it,
                Modifier.fillMaxWidth(),
                value =
                    stringResource(
                        if (ambient.photos) {
                            R.string.enabled
                        } else {
                            R.string.disabled
                        },
                    ),
            )
        }
        onAmbientClock?.let {
            SettingsRow(
                stringResource(R.string.ambient_clock),
                it,
                Modifier.fillMaxWidth(),
                value =
                    stringResource(
                        if (ambient.clock) {
                            R.string.enabled
                        } else {
                            R.string.disabled
                        },
                    ),
            )
        }
        onDreamSettings?.let {
            SettingsRow(
                stringResource(R.string.ambient_system_settings),
                it,
                Modifier.fillMaxWidth().testTag("settings-dream"),
            )
        }
        Help(R.string.ambient_help)
    }
    SettingsRow(
        stringResource(R.string.defaults),
        { onChange(LauncherPreferences()) },
        Modifier.fillMaxWidth().testTag("settings-defaults"),
        reduced = reduceMotion,
    )
}

@Composable
internal fun HomeSettings(
    content: ContentSettingsState,
    inputTrigger: FocusRequester,
    onInputs: () -> Unit,
    onAction: (SettingsAction) -> Unit,
    controls: Set<SettingsControl>,
) {
    val onAppPreviews: (() -> Unit)? =
        if (SettingsControl.APP_PREVIEWS in controls) {
            { onAction(SettingsAction.ToggleAppPreviews) }
        } else {
            null
        }
    val onDiagnostics: (() -> Unit)? =
        if (SettingsControl.DIAGNOSTICS in controls) {
            { onAction(SettingsAction.Diagnostics) }
        } else {
            null
        }
    val onWallpaper: (() -> Unit)? =
        if (SettingsControl.WALLPAPER in controls) {
            { onAction(SettingsAction.Wallpaper) }
        } else {
            null
        }
    val onProfiles: (() -> Unit)? =
        if (SettingsControl.PROFILES in controls) {
            { onAction(SettingsAction.Profiles) }
        } else {
            null
        }
    val onWatchNext = { onAction(SettingsAction.ToggleWatchNext) }
    val onRetryContent = { onAction(SettingsAction.RetryContent) }
    val onCustomizeControls = { onAction(SettingsAction.CustomizeControls) }
    onAppPreviews?.let {
        SettingsRow(
            stringResource(R.string.app_previews),
            it,
            Modifier.fillMaxWidth().testTag("settings-app-previews"),
            value =
                stringResource(
                    if (content.appPreviewsEnabled) {
                        R.string.enabled
                    } else {
                        R.string.disabled
                    },
                ),
        )
        Text(
            stringResource(
                when (content.appPreviewAvailability) {
                    ContentAvailability.LOADING -> R.string.diagnostics_loading

                    ContentAvailability.AVAILABLE -> R.string.preview_available

                    ContentAvailability.DENIED -> R.string.diagnostics_denied

                    ContentAvailability.UNSUPPORTED ->
                        R.string.diagnostics_unsupported

                    ContentAvailability.ERROR -> R.string.diagnostics_error

                    ContentAvailability.DISABLED -> R.string.disabled

                    else -> R.string.preview_empty
                },
            ),
            color = D.secondary,
            fontSize = 13.sp,
        )
        if (content.appPreviewAvailability == ContentAvailability.ERROR) {
            SettingsRow(
                stringResource(R.string.retry),
                onRetryContent,
                Modifier.fillMaxWidth(),
            )
        }
        Help(R.string.app_previews_help)
    }
    onDiagnostics?.let {
        SettingsRow(
            stringResource(R.string.content_diagnostics),
            it,
            Modifier.fillMaxWidth().testTag("settings-content-diagnostics"),
            symbol = LauncherSymbol.ABOUT,
        )
    }
    SettingsRow(
        stringResource(R.string.watch_next_home),
        onWatchNext,
        Modifier.fillMaxWidth().testTag("settings-watch-next"),
        value =
            stringResource(
                if (content.watchNextEnabled) R.string.enabled else R.string.disabled,
            ),
    )
    Text(
        stringResource(
            when (content.watchNextAvailability) {
                ContentAvailability.AVAILABLE -> R.string.watch_next_available
                ContentAvailability.DISABLED -> R.string.watch_next_disabled
                ContentAvailability.DENIED -> R.string.watch_next_denied
                else -> contentStatus(content.watchNextAvailability)
            },
        ),
        color = D.secondary,
        fontSize = 13.sp,
        modifier = Modifier.testTag("watch-next-status"),
    )
    if (content.watchNextAvailability == ContentAvailability.ERROR) {
        SettingsRow(
            stringResource(R.string.retry),
            onRetryContent,
            Modifier.fillMaxWidth().testTag("watch-next-retry"),
        )
    }
    Help(R.string.watch_next_help)
    SettingsRow(
        stringResource(R.string.inputs),
        { onInputs() },
        Modifier.fillMaxWidth()
            .focusRequester(inputTrigger)
            .testTag("settings-inputs"),
        symbol = LauncherSymbol.ANDROID,
    )
    onWallpaper?.let {
        SettingsRow(
            stringResource(R.string.wallpaper),
            it,
            Modifier.fillMaxWidth().testTag("settings-wallpaper"),
            symbol = LauncherSymbol.WALLPAPER,
        )
    }
    onProfiles?.let {
        SettingsRow(
            stringResource(R.string.profiles),
            it,
            Modifier.fillMaxWidth().testTag("settings-profiles"),
            symbol = LauncherSymbol.PROFILE,
        )
    }
    SettingsRow(
        stringResource(R.string.customize_controls),
        onCustomizeControls,
        Modifier.fillMaxWidth().testTag("settings-customize-controls"),
        symbol = LauncherSymbol.CONTROLS,
    )
    Help(R.string.home_dock_help)
}

@Composable
internal fun AccessibilitySettings(value: LauncherPreferences, reduceMotion: Boolean, onChange: (LauncherPreferences) -> Unit) {
    SettingsToggle(
        R.string.contrast_label,
        value.highContrast,
        "contrast",
        reduceMotion,
    ) {
        onChange(value.copy(highContrast = !value.highContrast))
    }
    SettingsToggle(
        R.string.transparency_label,
        value.reduceTransparency,
        "transparency",
        reduceMotion,
    ) {
        onChange(value.copy(reduceTransparency = !value.reduceTransparency))
    }
    SettingsToggle(
        R.string.reduced_motion,
        value.reduceMotion,
        "motion",
        reduceMotion,
    ) {
        onChange(value.copy(reduceMotion = !value.reduceMotion))
    }
    SettingsRow(
        stringResource(R.string.text_size),
        {
            onChange(
                value.copy(
                    textScale =
                        when {
                            value.textScale < 1.25f -> 1.25f
                            value.textScale < 1.5f -> 1.5f
                            value.textScale < 2f -> 2f
                            else -> 1f
                        },
                ),
            )
        },
        Modifier.fillMaxWidth().testTag("settings-text-size"),
        value = "${(value.textScale * 100).toInt()}%",
        reduced = reduceMotion,
    )
}

@Composable
internal fun StorageSettings(
    backup: BackupSettingsState,
    cacheBytes: Long,
    reduceMotion: Boolean,
    onAction: (SettingsAction) -> Unit,
    controls: Set<SettingsControl>,
) {
    val onExport: (() -> Unit)? =
        if (SettingsControl.EXPORT in controls) {
            { onAction(SettingsAction.ExportBackup) }
        } else {
            null
        }
    val onImport: (() -> Unit)? =
        if (SettingsControl.IMPORT in controls) {
            { onAction(SettingsAction.ImportBackup) }
        } else {
            null
        }
    val onUndoRestore: (() -> Unit)? =
        if (SettingsControl.UNDO in controls) {
            { onAction(SettingsAction.UndoRestore) }
        } else {
            null
        }
    val onClearCache = { onAction(SettingsAction.ClearCache) }
    onExport?.let {
        SettingsRow(
            stringResource(R.string.backup_export),
            it,
            Modifier.fillMaxWidth().testTag("backup-export"),
            enabled = !backup.busy,
        )
    }
    onImport?.let {
        SettingsRow(
            stringResource(R.string.backup_import),
            it,
            Modifier.fillMaxWidth().testTag("backup-import"),
            enabled = !backup.busy,
        )
    }
    onUndoRestore?.let {
        SettingsRow(
            stringResource(R.string.backup_undo),
            it,
            Modifier.fillMaxWidth().testTag("backup-undo"),
            enabled = !backup.busy,
        )
    }
    if (onExport != null) Help(R.string.backup_assets_help)
    if (backup.failed) {
        Text(stringResource(R.string.backup_error), color = D.foreground)
    }
    Help(R.string.cache_help)
    Text(
        stringResource(R.string.cache_size, cacheBytes / 1024),
        color = D.secondary,
    )
    SettingsRow(
        stringResource(R.string.clear_image_cache),
        { onClearCache() },
        Modifier.fillMaxWidth().testTag("clear-image-cache"),
        symbol = LauncherSymbol.STORAGE,
        reduced = reduceMotion,
    )
}

@Composable
internal fun AboutSettings(versionName: String, reduceMotion: Boolean, onAndroidSettings: () -> Unit) {
    Help(R.string.about_body)
    Text(
        stringResource(R.string.version_label, versionName),
        color = D.secondary,
    )
    SettingsRow(
        stringResource(R.string.system_settings),
        { onAndroidSettings() },
        Modifier.fillMaxWidth(),
        symbol = LauncherSymbol.ANDROID,
        reduced = reduceMotion,
    )
}
private fun themeLabel(theme: ThemeMode) = when (theme) {
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SYSTEM -> R.string.theme_system
}

@Composable
internal fun SettingsChoice(title: Int, value: Int, tag: String, reduced: Boolean, onClick: () -> Unit) {
    SettingsRow(
        stringResource(title),
        onClick,
        Modifier.fillMaxWidth().testTag("settings-$tag"),
        value = stringResource(value),
        reduced = reduced,
    )
}

@Composable
internal fun SettingsToggle(title: Int, checked: Boolean, tag: String, reduced: Boolean, onClick: () -> Unit) {
    SettingsChoice(
        title,
        if (checked) R.string.enabled else R.string.disabled,
        tag,
        reduced,
        onClick,
    )
}

@Composable
internal fun Help(resource: Int) {
    Text(stringResource(resource), color = D.secondary, fontSize = 15.sp)
}
