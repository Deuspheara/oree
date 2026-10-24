package fr.deuspheara.oree.feature.profiles.presentation.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfileEditStatus
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfilesViewModel

/** Owns operation observation; editor drafts and native focus remain in the panel. */
@Composable
fun ProfilesRoute(
    model: ProfilesViewModel,
    snapshot: ProfileSnapshot,
    reduced: Boolean,
    opaque: Boolean,
    onCompleted: () -> Unit,
    onClose: () -> Unit,
    sourceKey: Any? = null,
    backdrop: ImageBitmap? = null,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.status) {
        if (state.status == ProfileEditStatus.COMPLETED) {
            onCompleted()
            model.acknowledge()
        }
    }
    ProfilePanel(
        snapshot, reduced, opaque,
        saving = state.status == ProfileEditStatus.SAVING,
        failed = state.status == ProfileEditStatus.FAILED,
        onSelect = model::select,
        onSave = model::save,
        onDelete = model::delete,
        onClose = {
            model.acknowledge()
            onClose()
        },
        sourceKey = sourceKey,
        backdrop = backdrop,
    )
}
