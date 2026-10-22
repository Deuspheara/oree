package fr.deuspheara.oree

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.height
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.scale
import androidx.compose.ui.test.then
import androidx.compose.ui.test.width
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.artwork.FocusArtwork
import fr.deuspheara.oree.core.designsystem.artwork.LocalHighContrast
import fr.deuspheara.oree.core.designsystem.avatar.smoothCircleClip
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import fr.deuspheara.oree.core.model.ReflectionColors
import fr.deuspheara.oree.core.model.reflectionColors

/** Pixel-only focus treatment. The caller keeps a separate, untransformed input rectangle. */
@Composable
private fun ReferenceFocusArtwork(
    focused: Boolean,
    pressed: Boolean,
    reduced: Boolean,
    tilt: Offset,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(DesignTokens.radius),
    edgeHighlight: Boolean = true,
    immediateFocus: Boolean = false,
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
                .drawWithContent {
                    drawContent()
                    if (edgeHighlight) {
                        drawOutline(
                            shape.createOutline(size, layoutDirection, this),
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = .34f),
                                    Color.Transparent,
                                    Color.Transparent,
                                ),
                                Offset.Zero,
                                Offset(size.width * .8f, size.height),
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                        )
                    }
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = .10f),
                            ),
                        ),
                    )
                    if (!reduced && lift.value > .001f) {
                        drawRect(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color.White.copy(alpha = .16f * lift.value),
                                        Color.Transparent,
                                    ),
                                center =
                                    Offset(
                                        size.width * (.45f + horizontal.value * .3f),
                                        size.height * (.12f + vertical.value * .3f),
                                    ),
                                radius = size.width * .85f,
                            ),
                        )
                    }
                },
            content = content,
        )
        // Badges share the artwork transform, but stay outside its shape clip.
        overlay()
    }
}

class ArtworkDrawCacheTest {
    @get:org.junit.Rule val compose = androidx.compose.ui.test.junit4.createComposeRule()

    @org.junit.Test
    fun cachedDrawingMatchesOriginalAcrossDirectionAndGeometryChanges() {
        var direction by androidx.compose.runtime.mutableStateOf(Offset.Zero)
        var selected by androidx.compose.runtime.mutableStateOf(false)
        var reduced by androidx.compose.runtime.mutableStateOf(false)
        var circle by androidx.compose.runtime.mutableStateOf(false)
        var wide by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            androidx.compose.foundation.layout.Row {
                val shape =
                    if (circle) {
                        androidx.compose.foundation.shape.CircleShape
                    } else {
                        RoundedCornerShape(16.dp)
                    }
                val width = if (wide) 210.dp else 180.dp
                Box(
                    Modifier.size(250.dp, 180.dp)
                        .background(Color(0xff141414))
                        .testTag("reference"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    ReferenceFocusArtwork(
                        selected,
                        false,
                        reduced,
                        direction,
                        Modifier.size(width, 100.dp),
                        shape = shape,
                        immediateFocus = true,
                    ) {
                        Box(Modifier.matchParentSize().background(Color(0xff536879)))
                    }
                }
                Box(
                    Modifier.size(250.dp, 180.dp).background(Color(0xff141414)).testTag("cached"),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    FocusArtwork(
                        selected,
                        false,
                        reduced,
                        direction,
                        Modifier.size(width, 100.dp),
                        shape = shape,
                        immediateFocus = true,
                    ) {
                        Box(Modifier.matchParentSize().background(Color(0xff536879)))
                    }
                }
            }
        }
        fun compare() {
            compose.waitForIdle()
            val a = compose.onNodeWithTag("reference").captureToImage().toPixelMap()
            val b = compose.onNodeWithTag("cached").captureToImage().toPixelMap()
            var total = 0.0
            var different = 0
            for (y in 0 until a.height) {
                for (x in 0 until a.width) {
                    val delta =
                        kotlin.math.max(
                            kotlin.math.abs(a[x, y].red - b[x, y].red),
                            kotlin.math.max(
                                kotlin.math.abs(a[x, y].green - b[x, y].green),
                                kotlin.math.abs(a[x, y].blue - b[x, y].blue),
                            ),
                        )
                    total += delta
                    if (delta > 3f / 255f) different++
                }
            }
            org.junit.Assert.assertTrue(
                "Equivalent mean pixels, delta=" + total / (a.width * a.height),
                total / (a.width * a.height) < .003,
            )
            org.junit.Assert.assertTrue(
                "No material changed area, pixels=" + different,
                different.toFloat() / (a.width * a.height) < .01f,
            )
        }
        compare()
        compose.runOnIdle { selected = true }
        compare()
        compose.runOnIdle { direction = Offset(.65f, -.65f) }
        compare()
        compose.runOnIdle {
            direction = Offset(-.65f, .65f)
            wide = true
        }
        compare()
        compose.runOnIdle { circle = true }
        compare()
        compose.runOnIdle { reduced = true }
        compare()
        compose.runOnIdle {
            selected = false
            circle = false
            wide = false
            reduced = false
        }
        compare()
    }

