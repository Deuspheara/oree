package fr.deuspheara.oree.core.domain.repository

/** Atomic profile-scoped favorites/folders; membership invariants stay in one transaction. */
interface OrganizationRepository {
    suspend fun setFavorite(id: String, component: String, favorite: Boolean)

    suspend fun reorderFavorites(id: String, expected: List<String>, order: List<String>)

    suspend fun createFolder(profileId: String, name: String, appId: String)

    suspend fun renameFolder(profileId: String, folderId: String, name: String)

    suspend fun deleteFolder(profileId: String, folderId: String)

    suspend fun moveToFolder(profileId: String, appId: String, folderId: String?)

    suspend fun reorderFolder(profileId: String, folderId: String, expected: List<String>, order: List<String>)
}
