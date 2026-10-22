package fr.deuspheara.oree.core.designsystem.avatar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.model.AvatarIds

@androidx.compose.runtime.Immutable
data class AvatarAsset(val id: String, @DrawableRes val resource: Int, @StringRes val label: Int)

/** Presentation metadata only; profile storage depends on AvatarIds, never resource paths. */
object AvatarCatalog {
    val entries =
        listOf(
            AvatarAsset("avatar_01", R.drawable.avatar_01, R.string.avatar_01_label),
            AvatarAsset("avatar_02", R.drawable.avatar_02, R.string.avatar_02_label),
            AvatarAsset("avatar_03", R.drawable.avatar_03, R.string.avatar_03_label),
            AvatarAsset("avatar_04", R.drawable.avatar_04, R.string.avatar_04_label),
            AvatarAsset("avatar_05", R.drawable.avatar_05, R.string.avatar_05_label),
            AvatarAsset("avatar_06", R.drawable.avatar_06, R.string.avatar_06_label),
            AvatarAsset("avatar_07", R.drawable.avatar_07, R.string.avatar_07_label),
            AvatarAsset("avatar_08", R.drawable.avatar_08, R.string.avatar_08_label),
        )

    fun resolve(id: String) = entries.firstOrNull { it.id == id } ?: entries.first { it.id == AvatarIds.DEFAULT }
}

/** Shared 12 MiB Coil cache, asynchronous decode capped at 192px, no full-size masters in APK. */
@Composable
fun ProfileAvatar(id: String, modifier: Modifier = Modifier, clipToCircle: Boolean = true) {
    val context = LocalContext.current
    val asset = AvatarCatalog.resolve(id)
    val request =
        remember(context, asset.id) {
            ImageRequest.Builder(context)
                .data(asset.resource)
                .size(192, 192)
                .memoryCacheKey("curated-avatar-v1:${asset.id}")
                .build()
        }
    Box(
        modifier
            .then(if (clipToCircle) Modifier.smoothCircleClip() else Modifier)
            .background(DesignTokens.surface),
    ) {
        AsyncImage(request, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/**
 * One physical pixel of alpha coverage smooths the circle independently of driver path AA. The
 * alpha mask stays local to this portrait-sized layer, outside the badge transform.
 */
fun Modifier.smoothCircleClip(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}
    .drawWithCache {
        val radius = (size.minDimension / 2).coerceAtLeast(1f)
        val mask =
            Brush.radialGradient(
                0f to Color.White,
                (radius - 1f) / radius to Color.White,
                1f to Color.Transparent,
                center = Offset(size.width / 2, size.height / 2),
                radius = radius,
            )
        onDrawWithContent {
            drawContent()
            drawRect(mask, blendMode = BlendMode.DstIn)
        }
    }
