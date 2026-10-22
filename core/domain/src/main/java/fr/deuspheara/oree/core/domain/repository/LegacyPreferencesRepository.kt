package fr.deuspheara.oree.core.domain.repository

import fr.deuspheara.oree.core.model.LauncherPreferences
import kotlinx.coroutines.flow.Flow

interface LegacyPreferencesRepository {
    val preferences: Flow<LauncherPreferences>
}
