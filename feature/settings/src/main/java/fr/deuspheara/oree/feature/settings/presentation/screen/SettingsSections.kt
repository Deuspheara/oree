package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.resource.contentStatus
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.HomeContentLayout
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.feature.settings.presentation.component.AccentSelector
import fr.deuspheara.oree.feature.settings.presentation.component.GlassIntensityPreview
import fr.deuspheara.oree.feature.settings.presentation.component.PreferenceChoices
import fr.deuspheara.oree.feature.settings.presentation.component.PreferenceSlider
import fr.deuspheara.oree.feature.settings.presentation.component.settingsFocus
import fr.deuspheara.oree.feature.settings.presentation.state.AmbientSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.BackupSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.ContentSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsAction
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsControl

/** The focus restoration order matches the stable item keys emitted below. */
internal fun appearanceItemKeys(controls: Set<SettingsControl>): List<String> = buildList {
    add("section-heading")
    addAll(
        listOf("experience", "theme", "quality", "quality-help", "liquid-glass", "glass-intensity", "background-blur", "preview", "accent")
            .map { "appearance:$it" },
    )
    if (SettingsControl.AMBIENT in controls) add("appearance:ambient")
    add("appearance:defaults")
}

internal fun LazyListScope.appearanceSettings(
    value: LauncherPreferences,
    reduceMotion: Boolean,
    ambient: AmbientSettingsState,
    ambientTrigger: FocusRequester,
    onChange: (LauncherPreferences) -> Unit,
    onAction: (SettingsAction) -> Unit,
    controls: Set<SettingsControl>,
    onExperience: () -> Unit,
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
    item(key = "appearance:experience", contentType = "appearance:experience") {
        SettingsRow(
            stringResource(R.string.visual_experience),
            onExperience,
            Modifier.fillMaxWidth().settingsFocus("settings-experience"),
            reduced = reduceMotion,
        )
    }
    item(key = "appearance:theme", contentType = "appearance:theme") {
        PreferenceChoices(
            title = stringResource(R.string.theme_label),
            options = ThemeMode.entries.map { stringResource(themeLabel(it)) },
            selectedIndex = value.theme.ordinal,
            tag = "theme",
            reduced = reduceMotion,
        ) { onChange(value.copy(theme = ThemeMode.entries[it])) }
    }
    item(key = "appearance:quality", contentType = "appearance:quality") {
        PreferenceChoices(
            title = stringResource(R.string.quality_label),
            options = QualityTier.entries.map {
                stringResource(if (it == QualityTier.ECONOMY) R.string.quality_economy else R.string.quality_balanced)
            },
            selectedIndex = value.quality.ordinal,
            tag = "quality",
            reduced = reduceMotion,
        ) { onChange(value.copy(quality = QualityTier.entries[it])) }
    }
    item(key = "appearance:quality-help", contentType = "appearance:quality-help") {
        Help(R.string.quality_help)
    }
    item(key = "appearance:liquid-glass", contentType = "appearance:liquid-glass") {
        SettingsToggle(
            R.string.liquid_glass,
            value.liquidGlass,
            "liquid-glass",
            reduceMotion,
        ) {
            onChange(value.copy(liquidGlass = !value.liquidGlass))
        }
    }
    item(key = "appearance:glass-intensity", contentType = "appearance:glass-intensity") {
        val intensities = ((25..100 step 5).map { it / 100f } + value.glassIntensity).distinct().sorted()
        PreferenceSlider(
            title = stringResource(R.string.glass_intensity),
            description = stringResource(R.string.glass_help),
            options = intensities.map { "${(it * 100).toInt()}%" },
            selectedIndex = intensities.indexOf(value.glassIntensity).coerceAtLeast(0),
            tag = "glass-intensity",
            reduced = reduceMotion,
            enabled = value.liquidGlass,
        ) { onChange(value.copy(glassIntensity = intensities[it])) }
    }
    item(key = "appearance:background-blur", contentType = "appearance:background-blur") {
        val blur = ((0..100 step 10).map { it / 100f } + value.backdropBlur).distinct().sorted()
        PreferenceSlider(
            title = stringResource(R.string.background_blur),
            description = stringResource(R.string.background_blur_help),
            options = blur.map { "${(it * 100).toInt()}%" },
            selectedIndex = blur.indexOf(value.backdropBlur).coerceAtLeast(0),
            tag = "background-blur",
            reduced = reduceMotion,
        ) { onChange(value.copy(backdropBlur = blur[it])) }
    }
    item(key = "appearance:preview", contentType = "appearance:preview") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassIntensityPreview(value)
            Help(R.string.glass_help)
        }
    }
    item(key = "appearance:accent", contentType = "appearance:accent") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                stringResource(R.string.accent_color),
                color = D.secondary,
                fontSize = 15.sp,
            )
            AccentSelector(value.accent) { onChange(value.copy(accent = it)) }
        }
    }
    if (onAmbient != null) {
        item(key = "appearance:ambient", contentType = "appearance:ambient") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                onAmbient?.let { action ->
                    SettingsRow(
                        stringResource(R.string.ambient_open),
                        {
                            action()
                        },
                        Modifier.fillMaxWidth()
                            .focusRequester(ambientTrigger)
                            .settingsFocus("settings-ambient"),
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
                            Modifier.fillMaxWidth().settingsFocus("settings-dream"),
                        )
                    }
                    Help(R.string.ambient_help)
                }
            }
        }
    }
    item(key = "appearance:defaults", contentType = "appearance:defaults") {
        var resetOpen by rememberSaveable { mutableStateOf(false) }
        SettingsRow(
            stringResource(R.string.defaults),
            { resetOpen = true },
            Modifier.fillMaxWidth().settingsFocus("settings-defaults"),
            reduced = reduceMotion,
        )
        if (resetOpen) {
            fr.deuspheara.oree.feature.settings.presentation.component.ResetPreferencesConfirmation(
                reduceMotion,
                onConfirm = {
                    val defaults = LauncherPreferences()
                    onChange(
                        value.copy(
                            theme = defaults.theme, quality = defaults.quality,
                            reduceMotion = defaults.reduceMotion, reduceTransparency = defaults.reduceTransparency,
                            motionIntensity = defaults.motionIntensity, liveGlass = defaults.liveGlass,
                            highContrast = defaults.highContrast, textScale = defaults.textScale,
                            liquidGlass = defaults.liquidGlass, glassIntensity = defaults.glassIntensity, accent = defaults.accent,
                        ),
                    )
                    resetOpen = false
                },
                onClose = { resetOpen = false },
            )
        }
    }
}

