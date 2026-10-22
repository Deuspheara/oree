package fr.deuspheara.oree.di

import androidx.lifecycle.ViewModelStore
import fr.deuspheara.oree.core.domain.repository.CapabilityRepository
import fr.deuspheara.oree.core.domain.repository.ContentRepository
import fr.deuspheara.oree.core.domain.repository.InputRepository
import fr.deuspheara.oree.core.domain.repository.LegacyOrganizationRepository
import fr.deuspheara.oree.core.domain.repository.LegacyPreferencesRepository
import fr.deuspheara.oree.core.domain.repository.PhotoRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository
import fr.deuspheara.oree.core.domain.source.ContentSelection
import fr.deuspheara.oree.core.domain.usecase.ImportWallpaperUseCase
import fr.deuspheara.oree.core.domain.usecase.InitializeLauncherUseCase
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.ContentCapabilities
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.presentation.LauncherViewModel
import fr.deuspheara.oree.presentation.LauncherWriteStatus
import fr.deuspheara.oree.presentation.WallpaperImportStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStateTest {
    private fun model(profiles: ProfileRepository, images: WallpaperRepository): LauncherViewModel = LauncherViewModel(
        profiles,
        object : PhotoRepository {
            override suspend fun load() = LocalPhotos(PhotoAvailability.EMPTY)
        },
        object : InputRepository {
            override val inputs = flowOf(TvInputs())
            override suspend fun contains(id: String) = false
        },
        object : CapabilityRepository {
            override suspend fun inspect(): ContentCapabilities = error("Unused fixture operation")
        },
        object : ContentRepository {
            override fun refresh() = Unit
            override fun observe(watchNextFeed: Boolean, appPreviews: Boolean, wallpaper: Boolean, packageName: Flow<String?>) =
                flowOf(ContentSelection())
        },
        InitializeLauncherUseCase(
            profiles,
            object : LegacyPreferencesRepository {
                override val preferences = flowOf(LauncherPreferences())
            },
            object : LegacyOrganizationRepository {
                override val organization = flowOf(AppOrganization())
            },
        ),
        ImportWallpaperUseCase(profiles, images),
    )

    @Test
    fun importProgressSurvivesObserverRecreationAndPreventsDuplicateAcceptedImports() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val gate = CompletableDeferred<Unit>()
            val committed = mutableListOf<Pair<String, String>>()
            var imports = 0
            val profiles = object : ProfileRepository by MemoryProfileFixture() {
                override suspend fun setImage(id: String, name: String) {
                    committed += id to name
                }
            }
            val viewModel = model(
                profiles,
                object : WallpaperRepository {
                    override suspend fun importImage(uri: String): String {
                        imports++
                        gate.await()
                        return "wallpaper-fixture.jpg"
                    }
                    override suspend fun discard(name: String) = Unit
                },
            )
            val firstRoute = backgroundScope.launch { viewModel.importing.collect() }
            viewModel.importPhoto("fixture", "content://fixture/first")
            runCurrent()
            firstRoute.cancel()
            viewModel.importPhoto("another", "content://fixture/second")
            val recreatedRoute = backgroundScope.launch { viewModel.importing.collect() }
            runCurrent()
            assertTrue((viewModel.importing.value!!.status == WallpaperImportStatus.SAVING))
            assertEquals(1, imports)
            gate.complete(Unit)
            runCurrent()
            assertEquals(listOf("fixture" to "wallpaper-fixture.jpg"), committed)
            assertTrue((viewModel.importing.value!!.status == WallpaperImportStatus.COMPLETED))
            viewModel.clearImportResult()
            assertEquals(null, viewModel.importing.value)
            recreatedRoute.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unreadableProfilesReportInitializationFailureWithoutApplyingLegacyDefaults() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val profiles = MemoryProfileFixture()
            profiles.snapshots.value = ProfileSnapshot("", emptyList(), failed = true)
            val viewModel = model(
                profiles,
                object : WallpaperRepository {
                    override suspend fun importImage(uri: String): String = error("Unused fixture operation")
                    override suspend fun discard(name: String) = error("Unused fixture operation")
                },
            )
            runCurrent()
            assertTrue(viewModel.initializationFailed.value)
            assertTrue(profiles.snapshots.value!!.failed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun acceptedProfileWritesKeepCapturedIdentityAndFailureAfterRouteCancellation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = ViewModelStore()
        try {
            val gate = CompletableDeferred<Unit>()
            val writes = mutableListOf<String>()
            val profiles = object : ProfileRepository by MemoryProfileFixture() {
                override suspend fun setAmbientClock(id: String, enabled: Boolean) {
                    gate.await()
                    writes += id
                    throw java.io.IOException("fixture")
                }
            }
            val viewModel = model(
                profiles,
                object : WallpaperRepository {
                    override suspend fun importImage(uri: String): String = error("Unused")
                    override suspend fun discard(name: String) = Unit
                },
            )
            owner.put("launcher", viewModel)
            val route = launch { viewModel.setAmbientClock("captured", false) }
            runCurrent()
            route.cancel()
            assertEquals(LauncherWriteStatus.SAVING, viewModel.writing.value?.status)
            gate.complete(Unit)
            runCurrent()
            assertEquals(listOf("captured"), writes)
            assertEquals("captured", viewModel.writing.value?.profileId)
            assertEquals(LauncherWriteStatus.FAILED, viewModel.writing.value?.status)
        } finally {
            owner.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repeatedInitializationRetriesShareOneAcceptedRead() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val owner = ViewModelStore()
        try {
            val gate = CompletableDeferred<Unit>()
            var reads = 0
            val fixture = MemoryProfileFixture()
            val profiles = object : ProfileRepository by fixture {
                override suspend fun readSnapshot(): ProfileSnapshot? {
                    reads++
                    gate.await()
                    return fixture.readSnapshot()
                }
            }
            val viewModel = model(
                profiles,
                object : WallpaperRepository {
                    override suspend fun importImage(uri: String): String = error("Unused")
                    override suspend fun discard(name: String) = Unit
                },
            )
            owner.put("launcher", viewModel)
            repeat(10) { viewModel.retryInitialization() }
            runCurrent()
            assertEquals(1, reads)
            gate.complete(Unit)
            runCurrent()
            assertEquals(false, viewModel.initializationFailed.value)
        } finally {
            owner.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
