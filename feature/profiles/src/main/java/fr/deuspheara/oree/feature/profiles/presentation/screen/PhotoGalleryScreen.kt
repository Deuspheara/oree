package fr.deuspheara.oree.feature.profiles.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.component.panelMotion
import fr.deuspheara.oree.core.designsystem.component.rememberPanelTransition
import fr.deuspheara.oree.core.designsystem.focus.awaitRegisteredFocus
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.designsystem.glass.RetainedMenuSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.platform.image.LocalPhotoThumbnail

@Composable
fun PhotoGalleryScreen(
    photos: LocalPhotos?,
    permitted: Boolean,
    partialAccess: Boolean,
    reduced: Boolean,
    saving: Boolean,
    failed: Boolean,
    onChoose: (String) -> Unit,
    onSystemPicker: () -> Unit,
    onClose: () -> Unit,
    onRequestPermission: () -> Unit,
    onRetry: () -> Unit,
    sourceKey: Any?,
    onPreview: ((LocalPhoto) -> Unit)? = null,
) {
    val context = LocalContext.current
    val first = remember { FocusRequester() }
    val close = remember { FocusRequester() }
    val picker = remember { FocusRequester() }
    var returnToPicker by rememberSaveable { mutableStateOf(false) }
    val motion = rememberPanelTransition(reduced)
    val grid = rememberLazyGridState()
    val requesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var restorePending by remember { mutableStateOf(true) }
    GlassDialog(onDismissRequest = { if (!saving) motion.close(onClose) }) {
        Box(
            Modifier.fillMaxSize().padding(24.dp).onPreviewKeyEvent {
                // Initial loading may restore a thumbnail; subsequent user navigation owns focus.
                if (photos == null && it.type == KeyEventType.KeyDown) restorePending = false
                false
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.width(720.dp)
                    .heightIn(max = 480.dp)
                    .panelMotion(motion, reduced)
                    .clip(RoundedCornerShape(26.dp))
                    .testTag("photo-gallery"),
            ) {
                // Isolate the stationary material from thumbnail focus/scroll invalidation.
                RetainedMenuSurface(sourceKey, reduced, Modifier.matchParentSize())
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.photo_gallery),
                        color = DesignTokens.foreground,
                        fontSize = 24.sp,
                    )
                    Text(
                        stringResource(R.string.photo_gallery_notice),
                        color = DesignTokens.secondary,
                        fontSize = 13.sp,
                    )
                    if (!permitted || photos?.availability == PhotoAvailability.DENIED) {
                        SettingsRow(
                            stringResource(R.string.photo_gallery_allow),
                            onRequestPermission,
                            Modifier.focusRequester(first).testTag("photo-permission"),
                            symbol = LauncherSymbol.PRIVACY,
                        )
                    } else {
                        when (photos?.availability) {
                            null ->
                                Box(
                                    Modifier.fillMaxWidth().height(220.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        stringResource(R.string.loading),
                                        color = DesignTokens.foreground,
                                    )
                                }

                            PhotoAvailability.AVAILABLE ->
                                LazyVerticalGrid(
                                    GridCells.Adaptive(136.dp),
                                    state = grid,
                                    modifier = Modifier.fillMaxWidth().height(220.dp).testTag("local-photos"),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    itemsIndexed(
                                        photos!!.photos,
                                        key = { _, p -> p.uri.toString() },
                                        contentType = { _, _ -> "photo" },
                                    ) { index, photo ->
                                        val requester = remember(photo.uri) { FocusRequester() }
                                        DisposableEffect(photo.uri) {
                                            requesters[photo.uri] = requester
                                            onDispose { requesters.remove(photo.uri) }
                                        }
                                        var focused by remember(photo.uri) { mutableStateOf(false) }
                                        val interactions =
                                            remember(photo.uri) { MutableInteractionSource() }
                                        val pressed by interactions.collectIsPressedAsState()
                                        val image =
                                            remember(photo.uri) {
                                                val thumbnail = LocalPhotoThumbnail(photo.uri)
                                                ImageRequest.Builder(context)
                                                    .data(thumbnail)
                                                    .memoryCacheKey(thumbnail.cacheKey)
                                                    .size(240, 135)
                                                    .precision(coil3.size.Precision.INEXACT)
                                                    .build()
                                            }
                                        Box(
                                            Modifier.fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .zIndex(if (focused) 1f else 0f),
                                        ) {
                                            FocusArtwork(
                                                focused,
                                                pressed,
                                                reduced,
                                                Offset.Zero,
                                                Modifier.fillMaxSize(),
                                                shape = RoundedCornerShape(12.dp),
                                            ) {
                                                Box(
                                                    Modifier.fillMaxSize().background(DesignTokens.surface),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    LauncherIcon(LauncherSymbol.WALLPAPER, DesignTokens.secondary)
                                                    AsyncImage(
                                                        image,
                                                        null,
                                                        Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                }
                                            }
                                            Box(
                                                Modifier.matchParentSize()
                                                    .focusRequester(requester)
                                                    .onFocusChanged {
                                                        focused = it.isFocused
                                                        if (focused) selectedUri = photo.uri
                                                    }
                                                    .then(
                                                        if (index == 0) {
                                                            Modifier.focusRequester(first)
                                                        } else {
                                                            Modifier
                                                        },
                                                    )
                                                    .testTag("local-photo:$index")
                                                    .semantics { contentDescription = photo.name }
                                                    .combinedClickable(
                                                        enabled = !saving,
                                                        interactionSource = interactions,
                                                        indication = null,
                                                        role = Role.Button,
                                                        onClick = {
                                                            if (onPreview != null) onPreview(photo) else onChoose(photo.uri)
                                                        },
                                                    ),
                                            )
                                        }
                                    }
                                }

                            PhotoAvailability.EMPTY ->
                                Text(
                                    stringResource(R.string.photo_gallery_empty),
                                    color = DesignTokens.foreground,
                                )

                            else ->
                                SettingsRow(
                                    stringResource(R.string.retry),
                                    onRetry,
                                    Modifier.focusRequester(first),
                                )
                        }
                    }
                    if (
                        partialAccess
                    ) {
                        SettingsRow(
                            stringResource(R.string.photo_gallery_manage),
                            onRequestPermission,
                            enabled = !saving,
                            symbol = LauncherSymbol.PRIVACY,
                        )
                    }
                    if (failed) {
                        Text(
                            stringResource(R.string.wallpaper_error),
                            color = DesignTokens.foreground,
                            modifier = Modifier.testTag("photo-import-error"),
                        )
                    }
                    if (saving) {
                        Text(stringResource(R.string.wallpaper_applying), color = DesignTokens.foreground)
                    }
                    SettingsRow(
                        stringResource(R.string.photo_system_picker),
                        {
                            returnToPicker = true
                            onSystemPicker()
                        },
                        Modifier.focusRequester(picker).testTag("photo-other-source"),
                        enabled = !saving,
                        symbol = LauncherSymbol.OPEN,
                    )
                    SettingsRow(
                        stringResource(R.string.close),
                        { motion.close(onClose) },
                        Modifier.focusRequester(close).testTag("photo-close"),
                        enabled = !saving,
                        symbol = LauncherSymbol.CLOSE,
                    )
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready, permitted, photos?.availability) {
            if (!ready) {
                restorePending = true
                return@LaunchedEffect
            }
            if (!restorePending) return@LaunchedEffect
            if (returnToPicker) {
                withFrameNanos {}
                picker.requestFocus()
                returnToPicker = false
                restorePending = false
                return@LaunchedEffect
            }
            if (permitted && photos == null) {
                withFrameNanos {}
                close.requestFocus()
                return@LaunchedEffect
            }
            val entries = photos?.photos.orEmpty()
            if (permitted && entries.isNotEmpty()) {
                val index = entries.indexOfFirst { it.uri == selectedUri }.coerceAtLeast(0)
                grid.scrollToItem(index)
                withFrameNanos {}
                (awaitRegisteredFocus { requesters[entries[index].uri] } ?: close).requestFocus()
            } else {
                withFrameNanos {}
                if (!permitted || photos?.availability in listOf(PhotoAvailability.DENIED, PhotoAvailability.ERROR)) {
                    first.requestFocus()
                } else {
                    close.requestFocus()
                }
            }
            restorePending = false
        }
    }
}
