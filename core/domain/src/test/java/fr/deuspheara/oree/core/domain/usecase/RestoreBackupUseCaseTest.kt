package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.domain.concurrency.PreferenceWriter
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreBackupUseCaseTest {
    private val snapshot = ProfileSnapshot("fixture", listOf(LocalProfile("fixture", "Fixture")))
    private val review = BackupReview(BackupCandidate(snapshot, 0), snapshot)

    private class BackupFixture(private val commit: suspend () -> Unit) : BackupRepository {
        override suspend fun restore(review: BackupReview) = commit()
        override suspend fun export(uri: String) = error("Unused fixture operation")
        override suspend fun read(uri: String): BackupCandidate = error("Unused fixture operation")
        override suspend fun previous(): BackupCandidate = error("Unused fixture operation")
    }

    @Test
    fun restoreWaitsForLatestPendingRevisionThenClearsOptimisticState() = runTest {
        val gate = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val writer = PreferenceWriter(backgroundScope) { _, value ->
            gate.await()
            events += "save:${value.highContrast}"
        }
        writer.submit("fixture", LauncherPreferences())
        runCurrent()
        writer.submit("fixture", LauncherPreferences(highContrast = true))
        val restoring = async { RestoreBackupUseCase(BackupFixture { events += "restore" })(review, writer) }
        runCurrent()
        assertFalse(restoring.isCompleted)
        gate.complete(Unit)
        runCurrent()
        restoring.await()
        assertEquals(listOf("save:false", "save:true", "restore"), events)
        assertTrue(writer.states.value.isEmpty())
    }

    @Test
    fun failedAtomicRestoreRetainsOptimisticStateForRetry() = runTest {
        val writer = PreferenceWriter(backgroundScope) { _, _ -> }
        writer.submit("fixture", LauncherPreferences(highContrast = true))
        runCurrent()
        var refused = false
        try {
            RestoreBackupUseCase(BackupFixture { throw IOException("stale snapshot") })(review, writer)
        } catch (_: IOException) {
            refused = true
        }
        assertTrue(refused)
        assertTrue(writer.states.value["fixture"]!!.value.highContrast)
        RestoreBackupUseCase(BackupFixture {})(review, writer)
        assertTrue(writer.states.value.isEmpty())
    }
}
