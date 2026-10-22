package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.component.ArtworkPhase
import fr.deuspheara.oree.feature.home.presentation.component.FolderTile
import fr.deuspheara.oree.feature.home.presentation.component.HomeArtworkPlaceholder

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContentCard(
    entry: ContentEntry,
    app: TvApp?,
    reduced: Boolean,
    modifier: Modifier,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val context = LocalContext.current
    val request =
        remember(entry.artwork, app?.artworkCacheKey) {
            val data =
                entry.artwork
                    ?: app?.takeIf { it.artwork != 0 }
                        ?.let {
                            android.net.Uri.parse(
                                "android.resource://${it.packageName}/${it.artwork}",
                            )
                        }
            data?.let { ImageRequest.Builder(context).data(it).size(480, 270).build() }
        }
    Column(
        modifier
            .onFocusChanged {
                focused = it.isFocused
                if (focused) onFocus()
            }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionUp || event.key == Key.DirectionDown) {
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.DirectionUp) onUp() else onDown()
                    }
                    true
                } else {
                    false
                }
            }
            .semantics { contentDescription = "${entry.title}, ${app?.label ?: entry.source}" }
            .testTag("content:${entry.id}")
            .combinedClickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FocusArtwork(
            focused,
            pressed,
            reduced,
            Offset.Zero,
            Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        ) {
            Text(
                app?.label?.take(2) ?: entry.title.take(2),
                Modifier.align(Alignment.Center),
                color = D.foreground,
                fontSize = 24.sp,
            )
            if (request != null) {
                AsyncImage(
                    request,
                    null,
                    Modifier.fillMaxSize(),
                    contentScale =
                        if (entry.artwork != null) ContentScale.Crop else ContentScale.Fit,
                )
            }
        }
        Text(
            entry.title,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 14.sp,
        )
        Text(
            app?.label ?: entry.source,
            color = Color.White.copy(alpha = .75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
        )
    }
}
