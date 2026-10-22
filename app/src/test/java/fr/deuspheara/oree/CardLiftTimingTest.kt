package fr.deuspheara.oree

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import org.junit.Assert.assertTrue
import org.junit.Test

class CardLiftTimingTest {
    @Test
    fun originalLiftHasAVisibleCatchUpInterval() {
        val animation =
            TargetBasedAnimation(
                spring<Float>(dampingRatio = .82f, stiffness = 550f),
                Float.VectorConverter,
                0f,
                1f,
            )
        val at32 = animation.getValueFromNanos(32_000_000L)
        println(
            "Original lift fraction at32ms=$at32; nominal durationMs=${animation.durationNanos / 1_000_000}",
        )
        assertTrue(at32 < .9f)
        assertTrue(animation.durationNanos > 100_000_000L)
    }
}
