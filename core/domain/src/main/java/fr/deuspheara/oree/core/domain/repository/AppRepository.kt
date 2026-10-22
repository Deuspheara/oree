package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.TvApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    val changes: Flow<Unit>

    suspend fun load(): List<TvApp>
}
