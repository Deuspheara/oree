package fr.deuspheara.oree.core.designsystem.glass

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toLong
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
private class RetainedSurfaceCapture {
    var recorded by mutableStateOf(false)
    var pixels by mutableStateOf<ImageBitmap?>(null)
    var failed by mutableStateOf(false)
}

/** Exact static glass pixels, retained only in composition. No disk or saved-state bitmap. */
@Composable
fun RetainedMenuSurface(
    sourceKey: Any?,
    reduced: Boolean,
    modifier: Modifier = Modifier,
    role: GlassSurfaceRole = GlassSurfaceRole.MENU,
    radius: Dp = 26.dp,
    treatment: GlassTreatment = GlassTreatment.SOFT,
    progressive: dev.chrisbanes.haze.HazeProgressive? = null,
    performanceMode: dev.chrisbanes.haze.HazePerformanceMode =
        dev.chrisbanes.haze.HazePerformanceMode.Balanced,
) {
    val source = LocalGlassSource.current
    var position by remember { mutableStateOf(Offset.Unspecified) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val windowInfo = LocalWindowInfo.current
    val window = windowInfo.containerSize
    val visible =
        position.isSpecified &&
            position.x < window.width &&
            position.x + size.width > 0 &&
            position.y < window.height &&
            position.y + size.height > 0
    val enabled =
        visible &&
            windowInfo.isWindowFocused &&
            Build.VERSION.SDK_INT >= 33 &&
            source != null &&
            LocalLiquidGlass.current &&
            !LocalReducedTransparency.current &&
            !LocalOpaquePanels.current
    val layer = rememberGraphicsLayer()
    // Reset capture ownership, not the live material node: moving must reuse its shader stages.
    val materialKey = listOf(
        sourceKey, source, LocalLiquidGlass.current, LocalGlassIntensity.current,
        LocalReducedTransparency.current, LocalOpaquePanels.current,
        DesignTokens.surface, DesignTokens.background, size, role, radius, treatment, progressive,
        performanceMode, LocalDensity.current, LocalLayoutDirection.current,
    )
    val capture = remember(materialKey, position) { RetainedSurfaceCapture() }
    LaunchedEffect(enabled, capture, capture.recorded) {
        if (
            enabled &&
            capture.recorded &&
            capture.pixels == null &&
            !capture.failed &&
            size.width > 0 &&
            size.height > 0 &&
            size.width.toLong() * size.height <= 4_194_304L
        ) {
            // Entry, scrolling and wallpaper crossfades must settle before freezing optical output.
            delay(if (reduced) 350 else maxOf(350, MotionTokens.PANEL_MILLIS + 80).toLong())
            try {
                capture.pixels = withContext(Dispatchers.Default) { layer.toImageBitmap() }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                capture.failed = true
            } catch (_: IllegalArgumentException) {
                capture.failed = true
            }
        }
    }
    val bounds =
        modifier
            .onSizeChanged { size = it }
            .onGloballyPositioned { position = it.positionInWindow() }
    val image = capture.pixels
    if (image != null) {
        Image(
            image,
            null,
            bounds.testTag("retained-menu-image"),
            contentScale = ContentScale.FillBounds,
        )
    } else {
        Box(
            bounds
                .drawWithContent { if (visible) drawContent() }
                .then(
                    if (enabled) {
                        Modifier.drawWithContent {
                            layer.record { this@drawWithContent.drawContent() }
                            drawLayer(layer)
                            if (!capture.recorded) capture.recorded = true
                        }
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (role == GlassSurfaceRole.MENU) {
                        Modifier.menuSurface(performanceMode = performanceMode)
                    } else {
                        Modifier.glassSurface(
                            radius = radius,
                            treatment = treatment,
                            role = role,
                            progressive = progressive,
                            performanceMode = performanceMode,
                        )
                    },
                ),
        )
    }
}
