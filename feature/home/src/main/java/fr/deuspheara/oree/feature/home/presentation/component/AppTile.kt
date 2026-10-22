package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.artwork.LocalArtworkReflections
import fr.deuspheara.oree.core.domain.rendering.ReflectionKey
import fr.deuspheara.oree.core.model.ArtworkKind
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.reflectionColors
import fr.deuspheara.oree.core.platform.image.ReflectionRequestKey
import fr.deuspheara.oree.feature.home.presentation.component.ArtworkPhase
import fr.deuspheara.oree.feature.home.presentation.component.FolderTile
import fr.deuspheara.oree.feature.home.presentation.component.HomeArtworkPlaceholder
import fr.deuspheara.oree.feature.home.presentation.state.HomeGridSlot
import fr.deuspheara.oree.feature.home.presentation.state.HomeNavigationMotion
import fr.deuspheara.oree.feature.home.presentation.state.homeArtworkSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTile(
    app: TvApp,
    requester: FocusRequester,
    reduceMotion: Boolean,
    slot: HomeGridSlot,
    navigation: HomeNavigationMotion,
    onFocus: () -> Unit,
    onClick: (Rect) -> Unit,
    onMenu: ((Rect) -> Unit)?,
    onUp: (() -> Unit)? = null,
    freshUpOnly: Boolean = false,
    loadingVisible: Boolean = true,
) {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    var tilt by remember { mutableStateOf(Offset.Zero) }
    var hoverTilt by remember { mutableStateOf<Offset?>(null) }
    var impulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(focused, impulse, reduceMotion) {
        tilt = if (focused && !reduceMotion) navigation.direction * .65f else Offset.Zero
        if (focused && !reduceMotion) {
            delay(90)
            tilt = Offset.Zero
        }
    }
    var artworkSize by remember { mutableStateOf(IntSize.Zero) }
    val reflectionKey =
        remember(app.artworkCacheKey, artworkSize, app.artworkKind) {
            ReflectionKey(
                app.artworkCacheKey,
                artworkSize.width,
                artworkSize.height,
                app.artworkKind == ArtworkKind.BANNER,
            )
        }
    val reflectionCache = LocalArtworkReflections.current
    var reflectionColors by
        remember(reflectionKey) { mutableStateOf(reflectionCache.colors(reflectionKey)) }
    var artworkPhase by
        remember(app.artworkCacheKey, artworkSize) { mutableStateOf(ArtworkPhase.WAITING) }
    var artworkVisible by remember { mutableStateOf(false) }
    val artworkWindow = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    Column(
        Modifier.zIndex(if (focused) 1f else 0f)
            .testTag("app-tile:${app.id}")
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).onSizeChanged {
                artworkSize = homeArtworkSize(it)
            },
            contentAlignment = Alignment.Center,
        ) {
            // Only pixels scale. The standard input surface above it owns a fixed spatial,
            // focus and accessibility rectangle throughout the animation.
            FocusArtwork(
                focused,
                pressed,
                reduceMotion,
                hoverTilt ?: tilt,
                Modifier.fillMaxWidth().fillMaxSize(),
                immediateFocus = true,
                reflectionColors = reflectionColors,
            ) {
                key(app.artworkCacheKey, artworkSize) {
                    HomeArtworkPlaceholder(
                        app.label,
                        artworkPhase,
                        artworkVisible && loadingVisible,
                        reduceMotion || !focused,
                    )
                }
                if (app.artwork != 0 && artworkSize != IntSize.Zero) {
                    val context = LocalContext.current
                    val request =
                        remember(reflectionKey, reduceMotion) {
                            ImageRequest.Builder(context)
                                .data("android.resource://${app.packageName}/${app.artwork}")
                                .memoryCacheKey(app.artworkCacheKey)
                                .size(artworkSize.width, artworkSize.height)
                                .precision(coil3.size.Precision.EXACT)
                                .apply {
                                    if (!reduceMotion) extras[ReflectionRequestKey] = reflectionKey
                                }
                                .build()
                        }
                    AsyncImage(
                        request,
                        contentDescription = null,
                        modifier =
                            if (app.artworkKind == ArtworkKind.ICON) {
                                Modifier.fillMaxSize().padding(12.dp)
                            } else {
                                Modifier.fillMaxSize()
                            },
                        contentScale =
                            if (app.artworkKind == ArtworkKind.BANNER) {
                                ContentScale.Crop
                            } else {
                                ContentScale.Fit
                            },
                        onLoading = { artworkPhase = ArtworkPhase.LOADING },
                        onSuccess = {
                            reflectionColors = reflectionCache.colors(reflectionKey)
                            artworkPhase = ArtworkPhase.READY
                        },
                        onError = { artworkPhase = ArtworkPhase.FAILED },
                    )
                }
            }
            Box(
                Modifier.matchParentSize()
                    .focusProperties {
                        start = if (slot.atStart) FocusRequester.Cancel else FocusRequester.Default
                        end = if (slot.atEnd) FocusRequester.Cancel else FocusRequester.Default
                    }
                    .focusRequester(requester)
                    .onGloballyPositioned {
                        bounds = it.boundsInWindow()
                        artworkVisible =
                            bounds.left < artworkWindow.width &&
                            bounds.right > 0 &&
                            bounds.top < artworkWindow.height &&
                            bounds.bottom > 0
                    }
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onFocus() else hoverTilt = null
                    }
                    .onPreviewKeyEvent {
                        if (it.key == Key.DirectionUp && onUp != null) {
                            if (
                                it.type == KeyEventType.KeyDown &&
                                (!freshUpOnly || it.nativeKeyEvent.repeatCount == 0)
                            ) {
                                onUp()
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (
                            it.type == KeyEventType.KeyDown &&
                            it.key in
                            listOf(
                                Key.DirectionLeft,
                                Key.DirectionRight,
                                Key.DirectionUp,
                                Key.DirectionDown,
                            )
                        ) {
                            hoverTilt = null
                            impulse++
                        }
                        false // Spatial traversal and held repeats remain Foundation-owned.
                    }
                    .pointerInput(reduceMotion) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (
                                    event.type == PointerEventType.Exit ||
                                    event.type == PointerEventType.Release
                                ) {
                                    hoverTilt = null
                                } else if (
                                    event.changes.firstOrNull()?.type == PointerType.Mouse &&
                                    (
                                        event.type == PointerEventType.Move ||
                                            event.type == PointerEventType.Enter
                                        )
                                ) {
                                    val position = event.changes.first().position
                                    requester.requestFocus()
                                    hoverTilt =
                                        Offset(
                                            (position.x / size.width.coerceAtLeast(1) * 2 - 1)
                                                .coerceIn(-1f, 1f),
                                            (position.y / size.height.coerceAtLeast(1) * 2 - 1)
                                                .coerceIn(-1f, 1f),
                                        )
                                }
                                // Observe only: dragging and click arbitration stay with
                                // combinedClickable.
                            }
                        }
                    }
                    .semantics { contentDescription = app.label }
                    .testTag("app:${app.id}")
                    .combinedClickable(
                        interactionSource = interactions,
                        indication = null,
                        role = Role.Button,
                        onLongClickLabel = stringResource(R.string.edit_apps),
                        onLongClick = onMenu?.let { { it(bounds) } },
                        onClick = { onClick(bounds) },
                    ),
            )
        }
    }
}
