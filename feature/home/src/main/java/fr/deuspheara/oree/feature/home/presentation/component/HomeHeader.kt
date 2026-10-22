package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.feature.home.presentation.state.HomeAction
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.state.HomeFocusState

/** Header rendering and lateral focus edges; Home coordinates its return to the dock. */
@Composable
internal fun HomeHeader(
    loading: Boolean,
    profile: LocalProfile?,
    reduced: Boolean,
    controls: Set<HomeControl>,
    focus: HomeFocusState,
    onAction: (HomeAction) -> Unit,
    onFocused: (HomeControl) -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.testTag("home-header").onPreviewKeyEvent {
            if (it.key == Key.DirectionDown && !loading) {
                if (it.type == KeyEventType.KeyDown) onDown()
                true
            } else {
                false
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (HomeControl.SEARCH in controls) {
            HeaderControl(
                stringResource(R.string.search),
                LauncherSymbol.SEARCH,
                Modifier.focusRequester(focus.search).testTag("search").focusProperties {
                    start = FocusRequester.Cancel
                },
                reduced,
                { onFocused(HomeControl.SEARCH) },
                onClick = { onAction(HomeAction.OpenSearch) },
            )
        }
        HeaderControl(
            stringResource(R.string.command_center),
            LauncherSymbol.CONTROLS,
            Modifier.focusRequester(focus.commands).testTag("commands").focusProperties {
                if (HomeControl.SEARCH !in controls) start = FocusRequester.Cancel
            },
            reduced,
            { onFocused(HomeControl.COMMANDS) },
            end = HomeControl.PROFILES !in controls,
        ) {
            onAction(if (HomeControl.COMMANDS in controls) HomeAction.OpenCommands else HomeAction.OpenSettings)
        }
        if (HomeControl.PROFILES in controls) {
            HeaderControl(
                profile?.name ?: stringResource(R.string.profiles),
                LauncherSymbol.PROFILE,
                Modifier.focusRequester(focus.avatar).testTag("profiles"),
                reduced,
                { onFocused(HomeControl.PROFILES) },
                end = true,
                avatar = profile?.avatar,
                onClick = { onAction(HomeAction.OpenProfiles) },
            )
        }
        if (!loading) Spacer(Modifier.size(1.dp).testTag("home-ready"))
    }
}
