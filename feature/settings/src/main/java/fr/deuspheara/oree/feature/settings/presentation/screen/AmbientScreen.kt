package fr.deuspheara.oree.feature.settings.presentation.screen

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.HomeClock
import fr.deuspheara.oree.core.designsystem.glass.GlassDialog
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.platform.image.WallpaperFrame
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay

/** Manually opened foreground view. No idle detection, standby override or keep-awake flag. */
@Composable
fun AmbientScreen(
    frame: WallpaperFrame?,
    photosEnabled: Boolean,
    showClock: Boolean,
    reduced: Boolean,
    loadPhotos: suspend () -> LocalPhotos,
    onClose: () -> Unit,
    photo: Uri? = null,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val photos by
        produceState(LocalPhotos(PhotoAvailability.EMPTY), photosEnabled, lifecycle) {
            if (photosEnabled) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    value = loadPhotos().let { it.copy(photos = it.photos.take(20)) }
                    awaitCancellation()
                }
            }
        }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(photos.photos, lifecycle) {
        if (photosEnabled && photos.photos.size > 1) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    delay(30_000)
                    index = (index + 1) % photos.photos.size
                }
            }
        }
    }
    val selected =
        photo ?: photos.photos.getOrNull(index % photos.photos.size.coerceAtLeast(1))?.uri
    var photoFailed by remember(selected) { mutableStateOf(false) }
    var exitKey by remember { mutableStateOf<Key?>(null) }
    val first = remember { FocusRequester() }
    GlassDialog(onDismissRequest = onClose) {
        val closeLabel = stringResource(R.string.ambient_exit)
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black)
                .focusRequester(first)
                .onPreviewKeyEvent { event ->
                    if (
                        event.key in
                        listOf(
                            Key.DirectionLeft,
                            Key.DirectionRight,
                            Key.DirectionUp,
                            Key.DirectionDown,
                            Key.DirectionCenter,
                            Key.Enter,
                            Key.NumPadEnter,
                        )
                    ) {
                        // A TV button can open this window on key-down. Its unmatched opening
                        // release/repeats must not immediately close the new ambient view.
                        if (
                            event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0
                        ) {
                            exitKey = event.key
                        }
                        if (event.type == KeyEventType.KeyUp) {
                            val complete = exitKey == event.key
                            exitKey = null
                            if (complete) onClose()
                        }
                        true
                    } else {
                        false
                    }
                }
                .clickable(role = Role.Button, onClick = onClose)
                .semantics {
                    contentDescription = closeLabel
                    testTagsAsResourceId = true
                }
                .testTag("ambient"),
        ) {
            if (frame != null) {
                Image(
                    frame.image.asImageBitmap(),
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Crossfade(
                selected,
                animationSpec = tween(if (reduced) 0 else 300),
                label = "ambient-photo",
            ) { uri ->
                if (uri != null) {
                    val context = LocalContext.current
                    val request =
                        remember(uri) {
                            ImageRequest.Builder(context)
                                .data(uri)
                                .size(1280, 720)
                                .precision(coil3.size.Precision.EXACT)
                                .build()
                        }
                    AsyncImage(
                        request,
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onError = { photoFailed = true },
                    )
                }
            }
            if (showClock) HomeClock(Modifier.align(Alignment.BottomStart).padding(40.dp))
            if (
                photoFailed ||
                (
                    photosEnabled &&
                        photos.availability in
                        listOf(
                            PhotoAvailability.DENIED,
                            PhotoAvailability.EMPTY,
                            PhotoAvailability.ERROR,
                        )
                    )
            ) {
                Text(
                    stringResource(R.string.ambient_photo_fallback),
                    Modifier.align(Alignment.BottomEnd).padding(32.dp),
                    color = Color.White,
                )
            }
        }
        val ready = LocalWindowInfo.current.isWindowFocused
        LaunchedEffect(ready) {
            if (ready) {
                withFrameNanos {}
                first.requestFocus()
            }
        }
    }
}
