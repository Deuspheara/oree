package fr.deuspheara.oree.core.data.repository

import fr.deuspheara.oree.core.domain.repository.CapabilityRepository
import fr.deuspheara.oree.core.domain.source.CapabilityDataSource

class DefaultCapabilityRepository(private val source: CapabilityDataSource) : CapabilityRepository {
    override suspend fun inspect() = source.inspect()
}
