package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.state.HomeFocusState

@Composable
internal fun HomeContinuationRow(
    entries: List<ContentEntry>,
    apps: List<TvApp>,
    focus: HomeFocusState,
    heading: String,
    cardWidth: Dp,
    large: Boolean,
    reduced: Boolean,
    onFocus: (ContentEntry) -> Unit,
    onOpen: (ContentEntry) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
    viewportWidth: Dp? = null,
    onEnterFromBelow: (() -> Boolean)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (large) 4.dp else 8.dp)) {
        Text(
            heading,
            color = Color.White.copy(alpha = if (large && !LocalHighContrast.current) .60f else 1f),
            modifier = Modifier.padding(start = 28.dp).testTag("continuation-heading"),
            fontSize = 20.sp,
            fontWeight = if (large) FontWeight.SemiBold else FontWeight.Normal,
        )
        androidx.compose.foundation.layout.Box {
            LazyRow(
                state = focus.contentRow,
                contentPadding = PaddingValues(horizontal = if (viewportWidth != null) 80.dp else 28.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (large) 48.dp else 28.dp),
                modifier = (
                    if (viewportWidth !=
                        null
                    ) {
                        Modifier.requiredWidth(viewportWidth)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    ).focusProperties {
                        onEnter = {
                            if (requestedFocusDirection == FocusDirection.Up && onEnterFromBelow?.invoke() == false) {
                                cancelFocusChange()
                            }
                        }
                    }.testTag("watch-next-row"),
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.id }, contentType = { _, _ -> "content" }) { index, entry ->
                    val requester = remember(entry.id) { FocusRequester() }
                    DisposableEffect(entry.id) {
                        focus.contentRequesters[entry.id] = requester
                        onDispose { focus.contentRequesters.remove(entry.id) }
                    }
                    ContentCard(
                        entry, apps.firstOrNull { it.packageName == entry.source }, reduced,
                        Modifier.width(cardWidth).focusRequester(requester).focusProperties {
                            start = if (index == 0) FocusRequester.Cancel else FocusRequester.Default
                            end = if (index == entries.lastIndex) FocusRequester.Cancel else FocusRequester.Default
                        },
                        onFocus = { onFocus(entry) },
                        onClick = { onOpen(entry) },
                        onUp = onUp,
                        onDown = onDown,
                        immersive = large,
                    )
                }
            }
        }
    }
}
