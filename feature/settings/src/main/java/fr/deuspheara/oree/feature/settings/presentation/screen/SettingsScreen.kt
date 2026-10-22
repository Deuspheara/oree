package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.resource.contentStatus
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.feature.settings.presentation.component.AccentSelector
import fr.deuspheara.oree.feature.settings.presentation.component.GlassIntensityPreview
import fr.deuspheara.oree.feature.settings.presentation.component.InputSelectionMenu
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsCategory

@Composable
fun SettingsScreen(
    value: LauncherPreferences,
    onChange: (LauncherPreferences) -> Unit,
    onClose: () -> Unit,
    onAndroidSettings: () -> Unit,
    cacheBytes: Long,
    onClearCache: () -> Unit,
    onRetrySave: () -> Unit = {},
    onCustomizeControls: () -> Unit = {},
    writeFailed: Boolean = false,
    reduceMotion: Boolean = false,
    dismissRequested: Boolean = false,
    onWallpaper: (() -> Unit)? = null,
    onProfiles: (() -> Unit)? = null,
    watchNextEnabled: Boolean = false,
    contentAvailability: ContentAvailability = ContentAvailability.DISABLED,
    onWatchNext: () -> Unit = {},
    onRetryContent: () -> Unit = {},
    inputs: TvInputs = TvInputs(),
    onInput: (TvInput) -> Unit = {},
    initialCategory: SettingsCategory = SettingsCategory.APPEARANCE,
    onDiagnostics: (() -> Unit)? = null,
    appPreviewsEnabled: Boolean = false,
    onAppPreviews: (() -> Unit)? = null,
    appPreviewAvailability: ContentAvailability = ContentAvailability.DISABLED,
    onExport: (() -> Unit)? = null,
    onImport: (() -> Unit)? = null,
    onUndoRestore: (() -> Unit)? = null,
    backupBusy: Boolean = false,
    backupFailed: Boolean = false,
    onAmbient: (() -> Unit)? = null,
    ambientVisible: Boolean = false,
    ambientPhotos: Boolean = false,
    ambientClock: Boolean = true,
    onAmbientPhotos: (() -> Unit)? = null,
    onAmbientClock: (() -> Unit)? = null,
    onDreamSettings: (() -> Unit)? = null,
    versionName: String = "",
) {
    var inputsOpen by rememberSaveable { mutableStateOf(false) }
    var restoreInputs by remember { mutableStateOf(false) }
    val inputTrigger = remember { FocusRequester() }
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    val ambientTrigger = remember { FocusRequester() }
    var restoreAmbient by remember { mutableStateOf(false) }
    LaunchedEffect(ambientVisible, windowFocused) {
        if (!ambientVisible && restoreAmbient && windowFocused) {
            withFrameNanos {}
            ambientTrigger.requestFocus()
            restoreAmbient = false
        }
    }
    LaunchedEffect(inputsOpen, windowFocused) {
        if (!inputsOpen && restoreInputs && windowFocused) {
            withFrameNanos {}
            inputTrigger.requestFocus()
            restoreInputs = false
        }
    }
    if (inputsOpen) {
        InputSelectionMenu(inputs, reduceMotion, onInput) {
            inputsOpen = false
            restoreInputs = true
        }
    }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    val first = remember { FocusRequester() }
    var firstPlaced by remember { mutableStateOf(false) }
    val contentScroll = rememberScrollState()
    LaunchedEffect(category) { contentScroll.scrollTo(0) }
    val motion = rememberPanelTransition(reduceMotion)
    fun change(preferences: LauncherPreferences) {
        if (!motion.closing) onChange(preferences)
    }
    val section = remember { Animatable(1f) }
    LaunchedEffect(category, reduceMotion) {
        section.snapTo(if (reduceMotion) 1f else 0f)
        section.animateTo(1f, tween(if (reduceMotion) 0 else MotionTokens.SECTION_MILLIS))
    }
    LaunchedEffect(dismissRequested) { if (dismissRequested) motion.close(onClose) }
    BackHandler { motion.close(onClose) }
    LaunchedEffect(firstPlaced) {
        if (firstPlaced) {
            withFrameNanos {}
            first.requestFocus()
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(D.background)
            .padding(horizontal = D.gutter, vertical = 24.dp)
            .panelMotion(motion, reduceMotion, 24f, minimumAlpha = .4f)
            .semantics { testTagsAsResourceId = true }
            .testTag("launcher-settings"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.launcher_settings), color = D.foreground, fontSize = 32.sp)
            Spacer(Modifier.weight(1f))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                LazyColumn(
                    Modifier.weight(1f).testTag("settings-categories"),
                    state =
                        androidx.compose.foundation.lazy.rememberLazyListState(
                            initialFirstVisibleItemIndex = initialCategory.ordinal,
                        ),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(SettingsCategory.entries, key = { it.name }) { item ->
                        SettingsRow(
                            stringResource(categoryTitle(item)),
                            { if (!motion.closing) category = item },
                            Modifier.fillMaxWidth()
                                .then(
                                    if (item == initialCategory) {
                                        Modifier.focusRequester(first).onGloballyPositioned {
                                            firstPlaced = true
                                        }
                                    } else {
                                        Modifier
                                    },
                                )
                                .testTag("settings-category:${item.name}"),
                            symbol = categorySymbol(item),
                            selected = category == item,
                            reduced = reduceMotion,
                        )
                    }
                }
            }
            Column(
                Modifier.weight(1f)
                    .background(
                        D.surface,
                        androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                    )
                    .verticalScroll(contentScroll)
                    .testTag("settings-content")
                    .padding(24.dp)
                    .graphicsLayer { alpha = section.value },
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    LauncherIcon(categorySymbol(category), D.secondary, Modifier.size(32.dp))
                    Text(
                        stringResource(categoryTitle(category)),
                        color = D.foreground,
                        fontSize = 26.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                when (category) {
                    SettingsCategory.APPEARANCE -> {
                        SettingsChoice(
                            R.string.theme_label,
                            themeLabel(value.theme),
                            "theme",
                            reduceMotion,
                        ) {
                            change(
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
                            change(
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
                            change(value.copy(liquidGlass = !value.liquidGlass))
                        }
                        SettingsRow(
                            stringResource(R.string.glass_intensity),
                            {
                                change(
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
                        AccentSelector(value.accent) { change(value.copy(accent = it)) }
                        onAmbient?.let { action ->
                            SettingsRow(
                                stringResource(R.string.ambient_open),
                                {
                                    restoreAmbient = true
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
                                            if (ambientPhotos) {
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
                                            if (ambientClock) {
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
                            { change(LauncherPreferences()) },
                            Modifier.fillMaxWidth().testTag("settings-defaults"),
                            reduced = reduceMotion,
                        )
                    }

                    SettingsCategory.HOME -> {
                        onAppPreviews?.let {
                            SettingsRow(
                                stringResource(R.string.app_previews),
                                it,
                                Modifier.fillMaxWidth().testTag("settings-app-previews"),
                                value =
                                    stringResource(
                                        if (appPreviewsEnabled) {
                                            R.string.enabled
                                        } else {
                                            R.string.disabled
                                        },
                                    ),
                            )
                            Text(
                                stringResource(
                                    when (appPreviewAvailability) {
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
                            if (appPreviewAvailability == ContentAvailability.ERROR) {
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
                                    if (watchNextEnabled) R.string.enabled else R.string.disabled,
                                ),
                        )
                        Text(
                            stringResource(
                                when (contentAvailability) {
                                    ContentAvailability.AVAILABLE -> R.string.watch_next_available
                                    ContentAvailability.DISABLED -> R.string.watch_next_disabled
                                    ContentAvailability.DENIED -> R.string.watch_next_denied
                                    else -> contentStatus(contentAvailability)
                                },
                            ),
                            color = D.secondary,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("watch-next-status"),
                        )
                        if (contentAvailability == ContentAvailability.ERROR) {
                            SettingsRow(
                                stringResource(R.string.retry),
                                onRetryContent,
                                Modifier.fillMaxWidth().testTag("watch-next-retry"),
                            )
                        }
                        Help(R.string.watch_next_help)
                        SettingsRow(
                            stringResource(R.string.inputs),
                            { inputsOpen = true },
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

                    SettingsCategory.NAVIGATION -> {
                        Help(R.string.navigation_help)
                        SettingsToggle(
                            R.string.reduced_motion,
                            value.reduceMotion,
                            "motion",
                            reduceMotion,
                        ) {
                            change(value.copy(reduceMotion = !value.reduceMotion))
                        }
                        Help(R.string.motion_help)
                    }

                    SettingsCategory.ACCESSIBILITY -> {
                        SettingsToggle(
                            R.string.contrast_label,
                            value.highContrast,
                            "contrast",
                            reduceMotion,
                        ) {
                            change(value.copy(highContrast = !value.highContrast))
                        }
                        SettingsToggle(
                            R.string.transparency_label,
                            value.reduceTransparency,
                            "transparency",
                            reduceMotion,
                        ) {
                            change(value.copy(reduceTransparency = !value.reduceTransparency))
                        }
                        SettingsToggle(
                            R.string.reduced_motion,
                            value.reduceMotion,
                            "motion",
                            reduceMotion,
                        ) {
                            change(value.copy(reduceMotion = !value.reduceMotion))
                        }
                        SettingsRow(
                            stringResource(R.string.text_size),
                            {
                                change(
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

                    SettingsCategory.PRIVACY -> Help(R.string.privacy_body)

                    SettingsCategory.STORAGE -> {
                        onExport?.let {
                            SettingsRow(
                                stringResource(R.string.backup_export),
                                it,
                                Modifier.fillMaxWidth().testTag("backup-export"),
                                enabled = !backupBusy,
                            )
                        }
                        onImport?.let {
                            SettingsRow(
                                stringResource(R.string.backup_import),
                                it,
                                Modifier.fillMaxWidth().testTag("backup-import"),
                                enabled = !backupBusy,
                            )
                        }
                        onUndoRestore?.let {
                            SettingsRow(
                                stringResource(R.string.backup_undo),
                                it,
                                Modifier.fillMaxWidth().testTag("backup-undo"),
                                enabled = !backupBusy,
                            )
                        }
                        if (onExport != null) Help(R.string.backup_assets_help)
                        if (backupFailed) {
                            Text(stringResource(R.string.backup_error), color = D.foreground)
                        }
                        Help(R.string.cache_help)
                        Text(
                            stringResource(R.string.cache_size, cacheBytes / 1024),
                            color = D.secondary,
                        )
                        SettingsRow(
                            stringResource(R.string.clear_image_cache),
                            { if (!motion.closing) onClearCache() },
                            Modifier.fillMaxWidth().testTag("clear-image-cache"),
                            symbol = LauncherSymbol.STORAGE,
                            reduced = reduceMotion,
                        )
                    }

                    SettingsCategory.ABOUT -> {
                        Help(R.string.about_body)
                        Text(
                            stringResource(R.string.version_label, versionName),
                            color = D.secondary,
                        )
                        SettingsRow(
                            stringResource(R.string.system_settings),
                            { if (!motion.closing) onAndroidSettings() },
                            Modifier.fillMaxWidth(),
                            symbol = LauncherSymbol.ANDROID,
                            reduced = reduceMotion,
                        )
                    }
                }
            }
        }
        if (writeFailed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.preferences_write_error),
                    color = D.foreground,
                    modifier = Modifier.weight(1f).testTag("settings-save-error"),
                )
                SettingsRow(
                    stringResource(R.string.retry),
                    onRetrySave,
                    Modifier.width(160.dp).testTag("settings-retry"),
                )
            }
        }
    }
}

private fun categoryTitle(category: SettingsCategory) = when (category) {
    SettingsCategory.APPEARANCE -> R.string.appearance
    SettingsCategory.HOME -> R.string.home_settings
    SettingsCategory.NAVIGATION -> R.string.navigation
    SettingsCategory.ACCESSIBILITY -> R.string.accessibility
    SettingsCategory.PRIVACY -> R.string.privacy
    SettingsCategory.STORAGE -> R.string.storage
    SettingsCategory.ABOUT -> R.string.about
}

private fun themeLabel(theme: ThemeMode) = when (theme) {
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SYSTEM -> R.string.theme_system
}

@Composable
private fun SettingsChoice(title: Int, value: Int, tag: String, reduced: Boolean, onClick: () -> Unit) {
    SettingsRow(
        stringResource(title),
        onClick,
        Modifier.fillMaxWidth().testTag("settings-$tag"),
        value = stringResource(value),
        reduced = reduced,
    )
}

@Composable
private fun SettingsToggle(title: Int, checked: Boolean, tag: String, reduced: Boolean, onClick: () -> Unit) {
    SettingsChoice(
        title,
        if (checked) R.string.enabled else R.string.disabled,
        tag,
        reduced,
        onClick,
    )
}

@Composable
private fun Help(resource: Int) {
    Text(stringResource(resource), color = D.secondary, fontSize = 15.sp)
}

private fun categorySymbol(category: SettingsCategory) = when (category) {
    SettingsCategory.APPEARANCE -> LauncherSymbol.SETTINGS
    SettingsCategory.HOME -> LauncherSymbol.HOME
    SettingsCategory.NAVIGATION -> LauncherSymbol.NAVIGATION
    SettingsCategory.ACCESSIBILITY -> LauncherSymbol.ACCESSIBILITY
    SettingsCategory.PRIVACY -> LauncherSymbol.PRIVACY
    SettingsCategory.STORAGE -> LauncherSymbol.STORAGE
    SettingsCategory.ABOUT -> LauncherSymbol.ABOUT
}