    @org.junit.Test
    fun animatedHighlightReusesGeometryWhileOriginalRebuildsIt() {
        val referenceCalls = java.util.concurrent.atomic.AtomicInteger()
        val cachedCalls = java.util.concurrent.atomic.AtomicInteger()
        fun countingShape(calls: java.util.concurrent.atomic.AtomicInteger) = object : androidx.compose.ui.graphics.Shape {
            override fun createOutline(
                size: androidx.compose.ui.geometry.Size,
                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                density: androidx.compose.ui.unit.Density,
            ): androidx.compose.ui.graphics.Outline {
                calls.incrementAndGet()
                return RoundedCornerShape(16.dp).createOutline(size, layoutDirection, density)
            }
        }
        val originalShape = countingShape(referenceCalls)
        val cachedShape = countingShape(cachedCalls)
        var tilt by androidx.compose.runtime.mutableStateOf(Offset.Zero)
        compose.setContent {
            androidx.compose.foundation.layout.Row {
                ReferenceFocusArtwork(
                    true,
                    false,
                    false,
                    tilt,
                    Modifier.size(180.dp, 100.dp),
                    shape = originalShape,
                    immediateFocus = true,
                ) {}
                FocusArtwork(
                    true,
                    false,
                    false,
                    tilt,
                    Modifier.size(180.dp, 100.dp),
                    shape = cachedShape,
                    immediateFocus = true,
                ) {}
            }
        }
        compose.waitForIdle()
        val originalBefore = referenceCalls.get()
        val cachedBefore = cachedCalls.get()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { tilt = Offset(.65f, -.65f) }
        repeat(24) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
        val original = referenceCalls.get() - originalBefore
        val cached = cachedCalls.get() - cachedBefore
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendStatus(
                2,
                android.os.Bundle().apply {
                    putInt("referenceOutlineCalls", original)
                    putInt("cachedOutlineCalls", cached)
                },
            )
        org.junit.Assert.assertTrue(
            "Actual animated outline construction must be lower: original=$original cached=$cached",
            original > cached,
        )
        compose.mainClock.autoAdvance = true
    }

    @org.junit.Test
    fun tintUpdatesWithArtworkAndReducedMotionRemovesIt() {
        var colors by
            androidx.compose.runtime.mutableStateOf(
                ReflectionColors(0xffff0000.toInt(), 0xffff0000.toInt(), 0xffff0000.toInt()),
            )
        var reduced by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            Box(
                Modifier.size(240.dp, 160.dp).background(Color(0xff141414)).testTag("tint"),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                FocusArtwork(
                    true,
                    false,
                    reduced,
                    Offset.Zero,
                    Modifier.size(180.dp, 100.dp),
                    immediateFocus = true,
                    reflectionColors = colors,
                ) {
                    Box(Modifier.matchParentSize().background(Color(0xff555555)))
                }
            }
        }
        fun sample(): Color {
            compose.waitForIdle()
            val pixels = compose.onNodeWithTag("tint").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height * 3 / 10]
        }
        val red = sample()
        org.junit.Assert.assertTrue("Artwork adds subtle red light", red.red > red.blue + 1f / 255)
        compose.runOnIdle {
            colors = ReflectionColors(0xff0000ff.toInt(), 0xff0000ff.toInt(), 0xff0000ff.toInt())
        }
        val blue = sample()
        org.junit.Assert.assertTrue(
            "New artwork invalidates old color",
            blue.blue > blue.red + 1f / 255,
        )
        compose.runOnIdle { reduced = true }
        val static = sample()
        org.junit.Assert.assertEquals(
            "Reduced motion omits reflection",
            static.red,
            static.blue,
            1f / 255,
        )
    }
}
