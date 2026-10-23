package fr.deuspheara.oree.feature.profiles.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.SettingsRow
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.LocalPhotos

/** Deliberate photo browsing; no ambient timer or direction-to-dismiss behavior. */
@Composable
fun PhotoViewerScreen(
    initial: LocalPhoto,
    collection: LocalPhotos?,
    reduced: Boolean,
    onClose: () -> Unit,
    onWallpaper: ((LocalPhoto) -> Unit)? = null,
) {
    var selectedUri by rememberSaveable(initial.uri) { mutableStateOf(initial.uri) }
    val photos = collection?.photos.orEmpty()
    val selected = photos.firstOrNull { it.uri == selectedUri } ?: photos.firstOrNull() ?: initial
    val index = photos.indexOfFirst { it.uri == selected.uri }
    var controls by rememberSaveable { mutableStateOf(false) }
    var failed by remember(selected.uri) { mutableStateOf(false) }
    var selectDown by remember { mutableStateOf(false) }
    val image = remember { FocusRequester() }
    val firstControl = remember { FocusRequester() }
    val context = LocalContext.current
    val request = remember(selected.uri) {
        ImageRequest.Builder(context).data(selected.uri).size(1280, 720)
            .precision(coil3.size.Precision.EXACT).build()
    }
    GlassDialog(onDismissRequest = { if (controls) controls = false else onClose() }) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).testTag("photo-viewer"),
        ) {
            Box(
                Modifier.fillMaxSize().focusRequester(image)
                    .onPreviewKeyEvent { event ->
                        when (event.key) {
                            Key.DirectionLeft, Key.DirectionRight -> {
                                if (event.type == KeyEventType.KeyDown && photos.size > 1) {
                                    val delta = if (event.key == Key.DirectionLeft) -1 else 1
                                    selectedUri = photos[(index + delta).coerceIn(0, photos.lastIndex)].uri
                                }
                                true
                            }

                            Key.DirectionUp, Key.DirectionDown -> true

                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) selectDown = true
                                if (event.type == KeyEventType.KeyUp && selectDown) {
                                    selectDown = false
                                    controls = true
                                }
                                true
                            }

                            else -> false
                        }
                    }.focusable().testTag("photo-viewer-image"),
            ) {
                AsyncImage(
                    request,
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onError = { failed = true },
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = .8f)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(selected.name, color = Color.White, modifier = Modifier.testTag("photo-viewer-name"))
                if (failed) Text(stringResource(R.string.photo_viewer_unavailable), color = Color.White)
                Text(
                    if (photos.size > 1) {
                        stringResource(R.string.photo_viewer_position, index + 1, photos.size)
                    } else {
                        stringResource(R.string.photo_viewer_hint)
                    },
                    color = Color.White,
                )
                if (controls) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsRow(
                            stringResource(R.string.close),
                            onClose,
                            Modifier.weight(1f).focusRequester(firstControl).testTag("photo-viewer-close"),
                            reduced = reduced,
                        )
                        if (onWallpaper != null) {
                            SettingsRow(
                                stringResource(R.string.photo_use_wallpaper),
                                { onWallpaper(selected) },
                                Modifier.weight(1f).testTag("photo-viewer-wallpaper"),
                                enabled = !failed,
                                reduced = reduced,
                            )
                        }
                    }
                }
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready, controls) {
            if (ready) {
                withFrameNanos {}
                if (controls) firstControl.requestFocus() else image.requestFocus()
            }
        }
    }
}
