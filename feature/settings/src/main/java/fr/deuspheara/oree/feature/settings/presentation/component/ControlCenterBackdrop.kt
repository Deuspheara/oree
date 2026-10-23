@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt

/** A feathered underlay around the actual panel, also recorded as the tiles' optical source. */
@Composable
internal fun ControlCenterBackdrop(image: ImageBitmap?, opaque: Boolean, alpha: () -> Float, panel: Rect, source: HazeState) {
    if (panel.isEmpty) return
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    val bounds = Rect(
        panel.left.coerceAtLeast(0f),
        panel.top.coerceAtLeast(0f),
        panel.right.coerceAtMost(window.width.toFloat()),
        panel.bottom.coerceAtMost(window.height.toFloat()),
    )
    if (bounds.isEmpty) return
    val feather = with(density) { 24.dp.toPx() }
    val edgeX = (feather / bounds.width).coerceIn(.01f, .45f)
    val edgeY = (feather / bounds.height).coerceIn(.01f, .45f)
    val horizontalMask = remember(edgeX) {
        Brush.horizontalGradient(0f to Color.Transparent, edgeX to Color.Black, (1f - edgeX) to Color.Black, 1f to Color.Transparent)
    }
    val verticalMask = remember(edgeY) {
        Brush.verticalGradient(0f to Color.Transparent, edgeY to Color.Black, (1f - edgeY) to Color.Black, 1f to Color.Transparent)
    }
    val style = remember(verticalMask) {
        GlassStyle.clear.then {
            shape(RoundedCornerShape(0.dp))
            backgroundColor(Color.Transparent)
            tint(Color.Transparent)
            contrast(0f)
            whitePoint(0f)
            specularIntensity(0f)
            ambientResponse(0f)
            edgeShadow(Color.Transparent)
            optics(
                refractionStrength = 0f,
                refractionDisplacement = 0.dp,
                blurRadius = 12.dp,
                progressive = HazeProgressive.Brush(
                    verticalMask,
                ),
                refractionDetailIntensity = 0f,
            )
        }
    }
    Canvas(
        Modifier.offset { IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()) }
            .size(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                this.alpha = alpha()
            }
            .hazeSource(source)
            .drawWithContent {
                drawContent()
                drawRect(
                    horizontalMask,
                    blendMode = BlendMode.DstIn,
                )
                drawRect(
                    verticalMask,
                    blendMode = BlendMode.DstIn,
                )
            }
            .then(if (image != null && !opaque) Modifier.hazeGlass(input = HazeInput.Content, style = style) else Modifier),
    ) {
        if (image != null && !opaque) {
            // Sample the complete screenshot in window coordinates; resizing the panel never crops
            // or stretches the image beneath an individual tile.
            translate(-bounds.left, -bounds.top) {
                drawImage(image, dstSize = window)
            }
            drawRect(Color.Black.copy(alpha = .10f))
        } else {
            drawRect(Color.Black.copy(alpha = .82f))
        }
    }
}
