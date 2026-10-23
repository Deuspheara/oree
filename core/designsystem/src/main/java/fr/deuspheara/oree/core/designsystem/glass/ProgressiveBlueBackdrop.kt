@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.core.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive

/** Feathered side material for the profile picker, with a quiet blue depth tint. */
@Composable
fun ProgressiveBlueBackdrop(sourceKey: Any?, reduced: Boolean, modifier: Modifier = Modifier) {
    val feather = remember { Brush.horizontalGradient(listOf(Color.Transparent, Color.Black)) }
    val mask = remember(feather) { HazeProgressive.Brush(feather) }
    Box(modifier) {
        RetainedMenuSurface(
            sourceKey,
            reduced,
            Modifier.matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(feather, blendMode = BlendMode.DstIn)
                },
            role = GlassSurfaceRole.CONTROL,
            radius = 0.dp,
            progressive = mask,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.horizontalGradient(listOf(Color.Transparent, Color(0xB31A3059))),
            ),
        )
    }
}
