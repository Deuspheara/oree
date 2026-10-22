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
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.feature.settings.presentation.component.InputSelectionMenu
import fr.deuspheara.oree.feature.settings.presentation.state.AmbientSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.BackupSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.ContentSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsAction
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsCategory
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsControl

@Composable
fun SettingsScreen(
    value: LauncherPreferences,
    onAction: (SettingsAction) -> Unit,
    cacheBytes: Long,
    writeFailed: Boolean = false,
    reduceMotion: Boolean = false,
    dismissRequested: Boolean = false,
    inputs: TvInputs = TvInputs(),
    initialCategory: SettingsCategory = SettingsCategory.APPEARANCE,
    versionName: String = "",
    content: ContentSettingsState = ContentSettingsState(),
    backup: BackupSettingsState = BackupSettingsState(),
    ambient: AmbientSettingsState = AmbientSettingsState(),
    controls: Set<SettingsControl> = emptySet(),
) {
    val onClose = { onAction(SettingsAction.Close) }
    val onAndroidSettings = { onAction(SettingsAction.AndroidSettings) }
    val onRetrySave = { onAction(SettingsAction.RetrySave) }
    val onChange = { value: LauncherPreferences -> onAction(SettingsAction.ChangePreferences(value)) }
    val onInput = { input: TvInput -> onAction(SettingsAction.OpenInput(input)) }
    var inputsOpen by rememberSaveable { mutableStateOf(false) }
    var restoreInputs by remember { mutableStateOf(false) }
    val inputTrigger = remember { FocusRequester() }
    val windowFocused = androidx.compose.ui.platform.LocalWindowInfo.current.isWindowFocused
    val ambientTrigger = remember { FocusRequester() }
    var restoreAmbient by remember { mutableStateOf(false) }
    LaunchedEffect(ambient.visible, windowFocused) {
        if (!ambient.visible && restoreAmbient && windowFocused) {
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
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    val entryCategory = remember(initialCategory) { category }
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
                            initialFirstVisibleItemIndex = entryCategory.ordinal,
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
                                    if (item == entryCategory) {
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
                    SettingsCategory.APPEARANCE -> AppearanceSettings(value, reduceMotion, ambient, ambientTrigger, ::change, { action ->
                        if (action == SettingsAction.OpenAmbient) restoreAmbient = true
                        onAction(action)
                    }, controls)

                    SettingsCategory.HOME -> HomeSettings(content, inputTrigger, { inputsOpen = true }, onAction, controls)

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

                    SettingsCategory.ACCESSIBILITY -> AccessibilitySettings(value, reduceMotion, ::change)

                    SettingsCategory.PRIVACY -> Help(R.string.privacy_body)

                    SettingsCategory.STORAGE -> StorageSettings(backup, cacheBytes, reduceMotion, { action ->
                        if (action != SettingsAction.ClearCache || !motion.closing) onAction(action)
                    }, controls)

                    SettingsCategory.ABOUT -> AboutSettings(versionName, reduceMotion, { if (!motion.closing) onAndroidSettings() })
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

private fun categorySymbol(category: SettingsCategory) = when (category) {
    SettingsCategory.APPEARANCE -> LauncherSymbol.SETTINGS
    SettingsCategory.HOME -> LauncherSymbol.HOME
    SettingsCategory.NAVIGATION -> LauncherSymbol.NAVIGATION
    SettingsCategory.ACCESSIBILITY -> LauncherSymbol.ACCESSIBILITY
    SettingsCategory.PRIVACY -> LauncherSymbol.PRIVACY
    SettingsCategory.STORAGE -> LauncherSymbol.STORAGE
    SettingsCategory.ABOUT -> LauncherSymbol.ABOUT
}
