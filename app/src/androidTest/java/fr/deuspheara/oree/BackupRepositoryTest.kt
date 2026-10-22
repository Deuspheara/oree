package fr.deuspheara.oree

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.repository.DataStoreProfileRepository
import fr.deuspheara.oree.core.data.repository.SafBackupRepository
import fr.deuspheara.oree.core.data.serialization.MAX_BACKUP_BYTES
import fr.deuspheara.oree.core.data.serialization.encodeBackup
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.AvatarIds
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.WallpaperMode
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupRepositoryTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val document = Uri.parse("content://fr.deuspheara.oree.backupfixture/document")

    private suspend fun isolated(
        test:
        suspend (
            DataStoreProfileRepository,
            SafBackupRepository,
            androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        ) -> Unit,
    ) {
        val file =
            File(context.cacheDir, "backup-test-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        val profiles = DataStoreProfileRepository(store)
        try {
            profiles.initialize(
                LauncherPreferences(accent = AccentColor.PURPLE),
                AppOrganization(listOf("org.example.alpha/.TV", "org.example.bravo/.TV")),
            )
            test(profiles, SafBackupRepository(context, profiles), store)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
            File(context.cacheDir, "backup-fixture.json").delete()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun committedReadRecoversFailedObservationWithoutAStopStartCycle() = kotlinx.coroutines.test.runTest {
        val file = File(context.cacheDir, "profile-retry-${java.util.UUID.randomUUID()}.preferences_pb")
        val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val actual = PreferenceDataStoreFactory.create(scope = storageScope, produceFile = { file })
        val failReads = java.util.concurrent.atomic.AtomicBoolean(true)
        val faulty = object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = kotlinx.coroutines.flow.flow {
                if (failReads.get()) throw IOException("Temporary read failure")
                actual.data.collect { emit(it) }
            }
            override suspend fun updateData(
                transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences,
            ) = actual.updateData(transform)
        }
        try {
            DataStoreProfileRepository(actual).initialize(LauncherPreferences(), AppOrganization())
            val profiles = DataStoreProfileRepository(faulty, backgroundScope)
            backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
                profiles.snapshots.collect()
            }
            assertTrue(profiles.snapshots.first { it?.failed == true }!!.failed)
            failReads.set(false)
            assertEquals("default", profiles.readSnapshot()!!.activeId)
            assertEquals("default", profiles.snapshots.first { it != null && !it.failed }!!.activeId)
        } finally {
            backgroundScope.coroutineContext[Job]!!.cancelAndJoin()
            storageScope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun exportReadsCommittedPreferencesWhenSharedObservationIsDelayed() = kotlinx.coroutines.test.runTest {
        val file = File(context.cacheDir, "export-race-${java.util.UUID.randomUUID()}.preferences_pb")
        val storageScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val releaseObservation = kotlinx.coroutines.CompletableDeferred<Unit>()
        val actual = PreferenceDataStoreFactory.create(scope = storageScope, produceFile = { file })
        // Each new collection gets the current committed value; an existing observer is delayed.
        val delayed = object : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
            override val data = kotlinx.coroutines.flow.flow {
                val initial = actual.data.first()
                emit(initial)
                actual.data.dropWhile { it == initial }.collect {
                    releaseObservation.await()
                    emit(it)
                }
            }
            override suspend fun updateData(
                transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences,
            ) = actual.updateData(transform)
        }
        val profiles = DataStoreProfileRepository(delayed, backgroundScope)
        try {
            profiles.initialize(LauncherPreferences(accent = AccentColor.PURPLE), AppOrganization())
            backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
                profiles.snapshots.collect()
            }
            val original = profiles.snapshots.first { it != null }!!
            profiles.setPreferences(original.activeId, LauncherPreferences(accent = AccentColor.GREEN))
            assertEquals(AccentColor.PURPLE, profiles.snapshots.first()!!.active!!.preferences.accent)
            val backups = SafBackupRepository(context, profiles)
            backups.export(document)
            assertEquals(AccentColor.GREEN, backups.read(document).snapshot.active!!.preferences.accent)
        } finally {
            releaseObservation.complete(Unit)
            backgroundScope.coroutineContext[Job]!!.cancelAndJoin()
            storageScope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
            File(context.cacheDir, "backup-fixture.json").delete()
        }
    }

    @Test
    fun documentRoundtripRestoreAndUndoPreserveProfileOrganization() = runBlocking {
        isolated { profiles, backups, _ ->
            val id = profiles.snapshots.first()!!.activeId
            profiles.createFolder(id, "Films", "org.example.alpha/.TV")
            val originalImage = "wallpaper-${java.util.UUID.randomUUID()}.jpg"
            profiles.setImage(id, originalImage)
            profiles.setWallpaper(id, WallpaperMode.LANDSCAPE)
            val original = profiles.snapshots.first()!!
            backups.export(document)
            val candidate = backups.read(document)
            assertEquals(
                original.copy(profiles = original.profiles.map { it.copy(image = null) }),
                candidate.snapshot,
            )
            // Use a metadata-only candidate so the same fixture also exercises rollback image
            // ownership.
            val restored = candidate.snapshot
            profiles.rename(id, "Changed", AvatarIds.DEFAULT)
            val changed = profiles.snapshots.first()!!
            backups.restore(BackupReview(candidate, changed))
            assertEquals(restored, profiles.snapshots.first())
            assertTrue(profiles.canUndoRestore.first())
            assertTrue(profiles.referencesImage(originalImage))
            assertFalse(profiles.referencesImage("wallpaper-unused.jpg"))
            val undo = backups.previous()
            backups.restore(BackupReview(undo, restored, true))
            assertEquals(
                changed.copy(profiles = changed.profiles.map { it.copy(image = null) }),
                profiles.snapshots.first(),
            )
            assertFalse(profiles.canUndoRestore.first())
            assertFalse(profiles.referencesImage(originalImage))
        }
    }

    @Test
    fun invalidOversizedRevokedAndStaleImportsRetainConfiguration() = runBlocking {
        isolated { profiles, backups, store ->
            val original = profiles.snapshots.first()!!
            val before = store.data.first()[stringPreferencesKey("profiles_v1")]
            val encoded = encodeBackup(original)
            val unsafe =
                JSONObject(encoded)
                    .apply {
                        getJSONObject("profiles")
                            .getJSONArray("profiles")
                            .getJSONObject(0)
                            .getJSONArray("favorites")
                            .put(0, "file:///outside")
                    }
                    .toString()
            val invalid =
                listOf(
                    "invalid",
                    JSONObject(encoded).put("version", 99).toString(),
                    "[".repeat(25),
                    unsafe,
                )
            invalid.forEachIndexed { index, raw ->
                File(context.cacheDir, "backup-fixture.json").writeText(raw)
                try {
                    backups.read(document)
                    fail("Invalid backup fixture $index accepted")
                } catch (_: IOException) {}
                assertEquals(before, store.data.first()[stringPreferencesKey("profiles_v1")])
            }
            File(context.cacheDir, "backup-fixture.json")
                .writeBytes(ByteArray(MAX_BACKUP_BYTES + 1))
            try {
                backups.read(document)
                fail("Oversized backup accepted")
            } catch (_: IOException) {}
            File(context.cacheDir, "backup-fixture.json")
                .writeBytes(byteArrayOf(0xc3.toByte(), 0x28))
            try {
                backups.read(document)
                fail("Malformed UTF-8 accepted")
            } catch (_: IOException) {}
            try {
                backups.read(Uri.parse("file:///outside"))
                fail("File URI accepted")
            } catch (_: IOException) {}
            try {
                backups.read(Uri.parse("content://fr.deuspheara.oree.backupfixture/denied"))
                fail("Refused URI accepted")
            } catch (_: SecurityException) {}
            profiles.rename(original.activeId, "Newer", AvatarIds.DEFAULT)
            val newer = profiles.snapshots.first()!!
            try {
                backups.restore(BackupReview(BackupCandidate(original, 0), original))
                fail("Stale review overwrote changes")
            } catch (_: IOException) {}
            assertEquals(newer, profiles.snapshots.first())
        }
    }

    @Test
    fun unavailableWallpaperReferencesFallBackWithoutEmbeddingAssets() = runBlocking {
        isolated { profiles, backups, _ ->
            val id = profiles.snapshots.first()!!.activeId
            val missing = "wallpaper-${java.util.UUID.randomUUID()}.jpg"
            profiles.setImage(id, missing)
            profiles.setAppPreviewsEnabled(id, true)
            backups.export(document)
            val imported = backups.read(document)
            assertEquals(1, imported.missingImages)
            assertNull(imported.snapshot.active!!.image)
            assertEquals(WallpaperMode.LANDSCAPE, imported.snapshot.active!!.wallpaper)
            assertTrue(imported.snapshot.active!!.appPreviewsEnabled)
            assertEquals(missing, profiles.snapshots.first()!!.active!!.image)
        }
    }
}
