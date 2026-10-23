package fr.deuspheara.oree.core.designsystem.artwork

import android.os.Trace
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.avatar.smoothCircleClip
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.ReflectionColors
import fr.deuspheara.oree.core.model.reflectionColors
import kotlin.math.ceil

val LocalMotionIntensity = androidx.compose.runtime.compositionLocalOf { 1f }

val LocalFastNavigation = androidx.compose.runtime.compositionLocalOf { false }

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
    cacheArtwork: Boolean = false,
    focusSpec: FiniteAnimationSpec<Float> = spring(dampingRatio = .82f, stiffness = 550f),
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    // Keep composition-local reads outside focus-dependent branches so the remembered
    // animation state retains its slot when focus enters or leaves the card.
    val motionIntensity = LocalMotionIntensity.current
    val fastNavigation = LocalFastNavigation.current
    val highContrast = LocalHighContrast.current
    val motionReduced = reduced || fastNavigation || motionIntensity == 0f
    val ring = focused && (motionReduced || highContrast)
    val surface = DesignTokens.surface
    val accent = DesignTokens.accent
    val lift =
        if (immediateFocus) {
            androidx.compose.runtime.rememberUpdatedState(if (focused && !motionReduced) 1f else 0f)
        } else {
            animateFloatAsState(
                if (focused && !motionReduced) 1f else 0f,
                focusSpec,
                label = "artworkLift",
            )
        }
    val compression =
        animateFloatAsState(
            if (pressed && !motionReduced) .975f else 1f,
            spring(dampingRatio = .9f, stiffness = 1100f),
            label = "artworkPress",
        )
    val animatedTilt = animateOffsetAsState(
        if (focused && !motionReduced) tilt else Offset.Zero,
        spring(dampingRatio = .86f, stiffness = 650f),
        label = "artworkTilt",
    )
    // Cache only static card pixels, at the largest supported focus scale. The independent
    // reflection/transform nodes stay live; native input bounds and Coil ownership do not change.
    val artworkLayer = rememberGraphicsLayer()
    val artworkCache = if (cacheArtwork) {
        Modifier.graphicsLayer().drawWithCache {
            val rasterScale = MotionTokens.FOCUS_SCALE.coerceAtLeast(1f)
            val rasterSize = IntSize(ceil(size.width * rasterScale).toInt(), ceil(size.height * rasterScale).toInt())
            val bounded = rasterSize.width > 0 && rasterSize.height > 0 && rasterSize.width.toLong() * rasterSize.height <= 4_194_304L
            onDrawWithContent {
                if (!bounded) {
                    drawContent()
                    return@onDrawWithContent
                }
                artworkLayer.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                val tracing = android.os.Build.VERSION.SDK_INT >= 29 && Trace.isEnabled()
                if (tracing) {
                    Trace.beginSection(
                        "TVL:artworkRecord:${System.identityHashCode(artworkLayer)}:${rasterSize.width}x${rasterSize.height}",
                    )
                }
                try {
                    artworkLayer.record(size = rasterSize) {
                        scale(rasterScale, pivot = Offset.Zero) { this@onDrawWithContent.drawContent() }
                    }
                } finally {
                    if (tracing) Trace.endSection()
                }
                scale(1f / rasterScale, pivot = Offset.Zero) { drawLayer(artworkLayer) }
            }
        }
    } else {
        Modifier
    }
    Box(
        modifier.graphicsLayer {
            val amount = if (motionReduced) 0f else lift.value * motionIntensity
            val scale =
                if (motionReduced) {
                    1f
                } else {
                    (1f + (MotionTokens.FOCUS_SCALE - 1f) * amount) * (1f + (compression.value - 1f) * motionIntensity)
                }
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin.Center
            rotationX = if (motionReduced) 0f else -animatedTilt.value.y * motionIntensity * MotionTokens.TILT_DEGREES
            rotationY = if (motionReduced) 0f else animatedTilt.value.x * motionIntensity * MotionTokens.TILT_DEGREES
            cameraDistance = 12f * density
            shadowElevation = if (motionReduced) 0f else 18.dp.toPx() * amount
            this.shape = shape
            clip = false
        },
    ) {
        Box(
            Modifier.matchParentSize()
                .then(artworkCache)
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
                    onDrawWithContent {
                        drawContent()
                        if (edge != null) drawOutline(edge, edgeBrush, style = edgeStroke)
                        drawRect(shade)
                    }
                },
            content = content,
        )
        Box(
            Modifier.matchParentSize().then(
                if (shape == androidx.compose.foundation.shape.CircleShape) Modifier.smoothCircleClip() else Modifier.clip(shape),
            ).drawWithCache {
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
                    if (!motionReduced && lift.value > .001f) {
                        val dx = size.width * animatedTilt.value.x * motionIntensity * .3f
                        val dy = size.height * animatedTilt.value.y * motionIntensity * .3f
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
        )
        Box(Modifier.matchParentSize().border(if (ring) 2.dp else 0.dp, if (ring) accent else Color.Transparent, shape))
        // Badges share the artwork transform, but stay outside its shape clip.
        overlay()
    }
}
