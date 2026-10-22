package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.TvInputs
import kotlinx.coroutines.flow.Flow

interface InputRepository {
    val inputs: Flow<TvInputs>

    suspend fun contains(id: String): Boolean
}
