package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.tv.material3.Text
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import kotlinx.coroutines.delay

enum class ArtworkPhase {
    WAITING,
    LOADING,
    READY,
    FAILED,
}

/** One decorative pass per request. No indefinite animation, even for a stuck decoder. */
@Composable
fun HomeArtworkPlaceholder(label: String, phase: ArtworkPhase, visible: Boolean, reduced: Boolean, modifier: Modifier = Modifier) {
    if (phase == ArtworkPhase.READY) return
    val progress = remember { Animatable(0f) }
    var started by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    val base = DesignTokens.surface
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(phase, visible, reduced, lifecycle) {
        if (phase == ArtworkPhase.LOADING && visible && !reduced && !started) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (!started) {
                    delay(120)
                    started = true
                    animating = true
                    try {
                        progress.animateTo(1f, tween(650, easing = LinearEasing))
                    } finally {
                        animating = false
                    }
                }
            }
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics {}
            .drawWithCache {
                val width = size.width * .45f
                val sheen =
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = .10f),
                            Color.Transparent,
                        ),
                        start = Offset.Zero,
                        end = Offset(width, 0f),
                    )
                onDrawBehind {
                    drawRect(base)
                    if (
                        phase == ArtworkPhase.LOADING &&
                        visible &&
                        !reduced &&
                        animating &&
                        progress.value < 1f
                    ) {
                        translate(left = -width + (size.width + width) * progress.value) {
                            drawRect(
                                sheen,
                                size = androidx.compose.ui.geometry.Size(width, size.height),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label.take(2).uppercase(), color = DesignTokens.secondary, fontSize = 30.sp)
    }
}
