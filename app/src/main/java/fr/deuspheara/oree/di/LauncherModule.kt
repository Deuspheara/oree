package fr.deuspheara.oree.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.memory.MemoryCache
import fr.deuspheara.oree.core.data.datastore.profileDataStore
import fr.deuspheara.oree.core.data.migration.DataStoreLegacyOrganization
import fr.deuspheara.oree.core.data.migration.DataStoreLegacyPreferences
import fr.deuspheara.oree.core.data.repository.DataStoreProfileRepository
import fr.deuspheara.oree.core.data.repository.DefaultAppRepository
import fr.deuspheara.oree.core.data.repository.DefaultCapabilityRepository
import fr.deuspheara.oree.core.data.repository.DefaultContentRepository
import fr.deuspheara.oree.core.data.repository.DefaultInputRepository
import fr.deuspheara.oree.core.data.repository.DefaultPhotoRepository
import fr.deuspheara.oree.core.data.repository.SafBackupRepository
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.concurrency.Clock
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
import fr.deuspheara.oree.core.domain.usecase.DeleteProfileUseCase
import fr.deuspheara.oree.core.domain.usecase.ImportWallpaperUseCase
import fr.deuspheara.oree.core.domain.usecase.InitializeLauncherUseCase
import fr.deuspheara.oree.core.domain.usecase.RestoreBackupUseCase
import fr.deuspheara.oree.core.platform.datasource.apps.AndroidAppCatalogSource
import fr.deuspheara.oree.core.platform.datasource.content.AndroidContentCapabilitySource
import fr.deuspheara.oree.core.platform.datasource.content.PreviewProgramsContentSource
import fr.deuspheara.oree.core.platform.datasource.content.WatchNextContentSource
import fr.deuspheara.oree.core.platform.datasource.input.AndroidTvInputSource
import fr.deuspheara.oree.core.platform.datasource.photos.MediaStorePhotoSource
import fr.deuspheara.oree.core.platform.image.WallpaperRenderer
import fr.deuspheara.oree.core.platform.image.impl.AndroidWallpaperStore
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeViewModel
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfilesViewModel
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchViewModel
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.SettingsViewModel
import fr.deuspheara.oree.presentation.LauncherViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** Only this composition root knows persistence implementations. Nothing is created eagerly. */
val launcherModule = module {
    single { AppDispatchers(Dispatchers.IO, Dispatchers.Default) }
    single<Clock> { Clock(System::currentTimeMillis) }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    single<AppRepository> {
        DefaultAppRepository(AndroidAppCatalogSource(androidContext(), get()), get())
    }
    single<DataStore<Preferences>> { profileDataStore(androidContext()) }
    single { DataStoreProfileRepository(get<DataStore<Preferences>>(), get<CoroutineScope>(), get()) }
    single<ProfileRepository> { get<DataStoreProfileRepository>() }
    single<OrganizationRepository> { get<DataStoreProfileRepository>() }
    single<LegacyPreferencesRepository> { DataStoreLegacyPreferences(androidContext()) }
    single<LegacyOrganizationRepository> { DataStoreLegacyOrganization(androidContext()) }
    single<BackupRepository> { SafBackupRepository(androidContext(), get(), get()) }
    single { AndroidWallpaperStore(androidContext(), get()) }
    single<WallpaperRepository> { get<AndroidWallpaperStore>() }
    single<WallpaperRenderer> { get<AndroidWallpaperStore>() }
    single<PhotoRepository> {
        DefaultPhotoRepository(MediaStorePhotoSource(androidContext(), get()))
    }
    single<InputRepository> {
        DefaultInputRepository(AndroidTvInputSource(androidContext(), get()))
    }
    single<CapabilityRepository> {
        DefaultCapabilityRepository(
            AndroidContentCapabilitySource(androidContext(), dispatchers = get()),
        )
    }
    single<ContentRepository> {
        DefaultContentRepository(
            WatchNextContentSource(androidContext(), dispatchers = get(), clock = get()),
            PreviewProgramsContentSource(androidContext(), dispatchers = get()),
            get(),
        )
    }
    single { ArtworkReflections(get()) }
    single {
        ImageLoader.Builder(androidContext())
            .memoryCache { MemoryCache.Builder().maxSizeBytes(12L * 1024 * 1024).build() }
            .components { add(get<ArtworkReflections>()) }
            .diskCache(null)
            .build()
    }
    factory { InitializeLauncherUseCase(get(), get(), get()) }
    factory { ImportWallpaperUseCase(get(), get()) }
    factory { DeleteProfileUseCase(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { ProfilesViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    factory { RestoreBackupUseCase(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel { LauncherViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
