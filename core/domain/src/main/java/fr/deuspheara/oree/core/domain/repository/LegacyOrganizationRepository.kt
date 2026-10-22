package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.AppOrganization
import kotlinx.coroutines.flow.Flow

interface LegacyOrganizationRepository {
    val organization: Flow<AppOrganization>
}