@Composable
internal fun HomeSettings(
    value: LauncherPreferences,
    reduceMotion: Boolean,
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
    PreferenceChoices(
        title = stringResource(R.string.continue_layout),
        options = listOf(stringResource(R.string.continue_below), stringResource(R.string.continue_above)),
        selectedIndex = value.homeContentLayout.ordinal,
        tag = "continue-layout",
        reduced = reduceMotion,
    ) { onAction(SettingsAction.ChangePreferences(value.copy(homeContentLayout = HomeContentLayout.entries[it]))) }
    Help(R.string.continue_layout_help)
    val onWatchNext = { onAction(SettingsAction.ToggleWatchNext) }
    val onRetryContent = { onAction(SettingsAction.RetryContent) }
    val onCustomizeControls = { onAction(SettingsAction.CustomizeControls) }
    onAppPreviews?.let {
        SettingsRow(
            stringResource(R.string.app_previews),
            it,
            Modifier.fillMaxWidth().settingsFocus("settings-app-previews"),
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
    SettingsRow(
        stringResource(R.string.watch_next_home),
        onWatchNext,
        Modifier.fillMaxWidth().settingsFocus("settings-watch-next"),
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
        modifier = Modifier.settingsFocus("watch-next-status"),
    )
    if (content.watchNextAvailability == ContentAvailability.ERROR) {
        SettingsRow(
            stringResource(R.string.retry),
            onRetryContent,
            Modifier.fillMaxWidth().settingsFocus("watch-next-retry"),
        )
    }
    Help(R.string.watch_next_help)
    SettingsRow(
        stringResource(R.string.inputs),
        { onInputs() },
        Modifier.fillMaxWidth()
            .focusRequester(inputTrigger)
            .settingsFocus("settings-inputs"),
        symbol = LauncherSymbol.ANDROID,
    )
    onWallpaper?.let {
        SettingsRow(
            stringResource(R.string.wallpaper),
            it,
            Modifier.fillMaxWidth().settingsFocus("settings-wallpaper"),
            symbol = LauncherSymbol.WALLPAPER,
        )
    }
    onProfiles?.let {
        SettingsRow(
            stringResource(R.string.profiles),
            it,
            Modifier.fillMaxWidth().settingsFocus("settings-profiles"),
            symbol = LauncherSymbol.PROFILE,
        )
    }
    SettingsRow(
        stringResource(R.string.customize_controls),
        onCustomizeControls,
        Modifier.fillMaxWidth().settingsFocus("settings-customize-controls"),
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
    val scales = ((10..20).map { it / 10f } + value.textScale).distinct().sorted()
    PreferenceSlider(
        title = stringResource(R.string.text_size),
        description = stringResource(R.string.text_size_help),
        textSize = true,
        options = scales.map { "${(it * 100).toInt()}%" },
        selectedIndex = scales.indexOf(value.textScale).coerceAtLeast(0),
        tag = "text-size",
        reduced = reduceMotion,
    ) { onChange(value.copy(textScale = scales[it])) }
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
            Modifier.fillMaxWidth().settingsFocus("backup-export"),
            enabled = !backup.busy,
        )
    }
    onImport?.let {
        SettingsRow(
            stringResource(R.string.backup_import),
            it,
            Modifier.fillMaxWidth().settingsFocus("backup-import"),
            enabled = !backup.busy,
        )
    }
    onUndoRestore?.let {
        SettingsRow(
            stringResource(R.string.backup_undo),
            it,
            Modifier.fillMaxWidth().settingsFocus("backup-undo"),
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
        Modifier.fillMaxWidth().settingsFocus("clear-image-cache"),
        symbol = LauncherSymbol.STORAGE,
        reduced = reduceMotion,
    )
}

@Composable
internal fun AboutSettings(
    versionName: String,
    reduceMotion: Boolean,
    onAction: (SettingsAction) -> Unit,
    controls: Set<SettingsControl>,
    onAndroidSettings: () -> Unit,
) {
    Text(stringResource(R.string.support), color = D.secondary, fontSize = 18.sp)
    if (SettingsControl.DIAGNOSTICS in controls) {
        SettingsRow(
            stringResource(R.string.content_diagnostics),
            { onAction(SettingsAction.Diagnostics) },
            Modifier.fillMaxWidth().settingsFocus("settings-content-diagnostics"),
            symbol = LauncherSymbol.ABOUT,
            reduced = reduceMotion,
        )
    }
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
        Modifier.fillMaxWidth().settingsFocus("settings-$tag"),
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
