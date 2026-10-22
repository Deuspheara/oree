package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.domain.source.ContentSelection
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun refresh()

    fun observe(watchNextFeed: Boolean, appPreviews: Boolean, wallpaper: Boolean, packageName: Flow<String?>): Flow<ContentSelection>
}
