package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens as D
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContinuationKind
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
    immersive: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val context = LocalContext.current
    var artworkFailed by remember(entry.artwork) { mutableStateOf(false) }
    val request =
        remember(entry.artwork, app?.artworkCacheKey, artworkFailed) {
            val data =
                entry.artwork.takeUnless { artworkFailed }
                    ?: app?.takeIf { it.artwork != 0 }
                        ?.let {
                            android.net.Uri.parse(
                                "android.resource://${it.packageName}/${it.artwork}",
                            )
                        }
            data?.let { ImageRequest.Builder(context).data(it).size(480, 270).build() }
        }
    val episode = entry.episodeTitle ?: when {
        entry.seasonNumber != null && entry.episodeNumber != null ->
            stringResource(R.string.content_season_episode, entry.seasonNumber.orEmpty(), entry.episodeNumber.orEmpty())

        entry.episodeNumber != null -> stringResource(R.string.content_episode, entry.episodeNumber.orEmpty())

        else -> null
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
            .padding(vertical = if (immersive) 6.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (immersive) 12.dp else 8.dp),
    ) {
        FocusArtwork(
            focused,
            pressed,
            reduced,
            Offset.Zero,
            Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            cacheArtwork = true,
            focusSpec = HomeCardFocusSpec,
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
                        if (entry.artwork != null && !artworkFailed) ContentScale.Crop else ContentScale.Fit,
                    onError = { if (!artworkFailed) artworkFailed = true },
                )
            }
            if (immersive) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .82f)),
                        ),
                    ),
                )
                app?.label?.let { label ->
                    Text(
                        label,
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                            .background(Color.Black.copy(alpha = .35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Column(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        stringResource(
                            if (entry.continuationKind == ContinuationKind.CONTINUE ||
                                entry.playbackProgress != null
                            ) {
                                R.string.continue_action
                            } else {
                                R.string.content_watch
                            },
                        ),
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                    if (episode !=
                        null
                    ) {
                        Text(episode, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    entry.playbackProgress?.let { progress ->
                        Box(
                            Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .35f)),
                        ) {
                            Box(Modifier.fillMaxWidth(progress).height(3.dp).background(Color.White))
                        }
                    }
                }
            }
        }
        Text(
            entry.title,
            color = Color.White.copy(alpha = if (immersive && !LocalHighContrast.current) .60f else 1f),
            fontWeight = if (immersive) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = if (immersive) 16.sp else 14.sp,
            textAlign = if (immersive) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!immersive) {
            if (episode != null) {
                Text(episode, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
            }
            entry.playbackProgress?.let { progress ->
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = .25f))) {
                    Box(Modifier.fillMaxWidth(progress).height(3.dp).background(Color.White))
                }
            }
            Text(
                app?.label ?: entry.source,
                color = Color.White.copy(alpha = .75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
        }
    }
}
