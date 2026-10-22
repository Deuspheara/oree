@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.presentation

import android.widget.Toast
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil3.SingletonImageLoader
import fr.deuspheara.oree.BuildConfig
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.LocalArtworkReflections
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassIntensity
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.glass.WallpaperBackground
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.domain.concurrency.PreferenceSaveStatus
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.ArtworkKind
import fr.deuspheara.oree.core.model.BackupReview
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.TvInputs
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.model.folderApps
import fr.deuspheara.oree.core.model.moveFavoriteApp
import fr.deuspheara.oree.core.model.orderFavoriteApps
import fr.deuspheara.oree.core.platform.datasource.content.resolveSystemSearch
import fr.deuspheara.oree.core.platform.image.WallpaperFrame
import fr.deuspheara.oree.core.platform.image.WallpaperRenderer
import fr.deuspheara.oree.core.platform.image.captureMenuBackdrop
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import fr.deuspheara.oree.core.platform.launch.AppUninstall
import fr.deuspheara.oree.core.platform.launch.SystemActionLauncher
import fr.deuspheara.oree.core.platform.launch.resolveAppInformation
import fr.deuspheara.oree.core.platform.launch.resolveContent
import fr.deuspheara.oree.core.platform.status.ControlStatus
import fr.deuspheara.oree.core.platform.status.controlStatus
import fr.deuspheara.oree.feature.home.presentation.component.AppContextMenu
import fr.deuspheara.oree.feature.home.presentation.component.FolderDestinationPanel
import fr.deuspheara.oree.feature.home.presentation.component.FolderPanel
import fr.deuspheara.oree.feature.home.presentation.component.FolderRenamePanel
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeViewModel
import fr.deuspheara.oree.feature.profiles.presentation.route.PhotoGalleryPanel
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import fr.deuspheara.oree.feature.profiles.presentation.screen.WallpaperPanel
import fr.deuspheara.oree.feature.profiles.presentation.viewmodel.ProfilesViewModel
import fr.deuspheara.oree.feature.search.domain.SearchAction
import fr.deuspheara.oree.feature.search.domain.SearchItem
import fr.deuspheara.oree.feature.search.presentation.route.SearchRoute
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchRequest
import fr.deuspheara.oree.feature.search.presentation.viewmodel.SearchViewModel
import fr.deuspheara.oree.feature.settings.presentation.component.BackupReviewPanel
import fr.deuspheara.oree.feature.settings.presentation.component.CommandPanel
import fr.deuspheara.oree.feature.settings.presentation.route.ContentCapabilitiesPanel
import fr.deuspheara.oree.feature.settings.presentation.screen.AmbientScreen
import fr.deuspheara.oree.feature.settings.presentation.screen.SettingsScreen
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsCategory
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.SettingsViewModel
import fr.deuspheara.oree.navigation.LauncherNavigation
import fr.deuspheara.oree.platform.contract.CreateLocalBackup
import fr.deuspheara.oree.platform.contract.OpenLocalBackup
import fr.deuspheara.oree.service.resolveDreamSettings
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppMenuTarget(
    val app: TvApp,
    val anchor: Rect,
    val awaitingRelease: Boolean,
    val dismissRequested: Boolean = false,
    val backdrop: WallpaperFrame? = null,
)

private data class FavoriteMove(val profileId: String, val appId: String, val original: List<String>, val draft: List<String>)

