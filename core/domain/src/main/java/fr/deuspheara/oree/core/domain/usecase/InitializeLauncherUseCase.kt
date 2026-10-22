package fr.deuspheara.oree.core.domain.usecase

import fr.deuspheara.oree.core.domain.repository.LegacyOrganizationRepository
import fr.deuspheara.oree.core.domain.repository.LegacyPreferencesRepository
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.first

class InitializeLauncherUseCase(
    private val profiles: ProfileRepository,
    private val preferences: LegacyPreferencesRepository,
    private val organization: LegacyOrganizationRepository,
) {
    suspend operator fun invoke() {
        // Existing profiles do not depend on successful reads of migration-only stores.
        val current = profiles.readSnapshot()
        if (current?.failed == true) throw java.io.IOException("Profiles unavailable")
        if (current == null) {
            profiles.initialize(preferences.preferences.first(), organization.organization.first())
        }
    }
}
