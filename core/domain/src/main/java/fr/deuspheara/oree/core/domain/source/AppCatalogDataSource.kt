package fr.deuspheara.oree.core.domain.source

import fr.deuspheara.oree.core.model.TvApp
import kotlinx.coroutines.flow.Flow

interface AppCatalogDataSource {
    val changes: Flow<Unit>

    suspend fun load(): List<TvApp>
}
