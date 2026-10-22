package fr.deuspheara.oree.feature.settings.presentation.route

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.platform.image.WallpaperFrame
import fr.deuspheara.oree.feature.settings.presentation.screen.AmbientScreen
import kotlinx.coroutines.awaitCancellation

/** Lifecycle owns optional photo reads; the screen owns only input and slideshow presentation. */
@Composable
fun AmbientRoute(
    frame: WallpaperFrame?,
    photosEnabled: Boolean,
    showClock: Boolean,
    reduced: Boolean,
    loadPhotos: suspend () -> LocalPhotos,
    onClose: () -> Unit,
    photo: Uri? = null,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentLoad by rememberUpdatedState(loadPhotos)
    val photos by produceState(LocalPhotos(PhotoAvailability.EMPTY), photosEnabled, lifecycle) {
        value = LocalPhotos(PhotoAvailability.EMPTY)
        if (photosEnabled) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                value = currentLoad().let { it.copy(photos = it.photos.take(20)) }
                awaitCancellation()
            }
        }
    }
    AmbientScreen(frame, photos, photosEnabled, showClock, reduced, onClose, photo)
}
