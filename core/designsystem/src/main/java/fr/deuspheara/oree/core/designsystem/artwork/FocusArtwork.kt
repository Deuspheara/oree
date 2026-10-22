package fr.deuspheara.oree.core.designsystem.artwork

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.avatar.smoothCircleClip
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.ReflectionColors
import fr.deuspheara.oree.core.model.reflectionColors

val LocalHighContrast = staticCompositionLocalOf { false }

/** Pixel-only focus treatment. The caller keeps a separate, untransformed input rectangle. */
@Composable
fun FocusArtwork(
    focused: Boolean,
    pressed: Boolean,
    reduced: Boolean,
    tilt: Offset,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(DesignTokens.radius),
    edgeHighlight: Boolean = true,
    immediateFocus: Boolean = false,
    reflectionColors: ReflectionColors? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val ring = focused && (reduced || LocalHighContrast.current)
    val surface = DesignTokens.surface
    val accent = DesignTokens.accent
    val lift =
        if (immediateFocus) {
            androidx.compose.runtime.rememberUpdatedState(if (focused && !reduced) 1f else 0f)
        } else {
            animateFloatAsState(
                if (focused && !reduced) 1f else 0f,
                spring(dampingRatio = .82f, stiffness = 550f),
                label = "artworkLift",
            )
        }
    val compression =
        animateFloatAsState(
            if (pressed && !reduced) .975f else 1f,
            spring(dampingRatio = .9f, stiffness = 1100f),
            label = "artworkPress",
        )
    val horizontal =
        animateFloatAsState(
            if (focused && !reduced) tilt.x else 0f,
            spring(dampingRatio = .86f, stiffness = 650f),
            label = "artworkTiltX",
        )
    val vertical =
        animateFloatAsState(
            if (focused && !reduced) tilt.y else 0f,
            spring(dampingRatio = .86f, stiffness = 650f),
            label = "artworkTiltY",
        )
    Box(
        modifier.graphicsLayer {
            val amount = if (reduced) 0f else lift.value
            val scale =
                if (reduced) {
                    1f
                } else {
                    (1f + (MotionTokens.FOCUS_SCALE - 1f) * amount) * compression.value
                }
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
            rotationX = if (reduced) 0f else -vertical.value * MotionTokens.TILT_DEGREES
            rotationY = if (reduced) 0f else horizontal.value * MotionTokens.TILT_DEGREES
            cameraDistance = 12f * density
            shadowElevation = if (reduced) 0f else 18.dp.toPx() * amount
            this.shape = shape
            clip = false
        },
    ) {
        Box(
            Modifier.matchParentSize()
                .border(
                    if (ring) 2.dp else 0.dp,
                    if (ring) accent else Color.Transparent,
                    shape,
                )
                .then(
                    if (shape == androidx.compose.foundation.shape.CircleShape) {
                        Modifier.smoothCircleClip()
                    } else {
                        Modifier.clip(shape)
                    },
                )
                .background(surface)
                .drawWithCache {
                    // Geometry and shaders depend on the native artwork size, not animation frames.
                    val edge =
                        if (edgeHighlight) {
                            shape.createOutline(size, layoutDirection, this)
                        } else {
                            null
                        }
                    val edgeBrush =
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = .34f),
                                Color.Transparent,
                                Color.Transparent,
                            ),
                            Offset.Zero,
                            Offset(size.width * .8f, size.height),
                        )
                    val edgeStroke = Stroke(1.dp.toPx())
                    val shade =
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = .10f),
                            ),
                        )
                    val reflection =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    (
                                        reflectionColors?.let {
                                            androidx.compose.ui.graphics.lerp(
                                                Color.White,
                                                Color(it.highlight),
                                                .18f,
                                            )
                                        } ?: Color.White
                                        )
                                        .copy(alpha = .16f),
                                    Color.Transparent,
                                ),
                            center = Offset(size.width * .45f, size.height * .12f),
                            radius = size.width * .85f,
                        )
                    onDrawWithContent {
                        drawContent()
                        if (edge != null) drawOutline(edge, edgeBrush, style = edgeStroke)
                        drawRect(shade)
                        if (!reduced && lift.value > .001f) {
                            val dx = size.width * horizontal.value * .3f
                            val dy = size.height * vertical.value * .3f
                            // Move the shader field while keeping the original clipped drawing
                            // bounds.
                            // Alpha is a draw parameter; no shader/color-list recreation on lift
                            // changes.
                            translate(dx, dy) {
                                drawRect(
                                    reflection,
                                    topLeft = Offset(-dx, -dy),
                                    size = size,
                                    alpha = lift.value,
                                )
                            }
                        }
                    }
                },
            content = content,
        )
        // Badges share the artwork transform, but stay outside its shape clip.
        overlay()
    }
}
