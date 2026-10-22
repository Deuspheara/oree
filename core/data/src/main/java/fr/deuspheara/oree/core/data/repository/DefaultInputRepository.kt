package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.repository.InputRepository
import fr.deuspheara.oree.core.domain.source.TvInputDataSource

class DefaultInputRepository(private val source: TvInputDataSource) : InputRepository {
    override val inputs = source.inputs

    override suspend fun contains(id: String) = source.contains(id)
}
