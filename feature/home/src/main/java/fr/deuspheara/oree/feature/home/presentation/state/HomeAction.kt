package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.ui.geometry.Rect
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.TvApp

/** Available header/menu affordances are independent from their event delivery. */
enum class HomeControl { COMMANDS, PROFILES, SEARCH, APP_MENU }

/** Screen intentions only; per-card focus, press, tilt and scrolling stay in Compose. */
sealed interface HomeAction {
    data class OpenApp(val app: TvApp) : HomeAction
    data object OpenSettings : HomeAction
    data object Retry : HomeAction
    data object OpenCommands : HomeAction
    data object OpenProfiles : HomeAction
    data class SelectArtwork(val app: TvApp?) : HomeAction
    data class OpenContent(val entry: ContentEntry) : HomeAction
    data class MoveFavorite(val direction: Int) : HomeAction
    data object CommitMove : HomeAction
    data object CancelMove : HomeAction
    data class OpenFolder(val id: String) : HomeAction
    data object OpenSearch : HomeAction
    data class OpenAppMenu(val app: TvApp, val anchor: Rect, val awaitingRelease: Boolean) : HomeAction
}
