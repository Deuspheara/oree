package fr.deuspheara.oree.feature.profiles.presentation.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.feature.profiles.presentation.screen.PhotoViewerScreen
import kotlinx.coroutines.awaitCancellation

@Composable
fun PhotoViewerRoute(
    initial: LocalPhoto,
    reduced: Boolean,
    loadPhotos: suspend () -> LocalPhotos,
    onClose: () -> Unit,
    onWallpaper: ((LocalPhoto) -> Unit)? = null,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentLoad by rememberUpdatedState(loadPhotos)
    val collection by produceState<LocalPhotos?>(null, lifecycle, initial.uri) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            value = currentLoad()
            awaitCancellation()
        }
    }
    PhotoViewerScreen(initial, collection, reduced, onClose, onWallpaper)
}
