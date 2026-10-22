package fr.deuspheara.oree.feature.settings.presentation.state

import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.TvInput

/** Independent integrations keep their own data contracts rather than one global screen state. */
data class ContentSettingsState(
    val watchNextEnabled: Boolean = false,
    val watchNextAvailability: ContentAvailability = ContentAvailability.DISABLED,
    val appPreviewsEnabled: Boolean = false,
    val appPreviewAvailability: ContentAvailability = ContentAvailability.DISABLED,
)
data class BackupSettingsState(val busy: Boolean = false, val failed: Boolean = false)
data class AmbientSettingsState(val visible: Boolean = false, val photos: Boolean = false, val clock: Boolean = true)

enum class SettingsControl {
    WALLPAPER,
    PROFILES,
    DIAGNOSTICS,
    APP_PREVIEWS,
    EXPORT,
    IMPORT,
    UNDO,
    AMBIENT,
    AMBIENT_PHOTOS,
    AMBIENT_CLOCK,
    DREAM_SETTINGS,
}

sealed interface SettingsAction {
    data object Close : SettingsAction
    data object AndroidSettings : SettingsAction
    data object ClearCache : SettingsAction
    data object RetrySave : SettingsAction
    data object CustomizeControls : SettingsAction
    data object Wallpaper : SettingsAction
    data object Profiles : SettingsAction
    data object ToggleWatchNext : SettingsAction
    data object RetryContent : SettingsAction
    data object Diagnostics : SettingsAction
    data object ToggleAppPreviews : SettingsAction
    data object ExportBackup : SettingsAction
    data object ImportBackup : SettingsAction
    data object UndoRestore : SettingsAction
    data object OpenAmbient : SettingsAction
    data object ToggleAmbientPhotos : SettingsAction
    data object ToggleAmbientClock : SettingsAction
    data object DreamSettings : SettingsAction
    data class ChangePreferences(val value: LauncherPreferences) : SettingsAction
    data class OpenInput(val input: TvInput) : SettingsAction
}
