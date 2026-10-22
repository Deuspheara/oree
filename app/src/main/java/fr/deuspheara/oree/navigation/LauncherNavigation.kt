package fr.deuspheara.oree.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import fr.deuspheara.oree.feature.settings.presentation.state.SettingsCategory

/** Save only navigation identities; frames, requesters, and input/animation state stay local. */
class LauncherNavigation(var profileId: String? = null) {
    val profilesOpen = mutableStateOf(false)
    val wallpaperOpen = mutableStateOf(false)
    val photoGalleryOpen = mutableStateOf(false)
    val settingsOpen = mutableStateOf(false)
    val commands = mutableStateOf(false)
    val searchOpen = mutableStateOf(false)
    val openFolder = mutableStateOf<String?>(null)
    val renameFolder = mutableStateOf<String?>(null)
    val searchQuery = mutableStateOf("")
    val returnToSearch = mutableStateOf(false)
    val settingsCategory = mutableStateOf(SettingsCategory.APPEARANCE)

    companion object {
        val Saver =
            listSaver<LauncherNavigation, Any?>(
                save = {
                    listOf(
                        it.profileId,
                        it.profilesOpen.value,
                        it.wallpaperOpen.value,
                        it.photoGalleryOpen.value,
                        it.settingsOpen.value,
                        it.commands.value,
                        it.searchOpen.value,
                        it.openFolder.value,
                        it.renameFolder.value,
                        it.searchQuery.value,
                        it.returnToSearch.value,
                        it.settingsCategory.value.name,
                    )
                },
                restore = { values ->
                    LauncherNavigation(values[0] as String?).apply {
                        profilesOpen.value = values[1] as Boolean
                        wallpaperOpen.value = values[2] as Boolean
                        photoGalleryOpen.value = values[3] as Boolean
                        settingsOpen.value = values[4] as Boolean
                        commands.value = values[5] as Boolean
                        searchOpen.value = values[6] as Boolean
                        openFolder.value = values[7] as String?
                        renameFolder.value = values[8] as String?
                        searchQuery.value = values[9] as String
                        returnToSearch.value = values[10] as Boolean
                        settingsCategory.value = SettingsCategory.valueOf(values[11] as String)
                    }
                },
            )
    }
}
