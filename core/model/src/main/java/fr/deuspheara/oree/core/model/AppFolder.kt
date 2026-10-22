package fr.deuspheara.oree.core.model

const val MAX_APP_FOLDERS = 40

data class AppFolder(val id: String, val name: String, val apps: List<String> = emptyList()) {
    val homeId: String
        get() = "folder:$id"
}

/** Uninstalled members stay in storage, but never become invented launchable cards. */
fun folderApps(folder: AppFolder, catalog: List<TvApp>): List<TvApp> {
    val installed = catalog.associateBy { it.id }
    return folder.apps.mapNotNull(installed::get)
}

fun validFolderName(name: String): String = name.trim().also {
    require(it.isNotEmpty() && it.length <= 40 && it.none(Char::isISOControl))
}

fun validFolderComponent(id: String): Boolean {
    val parts = id.split('/')
    return parts.size == 2 &&
        parts[0].matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) &&
        parts[1].matches(Regex("[.]?[A-Za-z_$][A-Za-z0-9_.$]*")) &&
        id.length <= 512
}
