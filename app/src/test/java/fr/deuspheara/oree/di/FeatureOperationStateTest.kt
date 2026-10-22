package fr.deuspheara.oree.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository
import fr.deuspheara.oree.core.domain.usecase.DeleteProfileUseCase
import fr.deuspheara.oree.core.domain.usecase.RestoreBackupUseCase
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeViewModel
import fr.deuspheara.oree.feature.home.presentation.viewmodel.OrganizationWriteStatus
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfileEditStatus
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfilesViewModel
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.BackupStatus
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.RestoreStatus
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.SettingsViewModel
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FeatureOperationStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun <T : ViewModel> retain(model: T): T = model.also { models.put(model.javaClass.name, it) }

    private val catalog = object : AppRepository {
        override val changes = emptyFlow<Unit>()
        override suspend fun load() = emptyList<TvApp>()
    }
    private val images = object : WallpaperRepository {
        override suspend fun importImage(uri: String): String = error("Unused")
        override suspend fun discard(name: String) = Unit
    }

    @Test fun organizationCommitsSurviveCancelledWaitersAndRemainSerialized() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val writes = mutableListOf<String>()
        val organization = object : OrganizationRepository by MemoryProfileFixture() {
            override suspend fun setFavorite(id: String, component: String, favorite: Boolean) {
                writes += "start:$id"
                if (id == "first") gate.await()
                writes += "commit:$id"
            }
        }
        val model = retain(HomeViewModel(catalog, organization))
        val route = launch { model.setFavorite("first", "app", true) }
        runCurrent()
        route.cancel()
        launch { model.setFavorite("second", "app", false) }
        runCurrent()
        assertEquals(listOf("start:first"), writes)
        assertEquals("first", model.writing.value?.profileId)
        model.acknowledgeWrite()
        assertEquals(OrganizationWriteStatus.SAVING, model.writing.value?.status)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("start:first", "commit:first", "start:second", "commit:second"), writes)
        assertEquals(OrganizationWriteStatus.COMPLETED, model.writing.value?.status)
    }

    @Test fun failedOrganizationWriteIsRetainedAndRetryCanComplete() = runTest(dispatcher) {
        var fail = true
        val organization = object : OrganizationRepository by MemoryProfileFixture() {
            override suspend fun setFavorite(id: String, component: String, favorite: Boolean) {
                if (fail) throw IOException("fixture")
            }
        }
        val model = retain(HomeViewModel(catalog, organization))
        try {
            model.setFavorite("captured", "app", true)
            error("Expected persistence failure")
        } catch (_: IOException) {
            assertEquals(OrganizationWriteStatus.FAILED, model.writing.value?.status)
        }
        fail = false
        model.setFavorite("captured", "app", true)
        assertEquals(OrganizationWriteStatus.COMPLETED, model.writing.value?.status)
        model.acknowledgeWrite()
        assertNull(model.writing.value)
    }

    @Test fun profileEditsCaptureIdentityRejectDuplicatesAndRecoverAfterFailure() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val selected = mutableListOf<String>()
        var fail = true
        val repository = object : ProfileRepository by MemoryProfileFixture() {
            override suspend fun select(id: String) {
                selected += id
                gate.await()
                if (fail) throw SecurityException("fixture")
            }
        }
        val model = retain(ProfilesViewModel(repository, DeleteProfileUseCase(repository, images)))
        model.select("captured")
        model.select("duplicate")
        model.acknowledge()
        runCurrent()
        assertEquals(ProfileEditStatus.SAVING, model.state.value.status)
        assertEquals(listOf("captured"), selected)
        gate.complete(Unit)
        runCurrent()
        assertEquals(ProfileEditStatus.FAILED, model.state.value.status)
        fail = false
        model.select("captured")
        runCurrent()
        assertEquals(ProfileEditStatus.COMPLETED, model.state.value.status)
        model.acknowledge()
        assertEquals(ProfileEditStatus.IDLE, model.state.value.status)
    }

    @Test fun restoreWaitsForAcceptedPreferencesBlocksEditsAndRetainsFailureForRetry() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val committed = mutableListOf<String>()
        val repository = object : ProfileRepository by MemoryProfileFixture() {
            override suspend fun setPreferences(id: String, value: LauncherPreferences) {
                gate.await()
                committed += id
            }
        }
        var restores = 0
        var fail = true
        val backups = object : BackupRepository {
            override suspend fun export(uri: String) = error("Unused")
            override suspend fun read(uri: String): BackupCandidate = error("Unused")
            override suspend fun previous(): BackupCandidate = error("Unused")
            override suspend fun restore(review: BackupReview) {
                assertEquals(listOf("captured"), committed)
                restores++
                if (fail) throw IOException("fixture")
            }
        }
        val model = retain(SettingsViewModel(repository, backups, RestoreBackupUseCase(backups)))
        val snapshot = repository.readSnapshot()!!
        val review = BackupReview(BackupCandidate(snapshot, 0), snapshot)
        model.setPreferences("captured", LauncherPreferences())
        model.restore(review)
        model.restore(review)
        model.setPreferences("blocked", LauncherPreferences())
        model.clearRestore()
        runCurrent()
        assertEquals(RestoreStatus.SAVING, model.restoring.value?.status)
        assertEquals(0, restores)
        gate.complete(Unit)
        runCurrent()
        assertEquals(RestoreStatus.FAILED, model.restoring.value?.status)
        assertEquals(1, restores)
        fail = false
        model.restore(review)
        runCurrent()
        assertEquals(RestoreStatus.COMPLETED, model.restoring.value?.status)
        assertEquals(emptyMap<Any, Any>(), model.preferenceStates.value)
        model.clearRestore()
        assertNull(model.restoring.value)
    }

    @Test fun backupReviewReadsCommittedPreferencesAndExportIsRetainedWithoutARoute() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val observation = MemoryProfileFixture()
        val original = observation.readSnapshot()!!
        var committed = original
        val changedPreferences = LauncherPreferences(reduceMotion = true)
        val repository = object : ProfileRepository by observation {
            override suspend fun setPreferences(id: String, value: LauncherPreferences) {
                gate.await()
                committed = original.copy(profiles = original.profiles.map { it.copy(preferences = value) })
            }
            override suspend fun readSnapshot() = committed
        }
        var exports = 0
        val candidate = BackupCandidate(original, 0)
        val backups = object : BackupRepository {
            override suspend fun export(uri: String) {
                assertEquals(changedPreferences, committed.active!!.preferences)
                exports++
            }
            override suspend fun read(uri: String) = candidate
            override suspend fun previous() = candidate
            override suspend fun restore(review: BackupReview) = error("Unused")
        }
        val model = retain(SettingsViewModel(repository, backups, RestoreBackupUseCase(backups)))
        model.setPreferences("fixture", changedPreferences)
        model.review("content://fixture/backup")
        model.export("content://fixture/duplicate")
        runCurrent()
        assertEquals(true, model.backup.value.busy)
        assertEquals(0, exports)
        gate.complete(Unit)
        runCurrent()
        assertEquals(committed, model.backup.value.review!!.expected)
        assertEquals(original, observation.snapshots.value)
        model.export("content://fixture/export")
        runCurrent()
        assertEquals(1, exports)
        assertEquals(BackupStatus.EXPORTED, model.backup.value.status)
        model.acknowledgeBackupResult()
        assertEquals(BackupStatus.IDLE, model.backup.value.status)
    }

    @Test fun obsoleteCatalogReadsCancelWithoutErasingCachedContentAndRetryRecovers() = runTest(dispatcher) {
        val invalidations = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val initial = TvApp("first", "package", "First", 0)
        val replacement = TvApp("second", "package", "Second", 0)
        var loads = 0
        var cancelled = false
        val repository = object : AppRepository {
            override val changes = invalidations.onStart { emit(Unit) }
            override suspend fun load(): List<TvApp> = when (++loads) {
                1 -> listOf(initial)

                2 -> try {
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    cancelled = true
                }

                3 -> throw IOException("fixture")

                else -> listOf(replacement)
            }
        }
        val model = retain(HomeViewModel(repository, MemoryProfileFixture()))
        val route = backgroundScope.launch { model.state.collect {} }
        runCurrent()
        assertEquals(listOf(initial), model.state.value.apps)
        invalidations.emit(Unit)
        runCurrent()
        invalidations.emit(Unit)
        runCurrent()
        assertEquals(true, cancelled)
        assertEquals(true, model.state.value.failed)
        assertEquals(listOf(initial), model.state.value.apps)
        model.reload()
        runCurrent()
        assertEquals(false, model.state.value.failed)
        assertEquals(listOf(replacement), model.state.value.apps)
        route.cancel()
    }
}