@Composable
fun LauncherRoot(
    launcher: LauncherViewModel,
    wallpaperRenderer: WallpaperRenderer,
    settingsViewModel: SettingsViewModel,
    systemActions: SystemActionLauncher,
    artworkReflections: ArtworkReflections,
    home: HomeViewModel,
    resume: Int,
    onOpen: (TvApp) -> Unit,
    onAndroidSettings: () -> Unit,
) {
    val state by home.state.collectAsStateWithLifecycle()
    val profiles by launcher.snapshots.collectAsStateWithLifecycle()
    val initializationFailed by launcher.initializationFailed.collectAsStateWithLifecycle()
    val profile = profiles?.active
    var navigation by rememberSaveable(stateSaver = LauncherNavigation.Saver) {
        mutableStateOf(LauncherNavigation())
    }
    LaunchedEffect(profile?.id) {
        val id = profile?.id ?: return@LaunchedEffect
        // Initial async loading after process death must not discard restored navigation.
        if (navigation.profileId == null) {
            navigation.profileId = id
        } else if (navigation.profileId != id) {
            navigation = LauncherNavigation(id)
        }
    }
    val stored = profile?.preferences
    val organization = profile?.let { AppOrganization(it.favorites) }
    var profilesOpen by navigation.profilesOpen
    var wallpaperOpen by navigation.wallpaperOpen
    var photoGalleryOpen by navigation.photoGalleryOpen
    var profileCommandSaving by remember { mutableStateOf(false) }
    var profileCommandFailed by remember { mutableStateOf(false) }
    val importProgress by launcher.importing.collectAsStateWithLifecycle()
    val profileSaving = profileCommandSaving || importProgress?.saving == true
    val profileFailed = profileCommandFailed || importProgress?.failed == true
    LaunchedEffect(importProgress) {
        if (importProgress?.completed == true) {
            if (importProgress?.profileId == profile?.id) photoGalleryOpen = false
            launcher.clearImportResult()
        }
    }

    var focusedApp by remember(profile?.id) { mutableStateOf<TvApp?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val selection by launcher.content.collectAsStateWithLifecycle()
    val content = selection.wallpaper
    val feed = selection.feed
    val visibleContent =
        remember(profile?.watchNextEnabled, profile?.appPreviewsEnabled, feed.entries, state.apps) {
            if (profile?.watchNextEnabled != true && profile?.appPreviewsEnabled != true) {
                emptyList()
            } else {
                val installed = state.apps.mapTo(hashSetOf()) { it.packageName }
                feed.entries.filter { it.source in installed }
            }
        }
    val dynamic =
        when (profile?.wallpaper) {
            WallpaperMode.APPLICATION ->
                focusedApp
                    ?.takeIf { it.artworkKind == ArtworkKind.BANNER && it.artwork != 0 }
                    ?.let {
                        android.net.Uri.parse("android.resource://${it.packageName}/${it.artwork}")
                    }

            WallpaperMode.CONTENT ->
                content.artwork.takeIf { profile.contentEnabled }?.let(android.net.Uri::parse)

            else -> null
        }
    val frame by
        key(profile?.id) {
            produceState<WallpaperFrame?>(
                null,
                profile?.wallpaper,
                profile?.landscape,
                profile?.image,
                dynamic,
            ) {
                profile?.let { value = wallpaperRenderer.load(it, dynamic) }
            }
        }
    var appMenu by remember { mutableStateOf<AppMenuTarget?>(null) }
    var menuCapturePending by remember { mutableStateOf(false) }
    var menuCaptureJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    androidx.activity.compose.BackHandler(menuCapturePending) {
        menuCaptureJob?.cancel()
        menuCapturePending = false
    }
    var organizationSaving by remember { mutableStateOf(false) }
    var organizationWriteFailed by remember { mutableStateOf(false) }
    var favoriteMove by remember(profile?.id) { mutableStateOf<FavoriteMove?>(null) }
    var openFolder by navigation.openFolder
    var renameFolder by navigation.renameFolder
    var folderDestination by remember(profile?.id) { mutableStateOf<TvApp?>(null) }
    var folderMoveApp by remember(profile?.id) { mutableStateOf<String?>(null) }
    var searchOpen by navigation.searchOpen
    var searchQuery by navigation.searchQuery
    var returnToSearch by navigation.returnToSearch
    var moveReturnToSearch by remember(profile?.id) { mutableStateOf(false) }
    var settingsCategory by navigation.settingsCategory
    var diagnosticsOpen by rememberSaveable { mutableStateOf(false) }
    var backupBusy by remember { mutableStateOf(false) }
    var backupFailed by remember { mutableStateOf(false) }
    var backupReview by remember { mutableStateOf<BackupReview?>(null) }
    var ambientOpen by remember(profile?.id) { mutableStateOf(false) }
    var photoPreview by remember(profile?.id) { mutableStateOf<LocalPhoto?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(profiles) {
        profiles
            ?.takeUnless { it.failed }
            ?.let { snapshot ->
                settingsViewModel.retainProfiles(snapshot.profiles.mapTo(hashSetOf()) { it.id })
            }
    }
    val restoreProgress by settingsViewModel.restoring.collectAsStateWithLifecycle()
    val preferenceStates by settingsViewModel.preferenceStates.collectAsStateWithLifecycle()
    val preferenceState = preferenceStates[profile?.id]
    val active = preferenceState?.value ?: stored ?: LauncherPreferences(reduceMotion = true)
    var settingsOpen by navigation.settingsOpen
    var customizeControls by remember { mutableStateOf(false) }
    fun savePreferences(value: LauncherPreferences) {
        if (settingsViewModel.restoring.value?.saving != true) {
            profile?.id?.let { settingsViewModel.setPreferences(it, value) }
        }
    }
    var commands by navigation.commands
    var controlsEditing by remember { mutableStateOf(false) }
    var restoreEpoch by remember { mutableIntStateOf(0) }
    val holder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val activity = androidx.activity.compose.LocalActivity.current
    LaunchedEffect(resume) { systemActions.resumed() }
    var importProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    fun importPhoto(uri: android.net.Uri, id: String?) {
        if (id != null && !profileSaving && settingsViewModel.restoring.value?.saving != true) {
            profileCommandFailed = false
            launcher.importPhoto(id, uri.toString())
        }
    }
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val id = importProfileId
            importProfileId = null
            if (uri != null) importPhoto(uri, id)
        }
    fun changeProfile(action: suspend () -> Unit) {
        if (!profileSaving && settingsViewModel.restoring.value?.saving != true) {
            scope.launch {
                profileCommandSaving = true
                profileCommandFailed = false
                try {
                    action()
                } catch (_: IOException) {
                    profileCommandFailed = true
                    Toast.makeText(context, R.string.preferences_write_error, Toast.LENGTH_LONG)
                        .show()
                } finally {
                    profileCommandSaving = false
                }
            }
        }
    }
    val uninstall = remember(context) { AppUninstall(context.applicationContext) }
    suspend fun awaitPreferenceWrites() {
        settingsViewModel.flushPreferences()
    }
    val exportBackup =
        rememberLauncherForActivityResult(CreateLocalBackup()) { uri ->
            if (uri != null) {
                scope.launch {
                    backupBusy = true
                    backupFailed = false
                    try {
                        awaitPreferenceWrites()
                        settingsViewModel.export(uri.toString())
                        Toast.makeText(context, R.string.backup_exported, Toast.LENGTH_LONG).show()
                    } catch (_: IOException) {
                        backupFailed = true
                    } catch (_: SecurityException) {
                        backupFailed = true
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val importBackup =
        rememberLauncherForActivityResult(OpenLocalBackup()) { uri ->
            if (uri != null) {
                scope.launch {
                    backupBusy = true
                    backupFailed = false
                    try {
                        val candidate = settingsViewModel.read(uri.toString())
                        awaitPreferenceWrites()
                        val expected =
                            launcher.readSnapshot() ?: throw IOException("Profiles unavailable")
                        backupReview = BackupReview(candidate, expected)
                    } catch (_: IOException) {
                        backupFailed = true
                    } catch (_: SecurityException) {
                        backupFailed = true
                    } finally {
                        backupBusy = false
                    }
                }
            }
        }
    val uninstallResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            home.reload()
            restoreEpoch++
            if (
                result.resultCode != android.app.Activity.RESULT_OK &&
                result.resultCode != android.app.Activity.RESULT_CANCELED
            ) {
                Toast.makeText(context, R.string.uninstall_error, Toast.LENGTH_LONG).show()
            }
        }
    var watchNextPermissionProfile by rememberSaveable { mutableStateOf<String?>(null) }
    var watchNextDenied by remember(profile?.id) { mutableStateOf(false) }
    val watchNextPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val id = watchNextPermissionProfile
            watchNextPermissionProfile = null
            watchNextDenied = !granted
            if (granted && id != null) changeProfile { launcher.setWatchNextEnabled(id, true) }
        }
    var previewPermissionProfile by rememberSaveable { mutableStateOf<String?>(null) }
    var previewDenied by remember(profile?.id) { mutableStateOf(false) }
    val previewPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val id = previewPermissionProfile
            previewPermissionProfile = null
            previewDenied = !granted
            if (granted && id != null) {
                changeProfile { launcher.setAppPreviewsEnabled(id, true) }
            } else {
                Toast.makeText(context, R.string.content_permission_denied, Toast.LENGTH_LONG)
                    .show()
            }
        }
    var contentPermissionProfile by rememberSaveable { mutableStateOf<String?>(null) }
    val contentPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val id = contentPermissionProfile
            contentPermissionProfile = null
            if (granted && id != null) {
                changeProfile { launcher.setContentEnabled(id, true) }
            } else {
                Toast.makeText(context, R.string.content_permission_denied, Toast.LENGTH_LONG)
                    .show()
            }
        }
    val systemReduced by
        produceState(true, resume) {
            value =
                withContext(Dispatchers.IO) {
                    android.provider.Settings.Global.getFloat(
                        context.contentResolver,
                        android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                        1f,
                    ) == 0f
                }
        }
    val loader = remember(context) { SingletonImageLoader.get(context) }
    var cacheBytes by remember { mutableLongStateOf(loader.memoryCache?.size ?: 0L) }
    fun closeSettings() {
        settingsOpen = false
        if (returnToSearch) {
            searchOpen = true
            returnToSearch = false
        }
        restoreEpoch++
    }
    LaunchedEffect(restoreProgress, profiles) {
        val progress = restoreProgress
        if (progress?.completed == true && profiles == progress.review.candidate.snapshot) {
            backupReview = null
            backupFailed = false
            searchOpen = false
            searchQuery = ""
            openFolder = null
            favoriteMove = null
            folderMoveApp = null
            settingsOpen = false
            returnToSearch = false
            restoreEpoch++
            settingsViewModel.clearRestore()
        }
    }
    LaunchedEffect(organization?.failed) {
        if (organization?.failed == true) {
            Toast.makeText(context, R.string.organization_read_error, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(state.apps) {
        if (appMenu != null && state.apps.none { it.id == appMenu?.app?.id }) {
            appMenu = null
            restoreEpoch++
        }
        if (favoriteMove?.let { move -> state.apps.none { it.id == move.appId } } == true) {
            favoriteMove = null
            restoreEpoch++
        }
    }
    // A stopped/recreated launcher cancels the transient preview; only explicit OK writes it.
    DisposableEffect(lifecycle, profile?.id) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    ambientOpen = false
                    photoPreview = null
                    if (!organizationSaving) {
                        favoriteMove = null
                        folderMoveApp = null
                    }
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(profile?.folders, state.apps) {
        if (openFolder != null && profile?.folders?.none { it.id == openFolder } == true) {
            openFolder = null
            restoreEpoch++
        }
        if (folderDestination?.let { target -> state.apps.none { it.id == target.id } } == true) {
            folderDestination = null
            restoreEpoch++
        }
    }
    fun showAppMenu(target: TvApp, anchor: Rect, held: Boolean) {
        if (
            appMenu != null ||
            commands ||
            settingsOpen ||
            menuCapturePending ||
            favoriteMove != null ||
            folderMoveApp != null
        ) {
            return
        }
        organizationWriteFailed = false
        menuCapturePending = true
        menuCaptureJob = scope.launch {
            try {
                val captured =
                    if (
                        !active.liquidGlass ||
                        active.reduceTransparency ||
                        active.highContrast ||
                        active.quality == QualityTier.ECONOMY
                    ) {
                        null
                    } else {
                        activity?.let { captureMenuBackdrop(it) }
                    }
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    appMenu = AppMenuTarget(target, anchor, held, backdrop = captured)
                }
            } finally {
                menuCapturePending = false
            }
        }
    }
    fun openContent(entry: ContentEntry) {
        scope.launch {
            val request = resolveContent(context, entry.intentUri, entry.source)
            if (request == null || systemActions.content(request) != true) {
                Toast.makeText(context, R.string.content_open_error, Toast.LENGTH_LONG).show()
            }
        }
    }
    ReportDrawnWhen { stored != null && organization != null && !state.loading }
    val glassSource = dev.chrisbanes.haze.rememberHazeState()
    val materialPreferences = active
    LauncherTheme(materialPreferences) {
        CompositionLocalProvider(
            LocalArtworkReflections provides artworkReflections,
            LocalOpaquePanels provides settingsOpen,
            LocalGlassSource provides glassSource,
            LocalLiquidGlass provides materialPreferences.liquidGlass,
            LocalGlassIntensity provides materialPreferences.glassIntensity,
            LocalReducedTransparency provides
                (
                    materialPreferences.reduceTransparency ||
                        materialPreferences.highContrast ||
                        materialPreferences.quality == QualityTier.ECONOMY
                    ),
            LocalHighContrast provides materialPreferences.highContrast,
        ) {
            Box(Modifier.fillMaxSize()) {
                if (!settingsOpen) {
                    WallpaperBackground(
                        remember(frame) { frame?.image?.asImageBitmap() },
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                    )
                }
                if (initializationFailed || profiles?.failed == true) {
                    Column(Modifier.align(Alignment.Center)) {
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.profile_load_error),
                            color = Color.White,
                        )
                        Button(onClick = { launcher.retryInitialization() }) {
                            Text(androidx.compose.ui.res.stringResource(R.string.retry))
                        }
                    }
                    return@Box
                }
                if (!settingsOpen) {
                    holder.SaveableStateProvider("home:${profile?.id}") {
                        HomeScreen(
                            appPreviewsEnabled = profile?.appPreviewsEnabled == true,
                            onSearch = { searchOpen = true },
                            folders = profile?.folders.orEmpty(),
                            onFolder = {
                                openFolder = it
                                profileCommandFailed = false
                            },
                            movingFavorite = favoriteMove?.appId,
                            moveSaving = organizationSaving,
                            moveFailed = organizationWriteFailed,
                            onMoveFavorite = { direction ->
                                favoriteMove = favoriteMove?.let { move ->
                                    move.copy(
                                        draft =
                                            moveFavoriteApp(
                                                move.draft,
                                                move.appId,
                                                direction,
                                                state.apps.mapTo(hashSetOf()) { it.id },
                                            ),
                                    )
                                }
                            },
                            onCancelMove = {
                                favoriteMove = null
                                organizationWriteFailed = false
                                if (moveReturnToSearch) {
                                    searchOpen = true
                                    moveReturnToSearch = false
                                }
                                restoreEpoch++
                            },
                            onCommitMove = {
                                favoriteMove?.let { move ->
                                    if (!organizationSaving) {
                                        organizationSaving = true
                                        organizationWriteFailed = false
                                        scope.launch {
                                            try {
                                                home.reorderFavorites(
                                                    move.profileId,
                                                    move.original,
                                                    move.draft,
                                                )
                                                favoriteMove = null
                                                if (moveReturnToSearch) {
                                                    searchOpen = true
                                                    moveReturnToSearch = false
                                                }
                                                restoreEpoch++
                                            } catch (_: IOException) {
                                                organizationWriteFailed = true
                                            } finally {
                                                organizationSaving = false
                                            }
                                        }
                                    }
                                }
                            },
                            retainDock =
                                frame != null &&
                                    profile?.wallpaper in
                                    listOf(WallpaperMode.LANDSCAPE, WallpaperMode.PERSONAL),
                            sourceKey = frame,
                            hideCards = controlsEditing || ambientOpen,
                            contentEntries = visibleContent,
                            onOpenContent = ::openContent,
                            state =
                                if (stored == null || organization == null) {
                                    state.copy(loading = true)
                                } else {
                                    state
                                },
                            resume = resume + restoreEpoch,
                            reduceMotion = active.reduceMotion,
                            onOpen = onOpen,
                            onSettings = {
                                settingsCategory = SettingsCategory.APPEARANCE
                                settingsOpen = true
                                cacheBytes = loader.memoryCache?.size ?: 0L
                            },
                            onReduceMotion = {
                                savePreferences(active.copy(reduceMotion = !active.reduceMotion))
                            },
                            onRetry = home::reload,
                            showContext = active.showContext,
                            economy = active.quality == QualityTier.ECONOMY,
                            onCommands = {
                                customizeControls = false
                                commands = true
                            },
                            systemReduceMotion = systemReduced,
                            favorites = favoriteMove?.draft ?: organization?.favorites.orEmpty(),
                            profile = profile,
                            onProfiles = {
                                profilesOpen = true
                                profileCommandFailed = false
                            },
                            onSelected = {
                                focusedApp = it
                                launcher.selectArtwork(it)
                            },
                            onAppMenu = ::showAppMenu,
                        )
                    }
                } else {
                    holder.SaveableStateProvider("settings:${profile?.id}") {
                        val tvInputs by
                            launcher.tvInputs.inputs.collectAsStateWithLifecycle(
                                initialValue = TvInputs(),
                            )
                        val canUndoRestore by
                            launcher.canUndoRestore.collectAsStateWithLifecycle(
                                initialValue = false,
                            )
                        val dreamSettings by
                            produceState<android.content.Intent?>(null, resume) {
                                value = resolveDreamSettings(context)
                            }
                        SettingsScreen(
                            versionName = BuildConfig.VERSION_NAME,
                            onAmbient = { ambientOpen = true },
                            ambientVisible = ambientOpen,
                            ambientPhotos = profile?.ambientPhotos == true,
                            ambientClock = profile?.ambientClock != false,
                            onAmbientPhotos = {
                                profile?.id?.let { id ->
                                    changeProfile {
                                        launcher.setAmbientPhotos(
                                            id,
                                            profile?.ambientPhotos != true,
                                        )
                                    }
                                }
                            },
                            onAmbientClock = {
                                profile?.id?.let { id ->
                                    changeProfile {
                                        launcher.setAmbientClock(id, profile?.ambientClock != true)
                                    }
                                }
                            },
                            onDreamSettings =
                                if (dreamSettings != null) {
                                    (
                                        {
                                            scope.launch {
                                                val request = resolveDreamSettings(context)
                                                if (
                                                    request == null ||
                                                    systemActions.content(request) != true
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.settings_error,
                                                        Toast.LENGTH_LONG,
                                                    )
                                                        .show()
                                                }
                                            }
                                        }
                                        )
                                } else {
                                    null
                                },
                            backupBusy = backupBusy || restoreProgress?.saving == true,
                            backupFailed = backupFailed,
                            onExport = {
                                try {
                                    exportBackup.launch("tv-launcher-backup.json")
                                } catch (_: android.content.ActivityNotFoundException) {
                                    backupFailed = true
                                } catch (_: SecurityException) {
                                    backupFailed = true
                                }
                            },
                            onImport = {
                                try {
                                    importBackup.launch(
                                        arrayOf(
                                            "application/json",
                                            "application/octet-stream",
                                            "text/plain",
                                        ),
                                    )
                                } catch (_: android.content.ActivityNotFoundException) {
                                    backupFailed = true
                                } catch (_: SecurityException) {
                                    backupFailed = true
                                }
                            },
                            onUndoRestore =
                                if (canUndoRestore) {
                                    (
                                        {
                                            scope.launch {
                                                backupBusy = true
                                                backupFailed = false
                                                try {
                                                    val candidate = settingsViewModel.previous()
                                                    awaitPreferenceWrites()
                                                    val expected =
                                                        launcher.readSnapshot()
                                                            ?: throw IOException("Profiles unavailable")
                                                    backupReview =
                                                        BackupReview(candidate, expected, true)
                                                } catch (_: IOException) {
                                                    backupFailed = true
                                                } finally {
                                                    backupBusy = false
                                                }
                                            }
                                        }
                                        )
                                } else {
                                    null
                                },
                            appPreviewsEnabled = profile?.appPreviewsEnabled == true,
                            appPreviewAvailability =
                                if (previewDenied) {
                                    ContentAvailability.DENIED
                                } else if (profile?.appPreviewsEnabled == true) {
                                    feed.sources[ContentSourceId.PREVIEW] ?: feed.availability
                                } else {
                                    ContentAvailability.DISABLED
                                },
                            onAppPreviews = {
                                profile?.let { p ->
                                    previewDenied = false
                                    if (
                                        p.appPreviewsEnabled ||
                                        context.checkSelfPermission(
                                            "android.permission.READ_TV_LISTINGS",
                                        ) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        changeProfile {
                                            launcher.setAppPreviewsEnabled(
                                                p.id,
                                                !p.appPreviewsEnabled,
                                            )
                                        }
                                    } else {
                                        previewPermissionProfile = p.id
                                        previewPermission.launch(
                                            "android.permission.READ_TV_LISTINGS",
                                        )
                                    }
                                }
                            },
                            onDiagnostics = { diagnosticsOpen = true },
                            initialCategory = settingsCategory,
                            onRetryContent = { launcher.retryContent() },
                            watchNextEnabled = profile?.watchNextEnabled == true,
                            contentAvailability =
                                if (watchNextDenied) {
                                    ContentAvailability.DENIED
                                } else if (profile?.watchNextEnabled == true) {
                                    feed.sources[ContentSourceId.WATCH_NEXT] ?: feed.availability
                                } else {
                                    ContentAvailability.DISABLED
                                },
                            onWatchNext = {
                                profile?.let { p ->
                                    watchNextDenied = false
                                    if (
                                        p.watchNextEnabled ||
                                        context.checkSelfPermission(
                                            "android.permission.READ_TV_LISTINGS",
                                        ) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        changeProfile {
                                            launcher.setWatchNextEnabled(p.id, !p.watchNextEnabled)
                                        }
                                    } else {
                                        watchNextPermissionProfile = p.id
                                        watchNextPermission.launch(
                                            "android.permission.READ_TV_LISTINGS",
                                        )
                                    }
                                }
                            },
                            inputs = tvInputs,
                            onInput = { input ->
                                scope.launch {
                                    if (
                                        !launcher.tvInputs.contains(input.id) ||
                                        systemActions.input(input) != true
                                    ) {
                                        Toast.makeText(
                                            context,
                                            R.string.input_open_error,
                                            Toast.LENGTH_LONG,
                                        )
                                            .show()
                                    }
                                }
                            },
                            value = active,
                            onChange = ::savePreferences,
                            onClose = ::closeSettings,
                            onRetrySave = { profile?.id?.let(settingsViewModel::retryPreferences) },
                            onCustomizeControls = {
                                customizeControls = true
                                commands = true
                            },
                            onAndroidSettings = onAndroidSettings,
                            cacheBytes = cacheBytes,
                            onClearCache = {
                                loader.memoryCache?.clear()
                                cacheBytes = loader.memoryCache?.size ?: 0L
                            },
                            writeFailed = preferenceState?.status == PreferenceSaveStatus.FAILED,
                            reduceMotion =
                                active.reduceMotion ||
                                    systemReduced ||
                                    active.quality == QualityTier.ECONOMY,
                            onWallpaper = {
                                wallpaperOpen = true
                                profileCommandFailed = false
                            },
                            onProfiles = {
                                profilesOpen = true
                                profileCommandFailed = false
                            },
                        )
                    }
                }
                if (searchOpen) {
                    holder.SaveableStateProvider("search:${profile?.id}") {
                        val systemSearchAvailable by
                            produceState(false, resume) {
                                value = resolveSystemSearch(context) != null
                            }
                        val photos by
                            produceState<List<LocalPhoto>>(emptyList(), profile?.id, lifecycle) {
                                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    value = launcher.localPhotos.load().photos
                                    kotlinx.coroutines.awaitCancellation()
                                }
                            }
                        val actions =
                            listOf(
                                SearchItem.Action(
                                    SearchAction.APPEARANCE,
                                    androidx.compose.ui.res.stringResource(R.string.appearance),
                                ),
                                SearchItem.Action(
                                    SearchAction.HOME,
                                    androidx.compose.ui.res.stringResource(R.string.home_settings),
                                ),
                                SearchItem.Action(
                                    SearchAction.NAVIGATION,
                                    androidx.compose.ui.res.stringResource(R.string.navigation),
                                ),
                                SearchItem.Action(
                                    SearchAction.ACCESSIBILITY,
                                    androidx.compose.ui.res.stringResource(R.string.accessibility),
                                ),
                                SearchItem.Action(
                                    SearchAction.PRIVACY,
                                    androidx.compose.ui.res.stringResource(R.string.privacy),
                                ),
                                SearchItem.Action(
                                    SearchAction.STORAGE,
                                    androidx.compose.ui.res.stringResource(R.string.storage),
                                ),
                                SearchItem.Action(
                                    SearchAction.ABOUT,
                                    androidx.compose.ui.res.stringResource(R.string.about),
                                ),
                                SearchItem.Action(
                                    SearchAction.PROFILES,
                                    androidx.compose.ui.res.stringResource(R.string.profiles),
                                ),
                                SearchItem.Action(
                                    SearchAction.WALLPAPER,
                                    androidx.compose.ui.res.stringResource(R.string.wallpaper),
                                ),
                                SearchItem.Action(
                                    SearchAction.ANDROID_SETTINGS,
                                    androidx.compose.ui.res.stringResource(
                                        R.string.system_settings,
                                    ),
                                ),
                            )
                        val searchModel: SearchViewModel =
                            org.koin.compose.viewmodel.koinViewModel(key = "search:${profile?.id}")
                        SearchRoute(
                            searchModel,
                            SearchRequest(
                                searchQuery,
                                state.apps,
                                profile?.folders.orEmpty(),
                                actions,
                                visibleContent,
                                profile?.favorites.orEmpty(),
                                photos,
                            ),
                            { searchQuery = it },
                            restoreEpoch + resume,
                            active.reduceMotion || systemReduced || active.quality == QualityTier.ECONOMY,
                            onSelect = { item ->
                                when (item) {
                                    is SearchItem.App -> onOpen(item.app)

                                    is SearchItem.Folder -> {
                                        openFolder = item.folder.id
                                        profileCommandFailed = false
                                    }

                                    is SearchItem.Content -> openContent(item.entry)

                                    is SearchItem.Photo -> {
                                        val uri = android.net.Uri.parse(item.photo.uri)
                                        if (
                                            uri.scheme == "content" &&
                                            uri.authority == "media" &&
                                            uri.pathSegments.take(3) ==
                                            listOf("external", "images", "media") &&
                                            uri.lastPathSegment?.toLongOrNull()?.let {
                                                it > 0
                                            } == true
                                        ) {
                                            photoPreview = item.photo
                                        }
                                    }

                                    is SearchItem.Action ->
                                        when (item.action) {
                                            SearchAction.PROFILES -> {
                                                profilesOpen = true
                                                profileCommandFailed = false
                                            }

                                            SearchAction.WALLPAPER -> {
                                                wallpaperOpen = true
                                                profileCommandFailed = false
                                            }

                                            SearchAction.ANDROID_SETTINGS -> onAndroidSettings()

                                            else -> {
                                                settingsCategory =
                                                    SettingsCategory.valueOf(item.action.name)
                                                returnToSearch = true
                                                searchOpen = false
                                                settingsOpen = true
                                                cacheBytes = loader.memoryCache?.size ?: 0L
                                            }
                                        }
                                }
                            },
                            onAppMenu = ::showAppMenu,
                            onClose = {
                                searchOpen = false
                                restoreEpoch++
                            },
                            onSystemSearch =
                                if (systemSearchAvailable) {
                                    (
                                        { query ->
                                            scope.launch {
                                                val request = resolveSystemSearch(context, query)
                                                if (
                                                    request == null ||
                                                    systemActions.content(request) != true
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.settings_error,
                                                        Toast.LENGTH_LONG,
                                                    )
                                                        .show()
                                                }
                                            }
                                        }
                                        )
                                } else {
                                    null
                                },
                        )
                    }
                }
                profile
                    ?.folders
                    ?.firstOrNull { it.id == openFolder }
                    ?.let { folder ->
                        FolderPanel(
                            folder,
                            state.apps,
                            restoreEpoch,
                            active.reduceMotion ||
                                systemReduced ||
                                active.quality == QualityTier.ECONOMY,
                            profileSaving,
                            profileFailed,
                            folderMoveApp,
                            onOpen,
                            ::showAppMenu,
                            onRename = {
                                renameFolder = folder.id
                                profileCommandFailed = false
                            },
                            onDelete = {
                                profile?.id?.let { id ->
                                    changeProfile {
                                        home.deleteFolder(id, folder.id)
                                        openFolder = null
                                        restoreEpoch++
                                    }
                                }
                            },
                            onClose = {
                                openFolder = null
                                folderMoveApp = null
                                restoreEpoch++
                            },
                            onCancelMove = {
                                folderMoveApp = null
                                restoreEpoch++
                            },
                            onReorder = { expected, order ->
                                profile?.id?.let { id ->
                                    changeProfile {
                                        home.reorderFolder(id, folder.id, expected, order)
                                        folderMoveApp = null
                                        restoreEpoch++
                                    }
                                }
                            },
                        )
                    }
                folderDestination?.let { target ->
                    profile?.let { p ->
                        FolderDestinationPanel(
                            p.folders,
                            p.folders.firstOrNull { target.id in it.apps }?.id,
                            profileSaving,
                            profileFailed,
                            onMove = { id ->
                                changeProfile {
                                    home.moveToFolder(p.id, target.id, id)
                                    folderDestination = null
                                    restoreEpoch++
                                }
                            },
                            onCreate = { name ->
                                changeProfile {
                                    home.createFolder(p.id, name, target.id)
                                    folderDestination = null
                                    restoreEpoch++
                                }
                            },
                            onClose = {
                                folderDestination = null
                                profileCommandFailed = false
                                restoreEpoch++
                            },
                        )
                    }
                }
                profile
                    ?.folders
                    ?.firstOrNull { it.id == renameFolder }
                    ?.let { folder ->
                        FolderRenamePanel(
                            folder,
                            profileSaving,
                            profileFailed,
                            onSave = { name ->
                                profile?.id?.let { id ->
                                    changeProfile {
                                        home.renameFolder(id, folder.id, name)
                                        renameFolder = null
                                        restoreEpoch++
                                    }
                                }
                            },
                            onClose = {
                                renameFolder = null
                                profileCommandFailed = false
                                restoreEpoch++
                            },
                        )
                    }
                appMenu?.let { target ->
                    val eligible by
                        produceState(false, target.app.packageName, resume) {
                            value = uninstall.intent(target.app.packageName) != null
                        }
                    val appInfoAvailable by
                        produceState(false, target.app.id, resume) {
                            value = resolveAppInformation(context, target.app) != null
                        }
                    val memberFolder = profile?.folders?.firstOrNull { target.app.id in it.apps }
                    val reorder: (() -> Unit)? =
                        when {
                            memberFolder != null && folderApps(memberFolder, state.apps).size > 1 ->
                                (
                                    {
                                        appMenu = null
                                        openFolder = memberFolder.id
                                        folderMoveApp = target.app.id
                                        restoreEpoch++
                                    }
                                    )

                            target.app.id in organization?.favorites.orEmpty() &&
                                orderFavoriteApps(state.apps, organization?.favorites.orEmpty())
                                    .size > 1 -> (
                                {
                                    profile?.let { p ->
                                        favoriteMove =
                                            FavoriteMove(
                                                p.id,
                                                target.app.id,
                                                p.favorites,
                                                p.favorites,
                                            )
                                    }
                                    if (searchOpen) {
                                        moveReturnToSearch = true
                                        searchOpen = false
                                    }
                                    appMenu = null
                                    organizationWriteFailed = false
                                    restoreEpoch++
                                }
                                )

                            else -> null
                        }
                    AppContextMenu(
                        onReorder = reorder,
                        reorderLabel =
                            androidx.compose.ui.res.stringResource(
                                if (memberFolder != null) {
                                    R.string.move_in_folder
                                } else {
                                    R.string.move_favorite
                                },
                            ),
                        onMoveToFolder = {
                            appMenu = null
                            folderDestination = target.app
                            profileCommandFailed = false
                        },
                        onMoveHome =
                            if (memberFolder != null) {
                                (
                                    {
                                        appMenu = null
                                        profile?.id?.let { id ->
                                            changeProfile {
                                                home.moveToFolder(id, target.app.id, null)
                                                restoreEpoch++
                                            }
                                        }
                                    }
                                    )
                            } else {
                                null
                            },
                        onAppInfo =
                            if (appInfoAvailable) {
                                (
                                    {
                                        appMenu = null
                                        restoreEpoch++
                                        scope.launch {
                                            val request = resolveAppInformation(context, target.app)
                                            if (
                                                request == null ||
                                                systemActions.content(request) != true
                                            ) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.settings_error,
                                                    Toast.LENGTH_LONG,
                                                )
                                                    .show()
                                            }
                                        }
                                    }
                                    )
                            } else {
                                null
                            },
                        onUninstall =
                            if (eligible) {
                                (
                                    {
                                        appMenu = null
                                        restoreEpoch++
                                        scope.launch {
                                            val request = uninstall.intent(target.app.packageName)
                                            if (request == null) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.uninstall_error,
                                                    Toast.LENGTH_LONG,
                                                )
                                                    .show()
                                            } else {
                                                try {
                                                    uninstallResult.launch(request)
                                                } catch (_: android.content.ActivityNotFoundException) {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.uninstall_error,
                                                        Toast.LENGTH_LONG,
                                                    )
                                                        .show()
                                                } catch (_: SecurityException) {
                                                    Toast.makeText(
                                                        context,
                                                        R.string.uninstall_error,
                                                        Toast.LENGTH_LONG,
                                                    )
                                                        .show()
                                                }
                                            }
                                        }
                                    }
                                    )
                            } else {
                                null
                            },
                        backdrop = target.backdrop,
                        label = target.app.label,
                        anchor = target.anchor,
                        awaitingOpeningRelease = target.awaitingRelease,
                        favorite = target.app.id in organization?.favorites.orEmpty(),
                        saving = organizationSaving,
                        failed = organizationWriteFailed,
                        reduceMotion =
                            active.reduceMotion ||
                                systemReduced ||
                                active.quality == QualityTier.ECONOMY,
                        dismissRequested = target.dismissRequested,
                        reduceTransparency = active.reduceTransparency,
                        onOpen = {
                            if (!organizationSaving && appMenu != null) {
                                appMenu = null
                                restoreEpoch++
                                onOpen(target.app)
                            }
                        },
                        onFavorite = { favorite ->
                            if (!organizationSaving && appMenu != null) {
                                organizationSaving = true
                                scope.launch {
                                    try {
                                        profile?.id?.let { id ->
                                            home.setFavorite(id, target.app.id, favorite)
                                        }
                                        appMenu = appMenu?.copy(dismissRequested = true)
                                    } catch (_: IOException) {
                                        organizationWriteFailed = true
                                    } finally {
                                        organizationSaving = false
                                    }
                                }
                            }
                        },
                        onClose = {
                            if (!organizationSaving) {
                                appMenu = null
                                restoreEpoch++
                            }
                        },
                    )
                }
                if (diagnosticsOpen) {
                    ContentCapabilitiesPanel(
                        launcher.contentCapabilities,
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                        onClose = { diagnosticsOpen = false },
                    )
                }
                (backupReview ?: restoreProgress?.review)?.let { review ->
                    val saving = backupBusy || restoreProgress?.saving == true
                    BackupReviewPanel(
                        review,
                        saving,
                        backupFailed || restoreProgress?.failed == true,
                        onClose = {
                            if (!saving) {
                                backupReview = null
                                backupFailed = false
                                settingsViewModel.clearRestore()
                            }
                        },
                        onRestore = {
                            if (!saving) {
                                backupFailed = false
                                settingsViewModel.restore(review)
                            }
                        },
                    )
                }
                if (ambientOpen) {
                    AmbientScreen(
                        frame,
                        profile?.ambientPhotos == true,
                        profile?.ambientClock != false,
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                        launcher.localPhotos::load,
                        onClose = {
                            ambientOpen = false
                            restoreEpoch++
                        },
                    )
                }
                photoPreview?.let { photo ->
                    AmbientScreen(
                        frame,
                        false,
                        false,
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                        launcher.localPhotos::load,
                        onClose = {
                            photoPreview = null
                            restoreEpoch++
                        },
                        photo = android.net.Uri.parse(photo.uri),
                    )
                }
                if (commands) {
                    val statusFlow = remember(context) { controlStatus(context.applicationContext) }
                    val controlStatus by
                        statusFlow.collectAsStateWithLifecycle(initialValue = ControlStatus())
                    CommandPanel(
                        sourceKey = frame,
                        onEditingChanged = { controlsEditing = it },
                        status = controlStatus,
                        sizes = active.controls,
                        positions = active.controlPositions,
                        initiallyCustomizing = customizeControls,
                        onLayout = { layout ->
                            savePreferences(
                                active.copy(
                                    controls =
                                        active.controls + layout.associate { it.id to it.size },
                                    controlPositions =
                                        active.controlPositions +
                                            layout.associate {
                                                it.id to ControlPosition(it.column, it.row)
                                            },
                                ),
                            )
                        },
                        writeFailed = preferenceState?.status == PreferenceSaveStatus.FAILED,
                        onRetrySave = { profile?.id?.let(settingsViewModel::retryPreferences) },
                        reduceMotion =
                            active.reduceMotion ||
                                systemReduced ||
                                active.quality == QualityTier.ECONOMY,
                        reduceTransparency = active.reduceTransparency,
                        onSettings = {
                            commands = false
                            settingsCategory = SettingsCategory.APPEARANCE
                            cacheBytes = loader.memoryCache?.size ?: 0L
                            settingsOpen = true
                        },
                        onWallpaper = {
                            commands = false
                            wallpaperOpen = true
                            profileCommandFailed = false
                        },
                        onNetwork = {
                            commands = false
                            if (systemActions.network() != true) {
                                Toast.makeText(context, R.string.settings_error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        },
                        onAudio = {
                            commands = false
                            if (systemActions.audio() != true) {
                                Toast.makeText(context, R.string.settings_error, Toast.LENGTH_LONG)
                                    .show()
                            }
                        },
                        onClose = {
                            commands = false
                            customizeControls = false
                            restoreEpoch++
                        },
                    )
                }
                if (profilesOpen && profiles != null) {
                    val profilesModel: ProfilesViewModel =
                        org.koin.compose.viewmodel.koinViewModel()
                    val profileState by profilesModel.state.collectAsStateWithLifecycle()
                    LaunchedEffect(profileState.completed) {
                        if (profileState.completed) {
                            settingsOpen = false
                            profilesOpen = false
                            restoreEpoch++
                            profilesModel.acknowledge()
                        }
                    }
                    ProfilePanel(
                        profiles!!,
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                        active.reduceTransparency || active.highContrast,
                        profileState.saving,
                        profileState.failed,
                        onSelect = profilesModel::select,
                        onSave = profilesModel::save,
                        onDelete = profilesModel::delete,
                        onClose = {
                            profilesOpen = false
                            profilesModel.acknowledge()
                            restoreEpoch++
                        },
                        sourceKey = frame,
                    )
                }
                if (photoGalleryOpen && profile != null) {
                    PhotoGalleryPanel(
                        reduced =
                            active.reduceMotion ||
                                systemReduced ||
                                active.quality == QualityTier.ECONOMY,
                        loadPhotos = launcher.localPhotos::load,
                        sourceKey = frame,
                        saving = profileSaving,
                        failed = profileFailed,
                        onChoose = { uri -> if (!profileSaving) importPhoto(uri, profile.id) },
                        onSystemPicker = {
                            importProfileId = profile.id
                            try {
                                imagePicker.launch("image/*")
                            } catch (_: android.content.ActivityNotFoundException) {
                                importProfileId = null
                                profileCommandFailed = true
                            }
                        },
                        onClose = {
                            photoGalleryOpen = false
                            profileCommandFailed = false
                            launcher.clearImportResult()
                        },
                    )
                }
                if (wallpaperOpen && !photoGalleryOpen && profile != null) {
                    WallpaperPanel(
                        profile,
                        content,
                        active.reduceMotion ||
                            systemReduced ||
                            active.quality == QualityTier.ECONOMY,
                        profileSaving,
                        profileFailed,
                        onMode = { mode ->
                            changeProfile { launcher.setWallpaper(profile.id, mode) }
                        },
                        onLandscape = {
                            changeProfile {
                                launcher.setLandscape(profile.id, (profile.landscape + 1) % 2)
                            }
                        },
                        onImport = {
                            photoGalleryOpen = true
                            profileCommandFailed = false
                        },
                        onContent = {
                            if (
                                profile.contentEnabled ||
                                context.checkSelfPermission(
                                    "android.permission.READ_TV_LISTINGS",
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                changeProfile {
                                    launcher.setContentEnabled(profile.id, !profile.contentEnabled)
                                }
                            } else {
                                contentPermissionProfile = profile.id
                                contentPermission.launch("android.permission.READ_TV_LISTINGS")
                            }
                        },
                        onClose = {
                            wallpaperOpen = false
                            launcher.clearImportResult()
                            restoreEpoch++
                        },
                    )
                }
            }
        }
    }
}
