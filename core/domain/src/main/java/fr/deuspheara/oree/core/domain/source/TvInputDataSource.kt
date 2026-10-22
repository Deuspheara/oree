package fr.deuspheara.oree.core.domain.source

import fr.deuspheara.oree.core.model.TvInputs
import kotlinx.coroutines.flow.Flow

interface TvInputDataSource {
    val inputs: Flow<TvInputs>

    suspend fun contains(id: String): Boolean
}
