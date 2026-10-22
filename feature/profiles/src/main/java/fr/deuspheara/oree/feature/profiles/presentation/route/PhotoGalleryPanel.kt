package fr.deuspheara.oree.feature.profiles.presentation.route

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fr.deuspheara.oree.core.model.LocalPhotos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// Only returns permission names. The explicit SDK parameter keeps API boundary tests deterministic.
@android.annotation.SuppressLint("InlinedApi")
fun photoReadPermissions(sdk: Int): Array<String> = when {
    sdk >= 34 ->
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

    sdk >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
fun PhotoGalleryPanel(
    reduced: Boolean,
    saving: Boolean,
    failed: Boolean,
    onChoose: (android.net.Uri) -> Unit,
    onSystemPicker: () -> Unit,
    onClose: () -> Unit,
    requestOnOpen: Boolean = true,
    loadPhotos: suspend () -> LocalPhotos,
    sourceKey: Any? = null,
) {
    val context = LocalContext.current
    val permissions = remember { photoReadPermissions(Build.VERSION.SDK_INT) }
    fun hasAccess() = permissions.any {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
    var permitted by remember { mutableStateOf(hasAccess()) }
    var epoch by remember { mutableIntStateOf(0) }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            epoch++
        }
    var requested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (requestOnOpen && !permitted && !requested) {
            requested = true
            request.launch(permissions)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var photos by remember { mutableStateOf<LocalPhotos?>(null) }
    val currentLoad by rememberUpdatedState(loadPhotos)
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { epoch }
                .collectLatest {
                    // Coalesce resume and permission callbacks before starting bounded IO.
                    delay(100)
                    permitted = hasAccess()
                    photos =
                        if (permitted) {
                            currentLoad()
                        } else {
                            null
                        }
                }
        }
    }
    fr.deuspheara.oree.feature.profiles.presentation.screen.PhotoGalleryScreen(
        photos, permitted,
        permitted && Build.VERSION.SDK_INT >= 34 &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED,
        reduced, saving, failed, { onChoose(android.net.Uri.parse(it)) }, onSystemPicker, onClose,
        { request.launch(permissions) }, { epoch++ }, sourceKey,
    )
}
