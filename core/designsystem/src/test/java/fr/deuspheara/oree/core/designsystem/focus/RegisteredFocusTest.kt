package fr.deuspheara.oree.core.designsystem.focus

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisteredFocusTest {
    @Test fun removedTargetFinishesWithinThePlacementBudget() = runTest {
        assertNull(awaitRegisteredFocus { null })
        assertEquals(1_000L, testScheduler.currentTime)
    }

    @Test fun delayedRegistrationResolvesTheCurrentRequester() = runTest {
        val targets = mutableStateMapOf<String, FocusRequester>()
        val pending = async { awaitRegisteredFocus { targets["app"] } }
        runCurrent()
        val requester = FocusRequester()
        targets["app"] = requester
        Snapshot.sendApplyNotifications()
        assertSame(requester, pending.await())
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun obsoleteRestorationRemainsCancellable() = runTest {
        val pending = async { awaitRegisteredFocus { null } }
        runCurrent()
        pending.cancel()
        runCurrent()
        assertTrue(pending.isCancelled)
        assertEquals(0L, testScheduler.currentTime)
    }
}
