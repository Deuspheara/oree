package fr.deuspheara.oree.core.domain.concurrency

import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.LauncherPreferences
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceWriterTest {
    @Test
    fun rapidChangesKeepOnlyLatestPendingValueAndFlushWaitsForBothProfiles() = runTest {
        val gate = CompletableDeferred<Unit>()
        val saved = mutableListOf<Pair<String, LauncherPreferences>>()
        val writer =
            PreferenceWriter(backgroundScope) { id, value ->
                if (saved.isEmpty()) gate.await()
                saved += id to value
            }
        writer.submit("first", LauncherPreferences())
        runCurrent()
        repeat(1_000) { writer.submit("first", LauncherPreferences(textScale = 1f + it / 1_000f)) }
        writer.submit("second", LauncherPreferences(accent = AccentColor.GREEN))
        assertEquals(2, writer.pendingCount)
        val barrier = async { writer.flush() }
        runCurrent()
        assertFalse(barrier.isCompleted)
        gate.complete(Unit)
        runCurrent()
        barrier.await()
        assertEquals(listOf("first", "first", "second"), saved.map { it.first })
        assertEquals(1.999f, saved[1].second.textScale, 0.00001f)
        assertEquals(PreferenceSaveStatus.SAVED, writer.states.value["first"]!!.status)
        assertEquals(AccentColor.GREEN, writer.states.value["second"]!!.value.accent)
    }

    @Test
    fun failurePreventsBackupBarrierUntilExplicitRetrySucceeds() = runTest {
        var failing = true
        val writer =
            PreferenceWriter(backgroundScope) { _, _ -> if (failing) throw IOException("fixture") }
        writer.submit("profile", LauncherPreferences(accent = AccentColor.RED))
        runCurrent()
        var rejected = false
        try {
            writer.flush()
        } catch (_: IOException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(AccentColor.RED, writer.states.value["profile"]!!.value.accent)
        failing = false
        writer.retry("profile")
        runCurrent()
        writer.flush()
        writer.resetAfterRestore()
        assertTrue(writer.states.value.isEmpty())
    }

    @Test
    fun retiringProfileRemovesPendingWriteWithoutCancellingAcceptedTransaction() = runTest {
        val gate = CompletableDeferred<Unit>()
        val saved = mutableListOf<String>()
        val writer = PreferenceWriter(backgroundScope) { id, _ ->
            gate.await()
            saved += id
        }
        writer.submit("deleted", LauncherPreferences())
        runCurrent()
        writer.submit("deleted", LauncherPreferences(accent = AccentColor.RED))
        val barrier = async { writer.flush() }
        runCurrent()
        writer.retainProfiles(emptySet())
        runCurrent()
        barrier.await()
        assertEquals(0, writer.pendingCount)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("deleted"), saved)
        assertTrue(writer.states.value.isEmpty())
    }

    @Test
    fun cancellingFlushCallerDoesNotCancelAcceptedWriteOrAcknowledgeNewerRevision() = runTest {
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        var writes = 0
        val writer = PreferenceWriter(backgroundScope) { _, _ ->
            if (++writes == 1) first.await() else second.await()
        }
        writer.submit("profile", LauncherPreferences())
        runCurrent()
        val caller = async { writer.flush() }
        runCurrent()
        caller.cancel()
        writer.submit("profile", LauncherPreferences(accent = AccentColor.RED))
        first.complete(Unit)
        runCurrent()
        assertEquals(2, writes)
        assertEquals(PreferenceSaveStatus.SAVING, writer.states.value["profile"]!!.status)
        second.complete(Unit)
        runCurrent()
        writer.flush()
        assertEquals(AccentColor.RED, writer.states.value["profile"]!!.value.accent)
        assertEquals(PreferenceSaveStatus.SAVED, writer.states.value["profile"]!!.status)
    }
}
