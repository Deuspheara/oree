package fr.deuspheara.oree.core.domain.source

import fr.deuspheara.oree.core.model.ContentCapabilities

interface CapabilityDataSource {
    suspend fun inspect(): ContentCapabilities
}
