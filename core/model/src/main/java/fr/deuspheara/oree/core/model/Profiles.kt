package fr.deuspheara.oree.core.model

enum class WallpaperMode {
    LANDSCAPE,
    PERSONAL,
    APPLICATION,
    CONTENT,
}

data class LocalProfile(
    val id: String,
    val name: String,
    val avatar: String = AvatarIds.DEFAULT,
    val favorites: List<String> = emptyList(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val wallpaper: WallpaperMode = WallpaperMode.LANDSCAPE,
    val landscape: Int = 0,
    val image: String? = null,
    val contentEnabled: Boolean = false,
    val watchNextEnabled: Boolean = false,
    val folders: List<AppFolder> = emptyList(),
    val appPreviewsEnabled: Boolean = false,
    val ambientPhotos: Boolean = false,
    val ambientClock: Boolean = true,
)

data class ProfileSnapshot(val activeId: String, val profiles: List<LocalProfile>, val failed: Boolean = false) {
    val active: LocalProfile?
        get() = profiles.firstOrNull { it.id == activeId }
}

data class AppOrganization(val favorites: List<String> = emptyList(), val failed: Boolean = false)

data class BackupCandidate(val snapshot: ProfileSnapshot, val missingImages: Int)

data class BackupReview(val candidate: BackupCandidate, val expected: ProfileSnapshot, val undo: Boolean = false)
