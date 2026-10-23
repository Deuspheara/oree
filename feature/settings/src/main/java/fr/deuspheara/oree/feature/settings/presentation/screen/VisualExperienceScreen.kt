package fr.deuspheara.oree.feature.settings.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.LocalMotionIntensity
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.VisualExperience
import fr.deuspheara.oree.feature.settings.presentation.component.GlassIntensityPreview
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceAction
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceControl
import fr.deuspheara.oree.feature.settings.presentation.state.ExperienceSaveStatus
import kotlin.math.roundToInt

/** Draft-only preview. Only the explicit final action submits one profile-scoped write. */
@Composable
fun VisualExperienceScreen(
    value: LauncherPreferences,
    onAction: (ExperienceAction) -> Unit,
    welcome: Boolean = false,
    systemReducedMotion: Boolean = false,
    saveStatus: ExperienceSaveStatus = ExperienceSaveStatus.IDLE,
    controls: Set<ExperienceControl> = emptySet(),
) {
    val saving = saveStatus == ExperienceSaveStatus.SAVING
    val failed = saveStatus == ExperienceSaveStatus.FAILED
    var submitted by remember { mutableStateOf(false) }
    val busy = !failed && (saving || submitted)
    LaunchedEffect(saveStatus) { if (failed) submitted = false }
    fun submit(action: ExperienceAction) {
        if (!submitted) {
            submitted = true
            onAction(action)
        }
    }
    var page by rememberSaveable { mutableIntStateOf(if (welcome) 0 else 1) }
    var index by rememberSaveable { mutableIntStateOf(VisualExperience.nearest(value).ordinal) }
    val level = VisualExperience.entries[index]
    val draft = remember(value, level) { level.applyTo(value) }
    val initial = remember { FocusRequester() }
    val finish = remember { FocusRequester() }
    val retry = remember { FocusRequester() }
    var retryPlaced by remember { mutableStateOf(false) }
    var placed by remember(page) { mutableStateOf(false) }
    val window = LocalWindowInfo.current
    val ready = window.isWindowFocused
    LaunchedEffect(page, ready, placed) {
        if (ready && placed) {
            withFrameNanos {}
            initial.requestFocus()
        }
    }
    LaunchedEffect(failed, ready, retryPlaced) {
        if (failed && ready && retryPlaced) {
            withFrameNanos {}
            retry.requestFocus()
        }
    }
    BackHandler {
        if (!busy) {
            if (welcome && page == 1) page = 0 else onAction(ExperienceAction.Cancel)
        }
    }
    Column(
        Modifier.fillMaxSize().background(D.background)
            .semantics { testTagsAsResourceId = true }.testTag("visual-experience")
            .padding(horizontal = 48.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(
                    if (page ==
                        0
                    ) {
                        R.string.setup_welcome
                    } else {
                        R.string.visual_experience
                    },
                ),
                color = D.foreground,
                fontSize = 32.sp,
            )
            Text(
                stringResource(
                    if (page ==
                        0
                    ) {
                        R.string.setup_welcome_body
                    } else {
                        R.string.experience_body
                    },
                ),
                color = D.secondary,
                fontSize = 17.sp,
            )
            if (page == 0) {
                SettingsRow(
                    stringResource(R.string.setup_choose),
                    { page = 1 },
                    Modifier.fillMaxWidth().focusRequester(initial).onGloballyPositioned { placed = true }.testTag("setup-start"),
                    enabled = !busy && !failed,
                )
                if (ExperienceControl.KEEP_CURRENT in controls) {
                    SettingsRow(
                        stringResource(R.string.setup_keep),
                        { submit(ExperienceAction.KeepCurrent) },
                        Modifier.fillMaxWidth().testTag("setup-keep-current"),
                        enabled = !busy && !failed,
                    )
                }
            } else {
                Text(stringResource(experienceTitle(level)), color = D.foreground, fontSize = 24.sp)
                ExperienceRail(index, !busy && !failed, initial, { placed = true }, { index = it }, { finish.requestFocus() })
                Text(stringResource(R.string.experience_rail_hint), color = D.secondary, fontSize = 14.sp)
                CompositionLocalProvider(LocalMotionIntensity provides draft.motionIntensity) {
                    GlassIntensityPreview(
                        draft,
                        previewFocus = index % 2,
                        reducedMotion =
                            systemReducedMotion || draft.reduceMotion || draft.quality == fr.deuspheara.oree.core.model.QualityTier.ECONOMY,
                    )
                }
                Text(stringResource(experienceDescription(level)), color = D.secondary, fontSize = 16.sp)
                SettingsRow(
                    stringResource(if (welcome) R.string.setup_finish else R.string.experience_apply),
                    { submit(ExperienceAction.Apply(draft)) },
                    Modifier.fillMaxWidth().focusRequester(finish).testTag("experience-apply"),
                    enabled = !busy && !failed,
                )
            }
            if (busy) Text(stringResource(R.string.saving), color = D.secondary, modifier = Modifier.testTag("setup-saving"))
            if (failed) {
                Text(stringResource(R.string.preferences_write_error), color = D.foreground)
                SettingsRow(stringResource(R.string.retry), {
                    submit(ExperienceAction.Retry)
                }, Modifier.fillMaxWidth().focusRequester(retry).onGloballyPositioned { retryPlaced = true }.testTag("setup-retry"))
            }
        }
    }
}

