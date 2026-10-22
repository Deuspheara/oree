package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.ContentCapabilities

interface CapabilityRepository {
    suspend fun inspect(): ContentCapabilities
}
