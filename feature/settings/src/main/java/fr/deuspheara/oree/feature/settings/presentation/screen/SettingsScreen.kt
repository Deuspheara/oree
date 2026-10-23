package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.key
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
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvInput
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.feature.settings.presentation.component.InputSelectionMenu
import fr.deuspheara.oree.feature.settings.presentation.component.SettingsFocusScope
import fr.deuspheara.oree.feature.settings.presentation.component.restoreSettingsFocus
import fr.deuspheara.oree.feature.settings.presentation.state.AmbientSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.BackupSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.ContentSettingsState
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceAction
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
    initiallyInCategory: Boolean = false,
    systemReduceMotion: Boolean = false,
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
    var inCategory by rememberSaveable(initialCategory, initiallyInCategory) {
        mutableStateOf(initiallyInCategory || initialCategory != SettingsCategory.APPEARANCE)
    }
    var experienceOpen by rememberSaveable { mutableStateOf(false) }
    val entryCategory = remember(initialCategory) { category }
    val categoryRequesters = remember { SettingsCategory.entries.associateWith { FocusRequester() } }
    val categoryRegistered = remember { mutableStateMapOf<SettingsCategory, FocusRequester>() }
    val categoryScroll = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = entryCategory.ordinal)
    val contentRequester = remember { FocusRequester() }
    val sections = rememberSaveableStateHolder()
    var enterContent by remember { mutableStateOf(inCategory) }
    // Give the category pane a first frame before subcomposing the independent content list.
    // This bounds entry work without delaying input until the panel animation completes.
    var headingPlaced by remember { mutableStateOf(false) }
    var contentReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(headingPlaced) {
        if (headingPlaced) {
            withFrameNanos {}
            contentReady = true
        }
    }
    val motion = rememberPanelTransition(reduceMotion)
    fun change(preferences: LauncherPreferences) {
        if (!motion.closing) onChange(preferences)
    }
    val section = remember { Animatable(1f) }
    var previousCategory by remember { mutableStateOf(category) }
    LaunchedEffect(category, reduceMotion) {
        if (reduceMotion) {
            section.snapTo(1f)
        } else if (previousCategory != category) {
            previousCategory = category
            section.snapTo(0f)
            section.animateTo(1f, tween(MotionTokens.SECTION_MILLIS))
        }
    }
    LaunchedEffect(dismissRequested) { if (dismissRequested) motion.close(onClose) }
    BackHandler {
        if (inCategory) {
            inCategory = false
            enterContent = false
        } else {
            motion.close(onClose)
        }
    }
    LaunchedEffect(inCategory, windowFocused) {
        if (!inCategory && windowFocused) {
            if (categoryScroll.layoutInfo.visibleItemsInfo.none {
                    it.index == category.ordinal
                }
            ) {
                categoryScroll.scrollToItem(category.ordinal)
            }
            val target = fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus { categoryRegistered[category] }
            withFrameNanos {}
            target?.requestFocus()
        }
    }
    if (experienceOpen) {
        fr.deuspheara.oree.core.designsystem.glass.GlassDialog(onDismissRequest = { experienceOpen = false }) {
            VisualExperienceScreen(
                value,
                systemReducedMotion = systemReduceMotion,
                onAction = { action ->
                    when (action) {
                        is ExperienceAction.Apply -> {
                            change(action.value.copy(setupComplete = true))
                            experienceOpen = false
                        }

                        ExperienceAction.Cancel -> experienceOpen = false

                        else -> Unit
                    }
                },
            )
        }
    }
    Column(
        Modifier.fillMaxSize()
            .background(D.background.copy(alpha = if (value.reduceTransparency || value.highContrast) 1f else .78f))
            .padding(horizontal = D.gutter, vertical = 24.dp)
            .graphicsLayer {
                alpha = .4f + .6f * motion.progress.value
                translationX = if (reduceMotion) 0f else (1f - motion.progress.value) * 24.dp.toPx()
            }
            .semantics { testTagsAsResourceId = true }
            .testTag("launcher-settings"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(if (inCategory) categoryTitle(category) else R.string.launcher_settings),
                color = D.foreground,
                fontSize = 32.sp,
            )
            Spacer(Modifier.weight(1f))
        }
        androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            if (!inCategory) {
                LazyColumn(
                    Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("settings-categories"),
                    state = categoryScroll,
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(SettingsCategory.entries, key = { it.name }, contentType = { "category" }) { item ->
                        DisposableEffect(item) {
                            categoryRegistered[item] = categoryRequesters.getValue(item)
                            onDispose { categoryRegistered.remove(item) }
                        }
                        SettingsRow(
                            stringResource(categoryTitle(item)),
                            {
                                if (!motion.closing) {
                                    category = item
                                    inCategory = true
                                    enterContent = true
                                }
                            },
                            Modifier.fillMaxWidth()
                                .focusRequester(categoryRequesters.getValue(item))
                                .testTag("settings-category:${item.name}"),
                            symbol = categorySymbol(item),
                            selected = category == item,
                            reduced = reduceMotion,
                        )
                    }
                }
            } else {
                sections.SaveableStateProvider(category.name) {
                    SettingsFocusScope {
                        val contentScroll = androidx.compose.foundation.lazy.rememberLazyListState()
                        val restoreOption = fr.deuspheara.oree.feature.settings.presentation.component.rememberSettingsFocusRestoration(
                            contentScroll,
                            if (category == SettingsCategory.APPEARANCE) appearanceItemKeys(controls) else emptyList(),
                        )
                        LaunchedEffect(enterContent, motion.closing, contentReady) {
                            if (enterContent && !motion.closing && contentReady) {
                                if (!restoreOption()) contentRequester.requestFocus()
                                enterContent = false
                            }
                        }
                        LazyColumn(
                            Modifier.widthIn(max = 640.dp).fillMaxSize()
                                .focusRequester(contentRequester)
                                .restoreSettingsFocus()
                                .focusGroup()
                                .testTag("settings-content")
                                .graphicsLayer { alpha = section.value },
                            state = contentScroll,
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            item(key = "section-heading", contentType = "heading") {
                                Spacer(Modifier.height(6.dp).onGloballyPositioned { headingPlaced = true })
                            }
                            if (!contentReady) {
                                // The heading can render while controls wait for their first measure.
                            } else if (category == SettingsCategory.APPEARANCE) {
                                appearanceSettings(
                                    value,
                                    reduceMotion,
                                    ambient,
                                    ambientTrigger,
                                    ::change,
                                    { action ->
                                        if (action == SettingsAction.OpenAmbient) restoreAmbient = true
                                        onAction(action)
                                    },
                                    controls,
                                    onExperience = { experienceOpen = true },
                                )
                            } else {
                                item(key = "section:${category.name}", contentType = category.name) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        when (category) {
                                            SettingsCategory.APPEARANCE -> Unit

                                            SettingsCategory.HOME -> HomeSettings(
                                                value,
                                                reduceMotion,
                                                content,
                                                inputTrigger,
                                                { inputsOpen = true },
                                                onAction,
                                                controls,
                                            )

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

                                            SettingsCategory.ABOUT -> AboutSettings(versionName, reduceMotion, onAction, controls) {
                                                if (!motion.closing) onAndroidSettings()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (writeFailed) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(R.string.preferences_write_error),
                    color = D.foreground,
                    modifier = Modifier.testTag("settings-save-error"),
                )
                SettingsRow(
                    stringResource(R.string.retry),
                    onRetrySave,
                    Modifier.fillMaxWidth().testTag("settings-retry"),
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