@Composable
private fun ExperienceRail(
    index: Int,
    enabled: Boolean,
    initial: FocusRequester,
    onPlaced: () -> Unit,
    onChange: (Int) -> Unit,
    onSelect: () -> Unit,
) {
    val direction = LocalLayoutDirection.current
    var focused by remember { mutableStateOf(false) }
    val foreground = D.foreground
    val label = stringResource(R.string.visual_experience)
    val selectedLabel = stringResource(experienceTitle(VisualExperience.entries[index]))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.experience_less), color = D.secondary, fontSize = 14.sp)
        Canvas(
            Modifier.weight(1f).height(66.dp).focusRequester(initial)
                .onGloballyPositioned { onPlaced() }
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (!enabled) return@onPreviewKeyEvent true
                    when (event.key) {
                        Key.DirectionLeft, Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                val delta = if ((event.key == Key.DirectionRight) == (direction == LayoutDirection.Ltr)) 1 else -1
                                onChange((index + delta).coerceIn(VisualExperience.entries.indices))
                            }
                            true
                        }

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type == KeyEventType.KeyUp) onSelect()
                            true
                        }

                        else -> false
                    }
                }
                .semantics {
                    contentDescription = label
                    stateDescription = selectedLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(index.toFloat(), 0f..4f, 3)
                    setProgress {
                        if (!enabled || !it.isFinite()) {
                            false
                        } else {
                            onChange(it.roundToInt().coerceIn(0, 4))
                            true
                        }
                    }
                }.testTag("experience-rail").focusable(enabled),
        ) {
            val inset = 22.dp.toPx()
            val width = (size.width - inset * 2).coerceAtLeast(0f)
            val y = size.height / 2
            val height = 6.dp.toPx()
            drawRoundRect(foreground.copy(alpha = .2f), Offset(inset, y - height / 2), Size(width, height), CornerRadius(height))
            repeat(5) {
                val fraction = if (direction == LayoutDirection.Rtl) 1f - it / 4f else it / 4f
                drawCircle(foreground.copy(alpha = .55f), 3.dp.toPx(), Offset(inset + width * fraction, y))
            }
            val fraction = if (direction == LayoutDirection.Rtl) 1f - index / 4f else index / 4f
            val thumbWidth = 40.dp.toPx()
            val thumbHeight = (if (focused) 22.dp else 18.dp).toPx()
            drawRoundRect(
                foreground.copy(alpha = if (focused) 1f else .7f),
                Offset(
                    inset + width * fraction - thumbWidth / 2,
                    y - thumbHeight / 2,
                ),
                Size(thumbWidth, thumbHeight),
                CornerRadius(thumbHeight),
            )
        }
        Text(stringResource(R.string.experience_more), color = D.secondary, fontSize = 14.sp)
    }
}

private fun experienceTitle(level: VisualExperience) = when (level) {
    VisualExperience.SIMPLE -> R.string.experience_simple
    VisualExperience.LIGHT -> R.string.experience_light
    VisualExperience.BALANCED -> R.string.experience_balanced
    VisualExperience.RICH -> R.string.experience_rich
    VisualExperience.FULL -> R.string.experience_full
}
private fun experienceDescription(level: VisualExperience) = when (level) {
    VisualExperience.SIMPLE -> R.string.experience_simple_body
    VisualExperience.LIGHT -> R.string.experience_light_body
    VisualExperience.BALANCED -> R.string.experience_balanced_body
    VisualExperience.RICH -> R.string.experience_rich_body
    VisualExperience.FULL -> R.string.experience_full_body
}
