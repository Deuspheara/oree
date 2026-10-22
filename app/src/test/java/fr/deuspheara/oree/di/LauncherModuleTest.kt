package fr.deuspheara.oree.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.repository.AppRepository
import fr.deuspheara.oree.core.domain.repository.BackupRepository
import fr.deuspheara.oree.core.domain.repository.CapabilityRepository
import fr.deuspheara.oree.core.domain.repository.ContentRepository
import fr.deuspheara.oree.core.domain.repository.InputRepository
import fr.deuspheara.oree.core.domain.repository.LegacyOrganizationRepository
import fr.deuspheara.oree.core.domain.repository.LegacyPreferencesRepository
import fr.deuspheara.oree.core.domain.repository.OrganizationRepository
import fr.deuspheara.oree.core.domain.repository.PhotoRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository
import fr.deuspheara.oree.core.domain.source.ContentSelection
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.BackupCandidate
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.ContentCapabilities
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import fr.deuspheara.oree.core.model.ProbeAvailability
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeViewModel
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfilesViewModel
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchViewModel
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.SettingsViewModel
import fr.deuspheara.oree.presentation.LauncherViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify

class LauncherModuleTest {
    @Test
    fun declaredGraphHasNoMissingConstructorDependencies() {
        // Android context and concrete dispatcher values are supplied by the application factory.
        launcherModule.verify(extraTypes = listOf(Context::class, CoroutineDispatcher::class))
    }

    @Test
    fun everyViewModelIsConstructedWithPortableTestDependencies() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val profiles = MemoryProfileFixture()
        val fixtures = module {
            single { AppDispatchers(dispatcher, dispatcher) }
            single<CoroutineScope> { backgroundScope }
            single<ProfileRepository> { profiles }
            single<OrganizationRepository> { profiles }
            single<LegacyPreferencesRepository> {
                object : LegacyPreferencesRepository {
                    override val preferences = flowOf(LauncherPreferences())
                }
            }
            single<LegacyOrganizationRepository> {
                object : LegacyOrganizationRepository {
                    override val organization = flowOf(AppOrganization())
                }
            }
            single<AppRepository> {
                object : AppRepository {
                    override val changes = flowOf(Unit)

                    override suspend fun load() = emptyList<TvApp>()
                }
            }
            single<PhotoRepository> {
                object : PhotoRepository {
                    override suspend fun load() = LocalPhotos(PhotoAvailability.EMPTY)
                }
            }
            single<InputRepository> {
                object : InputRepository {
                    override val inputs = flowOf(TvInputs())

                    override suspend fun contains(id: String) = false
                }
            }
            single<CapabilityRepository> {
                object : CapabilityRepository {
                    override suspend fun inspect() = ContentCapabilities(
                        emptyList(),
                        false,
                        false,
                        0,
                        0,
                        ProbeAvailability.UNSUPPORTED,
                    )
                }
            }
            single<ContentRepository> {
                object : ContentRepository {
                    override fun refresh() = Unit
                    override fun observe(watchNextFeed: Boolean, appPreviews: Boolean, wallpaper: Boolean, packageName: Flow<String?>) =
                        flowOf(ContentSelection())
                }
            }
            single<WallpaperRepository> {
                object : WallpaperRepository {
                    override suspend fun importImage(uri: String) = error("No image import in construction fixture")

                    override suspend fun discard(name: String) = Unit
                }
            }
            single<BackupRepository> {
                object : BackupRepository {
                    override suspend fun export(uri: String) = Unit

                    override suspend fun read(uri: String) = BackupCandidate(profiles.snapshots.value!!, 0)

                    override suspend fun previous() = BackupCandidate(profiles.snapshots.value!!, 0)

                    override suspend fun restore(review: BackupReview) = Unit
                }
            }
        }
        val graph = koinApplication {
            allowOverride(true)
            modules(launcherModule, fixtures)
        }
        val owner = ViewModelStore()
        try {
            val models =
                listOf<ViewModel>(
                    graph.koin.get<HomeViewModel>(),
                    graph.koin.get<SettingsViewModel>(),
                    graph.koin.get<ProfilesViewModel>(),
                    graph.koin.get<SearchViewModel>(),
                    graph.koin.get<LauncherViewModel>(),
                )
            models.forEachIndexed { index, model -> owner.put("fixture-$index", model) }
            runCurrent()
            assertEquals(5, models.size)
            assertFalse((models.last() as LauncherViewModel).initializationFailed.value)
        } finally {
            owner.clear()
            graph.close()
            Dispatchers.resetMain()
        }
    }
}
