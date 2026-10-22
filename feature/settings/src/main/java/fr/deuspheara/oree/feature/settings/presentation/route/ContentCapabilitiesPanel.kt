package fr.deuspheara.oree.feature.settings.presentation.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import fr.deuspheara.oree.core.domain.repository.CapabilityRepository
import fr.deuspheara.oree.core.model.ContentCapabilities
import kotlinx.coroutines.awaitCancellation

@Composable
fun ContentCapabilitiesPanel(probe: CapabilityRepository, reduced: Boolean, onClose: () -> Unit) {
    var retry by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val report by
        produceState<ContentCapabilities?>(null, retry, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                value = null
                value = probe.inspect()
                awaitCancellation()
            }
        }
    fr.deuspheara.oree.feature.settings.presentation.screen.ContentCapabilitiesScreen(report, reduced, { retry++ }, onClose)
}
