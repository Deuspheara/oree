package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.copy
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.columns
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rows
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.GlassSurfaceRole
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.glass.glassControlForeground
import fr.deuspheara.oree.core.designsystem.glass.menuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.ControlPlacement
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.ControlSize
import fr.deuspheara.oree.core.model.defaultControlSize
import fr.deuspheara.oree.core.model.placeControls
import fr.deuspheara.oree.core.platform.status.ControlStatus
import fr.deuspheara.oree.core.platform.status.NetworkKind

private data class ControlGeometry(val x: State<Dp>, val y: State<Dp>, val width: State<Dp>, val height: State<Dp>)

/** Four bounded controls, with native modal focus and a D-pad resize transaction. */
@Composable
fun CommandPanel(
    reduceMotion: Boolean,
    reduceTransparency: Boolean,
    onSettings: () -> Unit,
    onClose: () -> Unit,
    onWallpaper: (() -> Unit)? = null,
    onNetwork: (() -> Unit)? = null,
    onAudio: (() -> Unit)? = null,
    onAdjustVolume: ((Int) -> Unit)? = null,
    status: ControlStatus = ControlStatus(),
    sizes: Map<ControlId, ControlSize> = emptyMap(),
    positions: Map<ControlId, ControlPosition> = emptyMap(),
    onLayout: (List<ControlPlacement>) -> Unit = {},
    onResize: (ControlId, ControlSize) -> Unit = { _, _ -> },
    initiallyCustomizing: Boolean = false,
    writeFailed: Boolean = false,
    onRetrySave: () -> Unit = {},
    onEditingChanged: (Boolean) -> Unit = {},
    sourceKey: Any? = null,
    backdrop: ImageBitmap? = null,
) {
    val motion = rememberPanelTransition(reduceMotion)
    val first = remember { FocusRequester() }
    val audioFirst = remember { FocusRequester() }
    var audioExpanded by remember { mutableStateOf(false) }
    var panelBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var activeControl by remember { mutableStateOf(ControlId.SETTINGS) }
    val handle = remember { FocusRequester() }
    val requesters = remember { ControlId.entries.associateWith { FocusRequester() } }
    var editing by remember {
        mutableStateOf<ControlId?>(if (initiallyCustomizing) ControlId.SETTINGS else null)
    }
    var resizing by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var candidatePosition by remember {
        mutableStateOf(positions[ControlId.SETTINGS] ?: ControlPosition())
    }
    var candidate by remember {
        mutableStateOf(sizes[ControlId.SETTINGS] ?: defaultControlSize(ControlId.SETTINGS))
    }
    var awaitingRelease by remember { mutableStateOf(false) }
    var settingsOpening by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf<ControlId?>(null) }
    val editingCallback by rememberUpdatedState(onEditingChanged)
    LaunchedEffect(editing) { editingCallback(editing != null) }
    DisposableEffect(Unit) { onDispose { editingCallback(false) } }
    fun finishResize(save: Boolean) {
        val id = editing ?: return
        if (save) {
            onResize(id, candidate)
            onLayout(
                placeControls(
                    ControlId.entries,
                    sizes + (id to candidate),
                    positions + (id to candidatePosition),
                    id,
                ),
            )
        }
        editing = null
        resizing = false
        moving = false
        restore = id
    }
    fun dismiss() {
        when {
            audioExpanded -> {
                audioExpanded = false
                restore = ControlId.AUDIO
            }

            editing != null -> finishResize(false)

            else -> motion.close(onClose)
        }
    }
    val ids =
        listOfNotNull(
            ControlId.SETTINGS,
            ControlId.WALLPAPER.takeIf { onWallpaper != null },
            ControlId.NETWORK.takeIf { onNetwork != null },
            ControlId.AUDIO.takeIf { onAudio != null },
        )
    val effective = if (editing != null) sizes + (editing!! to candidate) else sizes
    val effectivePositions =
        if (editing != null) positions + (editing!! to candidatePosition) else positions
    val placements = placeControls(ids, effective, effectivePositions, editing)
    LaunchedEffect(editing) {
        if (editing != null) {
            candidate = sizes[editing] ?: defaultControlSize(editing!!)
            val current = placeControls(ids, sizes, positions).first { it.id == editing }
            candidatePosition = ControlPosition(current.column, current.row)
        }
    }
    GlassDialog(onDismissRequest = ::dismiss) {
        ControlCenterScene(backdrop) { underlaySource ->
            Box(Modifier.fillMaxSize()) {
                ControlCenterBackdrop(
                    backdrop,
                    reduceTransparency || LocalOpaquePanels.current,
                    { motion.progress.value },
                    panelBounds,
                    underlaySource,
                )
            }
            BackHandler { dismiss() }
            val windowFocused = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(windowFocused, editing, audioExpanded) {
                if (windowFocused) {
                    withFrameNanos {}
                    if (audioExpanded) {
                        audioFirst.requestFocus()
                    } else if (editing != null) {
                        handle.requestFocus()
                    } else if (restore == null) {
                        requesters.getValue(activeControl).requestFocus()
                    }
                }
            }
            LaunchedEffect(windowFocused, restore) {
                if (windowFocused) {
                    restore?.let {
                        withFrameNanos {}
                        requesters.getValue(it).requestFocus()
                        restore = null
                    }
                }
            }
            Box(
                Modifier.fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        val key = event.nativeKeyEvent
                        if (
                            awaitingRelease &&
                            key.keyCode in
                            listOf(
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER,
                                android.view.KeyEvent.KEYCODE_NUMPAD_ENTER,
                            )
                        ) {
                            if (key.action == android.view.KeyEvent.ACTION_UP) {
                                awaitingRelease = false
                                true
                            } else if (key.repeatCount > 0) {
                                true
                            } else {
                                awaitingRelease = false
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .padding(start = 32.dp, end = 32.dp, top = 80.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                val cellHeight = 72.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                Column(
                    Modifier.width(cellHeight * 4 + 42.dp)
                        .onGloballyPositioned {
                            panelBounds = androidx.compose.ui.geometry.Rect(
                                it.positionInWindow(),
                                androidx.compose.ui.geometry.Size(it.size.width.toFloat(), it.size.height.toFloat()),
                            )
                        }
                        .panelMotion(motion, reduceMotion, 32f)
                        .verticalScroll(rememberScrollState())
                        .padding(6.dp)
                        .semantics { testTagsAsResourceId = true }
                        .testTag("command-panel"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (audioExpanded) {
                        Text(
                            stringResource(R.string.control_sound),
                            color = glassControlForeground(),
                            fontSize = 22.sp,
                            modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                        )
                    }
                    if (audioExpanded) {
                        ControlVolumeSlider(
                            status,
                            reduceMotion,
                            onAdjustVolume,
                            Modifier.focusRequester(audioFirst),
                        )
                        SettingsRow(stringResource(R.string.audio_settings), { onAudio?.invoke() }, reduced = reduceMotion)
                    } else {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            val gap = 10.dp
                            val cellWidth = (maxWidth - gap * 3) / 4
                            val resizeSpec: FiniteAnimationSpec<Dp> =
                                if (reduceMotion) snap() else tween(210, easing = FastOutSlowInEasing)
                            val geometry = placements.map { placement ->
                                key(placement.id) {
                                    ControlGeometry(
                                        animateDpAsState(
                                            (cellWidth + gap) * placement.column,
                                            resizeSpec,
                                            label = "control-x",
                                        ),
                                        animateDpAsState(
                                            (cellHeight + gap) * placement.row,
                                            resizeSpec,
                                            label = "control-y",
                                        ),
                                        animateDpAsState(
                                            cellWidth * placement.size.columns +
                                                gap * (placement.size.columns - 1),
                                            resizeSpec,
                                            label = "control-width",
                                        ),
                                        animateDpAsState(
                                            cellHeight * placement.size.rows +
                                                gap * (placement.size.rows - 1),
                                            resizeSpec,
                                            label = "control-height",
                                        ),
                                    )
                                }
                            }
                            val rows = if (editing != null) 4 else placements.maxOf { it.row + it.size.rows }
                            val totalHeight =
                                animateDpAsState(
                                    (cellHeight + gap) * rows - gap,
                                    resizeSpec,
                                    label = "controls-height",
                                )
                            val gridColor = D.foreground.copy(alpha = .08f)
                            Layout(
                                modifier =
                                    Modifier.drawBehind {
                                        if (editing != null) {
                                            repeat(4) { y ->
                                                repeat(4) { x ->
                                                    if (
                                                        placements.none {
                                                            x in
                                                                it.column until
                                                                it.column + it.size.columns &&
                                                                y in it.row until it.row + it.size.rows
                                                        }
                                                    ) {
                                                        drawRoundRect(
                                                            gridColor,
                                                            Offset(
                                                                (cellWidth + gap).toPx() * x,
                                                                (cellHeight + gap).toPx() * y,
                                                            ),
                                                            Size(cellWidth.toPx(), cellHeight.toPx()),
                                                            androidx.compose.ui.geometry.CornerRadius(
                                                                36.dp.toPx(),
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                content = {
                                    ids.forEach { id ->
                                        key(id) {
                                            val label =
                                                when (id) {
                                                    ControlId.SETTINGS ->
                                                        stringResource(R.string.launcher_settings)

                                                    ControlId.WALLPAPER ->
                                                        stringResource(R.string.wallpaper)

                                                    ControlId.NETWORK ->
                                                        when (status.network) {
                                                            NetworkKind.WIFI -> "Wi-Fi"

                                                            NetworkKind.ETHERNET -> "Ethernet"

                                                            NetworkKind.VPN -> "VPN"

                                                            else ->
                                                                stringResource(R.string.network_settings)
                                                        }

                                                    ControlId.AUDIO ->
                                                        stringResource(R.string.control_sound)
                                                }
                                            val volume = status.volume
                                            val maxVolume = status.maxVolume
                                            val detail =
                                                when (id) {
                                                    ControlId.NETWORK ->
                                                        stringResource(
                                                            when {
                                                                status.network == NetworkKind.UNKNOWN ->
                                                                    R.string.status_unavailable

                                                                status.network == NetworkKind.OFFLINE ->
                                                                    R.string.network_offline

                                                                status.validated ->
                                                                    R.string.network_validated

                                                                else -> R.string.network_unvalidated
                                                            },
                                                        )

                                                    ControlId.AUDIO -> when {
                                                        status.volumeFixed -> stringResource(R.string.control_volume_fixed)

                                                        status.muted -> stringResource(R.string.control_muted)

                                                        volume != null && maxVolume != null && maxVolume > 0 ->
                                                            stringResource(R.string.control_volume_percent, volume * 100 / maxVolume)

                                                        else -> stringResource(R.string.status_unavailable)
                                                    }

                                                    else -> null
                                                }
                                            val symbol =
                                                when (id) {
                                                    ControlId.SETTINGS -> LauncherSymbol.SETTINGS
                                                    ControlId.WALLPAPER -> LauncherSymbol.WALLPAPER
                                                    ControlId.NETWORK -> LauncherSymbol.NETWORK
                                                    ControlId.AUDIO -> LauncherSymbol.AUDIO
                                                }
                                            val available =
                                                when (id) {
                                                    ControlId.NETWORK ->
                                                        status.network !in
                                                            listOf(NetworkKind.UNKNOWN, NetworkKind.OFFLINE)

                                                    ControlId.AUDIO -> status.audio.isNotEmpty()

                                                    else -> false
                                                }
                                            Box(
                                                Modifier.fillMaxSize().zIndex(if (editing == id) 1f else 0f),
                                            ) {
                                                val size = effective[id] ?: defaultControlSize(id)
                                                QuickControl(
                                                    label,
                                                    symbol,
                                                    detail,
                                                    available,
                                                    size,
                                                    Modifier.fillMaxSize()
                                                        .focusRequester(requesters.getValue(id))
                                                        .onFocusChanged { if (it.isFocused) activeControl = id }
                                                        .then(
                                                            if (id == ControlId.SETTINGS) {
                                                                Modifier.focusRequester(first)
                                                            } else {
                                                                Modifier
                                                            },
                                                        )
                                                        .onPreviewKeyEvent { event ->
                                                            if (
                                                                editing == null &&
                                                                placements.first { it.id == id }.row ==
                                                                placements.minOf { it.row } &&
                                                                event.key == Key.DirectionUp
                                                            ) {
                                                                if (event.type == KeyEventType.KeyDown) {
                                                                    motion.close(onClose)
                                                                }
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        .testTag("command-${id.name.lowercase()}"),
                                                    !motion.closing && !settingsOpening && editing == null,
                                                    reduceTransparency,
                                                    reduceMotion,
                                                    sourceKey,
                                                    onLongClick = {
                                                        if (editing == null) {
                                                            candidate = sizes[id] ?: defaultControlSize(id)
                                                            awaitingRelease = true
                                                            editing = id
                                                        }
                                                    },
                                                    onClick = {
                                                        when (id) {
                                                            ControlId.SETTINGS -> if (!settingsOpening) {
                                                                settingsOpening = true
                                                                onSettings()
                                                            }

                                                            ControlId.WALLPAPER -> motion.close(onWallpaper!!)

                                                            ControlId.NETWORK -> {
                                                                restore = id
                                                                onNetwork?.invoke()
                                                            }

                                                            ControlId.AUDIO -> audioExpanded = true
                                                        }
                                                    },
                                                )
                                                if (editing == id) {
                                                    var handleFocused by remember { mutableStateOf(false) }
                                                    val handleDescription =
                                                        stringResource(R.string.resize_control, label)
                                                    val sizeText =
                                                        if (moving) {
                                                            stringResource(
                                                                R.string.control_position,
                                                                candidatePosition.column + 1,
                                                                candidatePosition.row + 1,
                                                            )
                                                        } else {
                                                            stringResource(
                                                                R.string.control_size,
                                                                candidate.columns,
                                                                candidate.rows,
                                                            )
                                                        }
                                                    val gripColor =
                                                        if (LocalHighContrast.current) {
                                                            D.foreground
                                                        } else {
                                                            Color.White
                                                        }
                                                    val interactions = remember {
                                                        MutableInteractionSource()
                                                    }
                                                    val pressed by interactions.collectIsPressedAsState()
                                                    val lift =
                                                        animateFloatAsState(
                                                            if (handleFocused && !reduceMotion) 1f else 0f,
                                                            if (reduceMotion) {
                                                                snap()
                                                            } else {
                                                                spring(
                                                                    dampingRatio = .82f,
                                                                    stiffness = 550f,
                                                                )
                                                            },
                                                            label = "handle-lift",
                                                        )
                                                    val compression =
                                                        animateFloatAsState(
                                                            if (pressed && !reduceMotion) .975f else 1f,
                                                            if (reduceMotion) {
                                                                snap()
                                                            } else {
                                                                spring(
                                                                    dampingRatio = .9f,
                                                                    stiffness = 1100f,
                                                                )
                                                            },
                                                            label = "handle-press",
                                                        )
                                                    Box(
                                                        Modifier.align(Alignment.BottomEnd)
                                                            .size(40.dp)
                                                            .focusRequester(handle)
                                                            .onFocusChanged { handleFocused = it.isFocused }
                                                            .testTag("resize:${id.name}")
                                                            .semantics {
                                                                contentDescription = handleDescription
                                                                stateDescription = sizeText
                                                            }
                                                            .onPreviewKeyEvent { event ->
                                                                if (
                                                                    (resizing || moving) &&
                                                                    event.key in
                                                                    listOf(
                                                                        Key.DirectionLeft,
                                                                        Key.DirectionRight,
                                                                        Key.DirectionUp,
                                                                        Key.DirectionDown,
                                                                    )
                                                                ) {
                                                                    if (
                                                                        event.type == KeyEventType.KeyDown
                                                                    ) {
                                                                        val previousSize = candidate
                                                                        val previousPosition =
                                                                            candidatePosition
                                                                        if (moving) {
                                                                            candidatePosition =
                                                                                when (event.key) {
                                                                                    Key.DirectionLeft ->
                                                                                        candidatePosition
                                                                                            .copy(
                                                                                                column =
                                                                                                    candidatePosition
                                                                                                        .column -
                                                                                                        1,
                                                                                            )

                                                                                    Key.DirectionRight ->
                                                                                        candidatePosition
                                                                                            .copy(
                                                                                                column =
                                                                                                    candidatePosition
                                                                                                        .column +
                                                                                                        1,
                                                                                            )

                                                                                    Key.DirectionUp ->
                                                                                        candidatePosition
                                                                                            .copy(
                                                                                                row =
                                                                                                    candidatePosition
                                                                                                        .row -
                                                                                                        1,
                                                                                            )

                                                                                    else ->
                                                                                        candidatePosition
                                                                                            .copy(
                                                                                                row =
                                                                                                    candidatePosition
                                                                                                        .row +
                                                                                                        1,
                                                                                            )
                                                                                }.normalized(candidate)
                                                                        } else {
                                                                            candidate =
                                                                                when (event.key) {
                                                                                    Key.DirectionLeft ->
                                                                                        candidate.copy(
                                                                                            columns = 1,
                                                                                        )

                                                                                    Key.DirectionRight ->
                                                                                        candidate.copy(
                                                                                            columns = 2,
                                                                                        )

                                                                                    Key.DirectionUp ->
                                                                                        candidate.copy(
                                                                                            rows = 1,
                                                                                        )

                                                                                    else ->
                                                                                        candidate.copy(
                                                                                            rows = 2,
                                                                                        )
                                                                                }
                                                                        }
                                                                        candidatePosition =
                                                                            candidatePosition.normalized(
                                                                                candidate,
                                                                            )
                                                                        val preview =
                                                                            placeControls(
                                                                                ids,
                                                                                sizes +
                                                                                    (id to candidate),
                                                                                positions +
                                                                                    (
                                                                                        id to
                                                                                            candidatePosition
                                                                                        ),
                                                                                id,
                                                                            )
                                                                                .first { it.id == id }
                                                                        if (
                                                                            preview.column !=
                                                                            candidatePosition.column ||
                                                                            preview.row !=
                                                                            candidatePosition.row
                                                                        ) {
                                                                            candidate = previousSize
                                                                            candidatePosition =
                                                                                previousPosition
                                                                        }
                                                                    }
                                                                    true
                                                                } else {
                                                                    false
                                                                }
                                                            }
                                                            .combinedClickable(
                                                                interactionSource = interactions,
                                                                indication = null,
                                                                role = Role.Button,
                                                                onClick = {
                                                                    if (resizing || moving) {
                                                                        finishResize(true)
                                                                    } else {
                                                                        resizing = true
                                                                    }
                                                                },
                                                            ),
                                                    ) {
                                                        Canvas(
                                                            Modifier.fillMaxSize().graphicsLayer {
                                                                val scale =
                                                                    if (reduceMotion) {
                                                                        1f
                                                                    } else {
                                                                        (
                                                                            1f +
                                                                                (
                                                                                    MotionTokens.FOCUS_SCALE -
                                                                                        1f
                                                                                    ) * lift.value
                                                                            ) *
                                                                            compression.value
                                                                    }
                                                                scaleX = scale
                                                                scaleY = scale
                                                                transformOrigin = TransformOrigin(.1f, .1f)
                                                                clip = false
                                                            },
                                                        ) {
                                                            // Follow the capsule's 36dp corner; the input
                                                            // target stays 40dp.
                                                            val origin =
                                                                Offset(-32.dp.toPx(), -32.dp.toPx())
                                                            val arcSize = Size(72.dp.toPx(), 72.dp.toPx())
                                                            val shine =
                                                                Brush.linearGradient(
                                                                    listOf(
                                                                        gripColor.copy(alpha = .8f),
                                                                        gripColor,
                                                                        gripColor.copy(alpha = .9f),
                                                                    ),
                                                                    Offset(8.dp.toPx(), 36.dp.toPx()),
                                                                    Offset(36.dp.toPx(), 8.dp.toPx()),
                                                                )
                                                            drawArc(
                                                                shine,
                                                                15f,
                                                                60f,
                                                                false,
                                                                origin,
                                                                arcSize,
                                                                style =
                                                                    Stroke(
                                                                        6.dp.toPx(),
                                                                        cap = StrokeCap.Round,
                                                                    ),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            ) { measurables, constraints ->
                                val children = measurables.mapIndexed { index, measurable ->
                                    val bounds = geometry[index]
                                    measurable.measure(
                                        Constraints.fixed(
                                            bounds.width.value.roundToPx().coerceAtLeast(1),
                                            bounds.height.value.roundToPx().coerceAtLeast(1),
                                        ),
                                    )
                                }
                                layout(
                                    constraints.maxWidth,
                                    totalHeight.value.roundToPx().coerceAtLeast(0),
                                ) {
                                    children.forEachIndexed { index, child ->
                                        val bounds = geometry[index]
                                        child.placeRelative(
                                            bounds.x.value.roundToPx(),
                                            bounds.y.value.roundToPx(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (editing != null && !resizing && !moving) {
                        Column(
                            Modifier.fillMaxWidth()
                                .menuSurface(opaque = reduceTransparency)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!resizing && !moving) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    SettingsRow(
                                        stringResource(R.string.move_control),
                                        {
                                            moving = true
                                            handle.requestFocus()
                                        },
                                        Modifier.weight(1f).testTag("control-move"),
                                        compact = true,
                                    )
                                    SettingsRow(
                                        stringResource(R.string.resize_action),
                                        {
                                            resizing = true
                                            handle.requestFocus()
                                        },
                                        Modifier.weight(1f).testTag("control-resize"),
                                        compact = true,
                                    )
                                }
                            }
                        }
                    }
                    if (writeFailed) {
                        Text(stringResource(R.string.preferences_write_error), color = D.foreground)
                        SettingsRow(
                            stringResource(R.string.retry),
                            onRetrySave,
                            Modifier.fillMaxWidth().testTag("controls-retry"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickControl(
    label: String,
    symbol: LauncherSymbol,
    detail: String?,
    available: Boolean,
    size: ControlSize,
    modifier: Modifier,
    enabled: Boolean,
    opaque: Boolean,
    reduced: Boolean,
    sourceKey: Any?,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val lift = animateFloatAsState(
        if (focused && !reduced) 1f else 0f,
        if (reduced) snap() else spring(dampingRatio = .92f, stiffness = 500f),
        label = "control-focus-lift",
    )
    val focusColor by androidx.compose.animation.animateColorAsState(
        if (focused) D.foreground else Color.Transparent,
        if (reduced) snap() else tween(140),
        label = "control-focus-surface",
    )
    val foreground by androidx.compose.animation.animateColorAsState(
        if (focused) D.background else glassControlForeground(),
        if (reduced) snap() else tween(140),
        label = "control-focus-foreground",
    )
    val interactions = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(36.dp)
    val accent = if (focused) D.focusedAccent else D.accent
    Box(
        modifier
            .graphicsLayer {
                scaleX = 1f + .035f * lift.value
                scaleY = scaleX
            }
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                if (detail != null) stateDescription = detail
            },
    ) {
        CompositionLocalProvider(LocalOpaquePanels provides (opaque || LocalOpaquePanels.current)) {
            RetainedMenuSurface(
                sourceKey,
                reduced,
                Modifier.matchParentSize(),
                role = GlassSurfaceRole.CONTROL,
                radius = 36.dp,
                progressive = remember { dev.chrisbanes.haze.HazeProgressive.verticalGradient(startIntensity = .65f, endIntensity = 1f) },
            )
        }
        Box(
            Modifier.matchParentSize()
                .background(focusColor, shape)
                .padding(
                    start = if (size == ControlSize(1, 2)) 6.dp else 18.dp,
                    end = if (size == ControlSize(1, 2)) 6.dp else 18.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (size == ControlSize(1, 1)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LauncherIcon(
                        symbol,
                        if (available) accent else foreground,
                        Modifier.size(24.dp),
                    )
                }
            } else if (size == ControlSize(1, 2)) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    LauncherIcon(
                        symbol,
                        if (available) accent else foreground,
                        Modifier.size(24.dp),
                    )
                    ControlText(label, foreground, 12.sp, focused && !reduced)
                    if (detail != null) {
                        ControlText(
                            detail,
                            foreground.copy(alpha = .8f),
                            11.sp,
                            focused && !reduced,
                        )
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherIcon(
                        symbol,
                        if (available) accent else foreground,
                        Modifier.size(24.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ControlText(label, foreground, 14.sp, focused && !reduced)
                        if (detail != null) {
                            ControlText(
                                detail,
                                foreground.copy(alpha = .8f),
                                11.sp,
                                focused && !reduced,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Only a focused, overflowing line animates; idle and reduced-motion text is ellipsized. */
@Composable
private fun ControlText(value: String, color: Color, size: androidx.compose.ui.unit.TextUnit, animate: Boolean) {
    Text(
        value,
        color = color,
        fontSize = size,
        maxLines = 1,
        softWrap = false,
        overflow =
            if (animate) {
                androidx.compose.ui.text.style.TextOverflow.Clip
            } else {
                androidx.compose.ui.text.style.TextOverflow.Ellipsis
            },
        modifier =
            Modifier.fillMaxWidth()
                .then(
                    if (animate) {
                        Modifier.basicMarquee(iterations = 1, initialDelayMillis = 900)
                    } else {
                        Modifier
                    },
                ),
    )
}
