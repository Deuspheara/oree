package fr.deuspheara.oree.feature.settings.presentation.state

import fr.deuspheara.oree.core.model.LauncherPreferences

enum class ExperienceControl { KEEP_CURRENT }
enum class ExperienceSaveStatus { IDLE, SAVING, FAILED }
sealed interface ExperienceAction {
    data class Apply(val value: LauncherPreferences) : ExperienceAction
    data object KeepCurrent : ExperienceAction
    data object Cancel : ExperienceAction
    data object Retry : ExperienceAction
}
