package fr.deuspheara.oree.core.designsystem.focus

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Lazy placement may register after scrolling. Removed items must not suspend restoration
 * forever. Callers own cancellation and choose the surviving fallback.
 */
suspend fun awaitRegisteredFocus(target: () -> FocusRequester?): FocusRequester? =
    withTimeoutOrNull(1_000) { snapshotFlow(target).filterNotNull().first() }
