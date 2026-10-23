package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.animation.core.FastOutSlowInEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePageReturnMotionTest {
    @Test
    fun handoffCarriesUpwardSpeedAndFinishesAtRest() {
        val motion = HomePageReturnMotion()
        motion.sample(300f, 0, 0)
        motion.sample(295f, 20_000_000, 20_000_000)
        val easing = motion.easing(-200f, 30_000_000)
        val step = .0001f
        assertEquals(.525f, easing.transform(step) / step, .01f)
        assertEquals(0f, easing.transform(0f), 0f)
        assertEquals(1f, easing.transform(1f), 0f)
        assertTrue((1f - easing.transform(1f - step)) / step < .01f)
    }

    @Test
    fun fastHandoffsStayMonotoneAndNeverOvershoot() {
        for (distance in listOf(-1f, -100f, -1000f)) {
            val motion = HomePageReturnMotion()
            motion.sample(1000f, 0, 0)
            motion.sample(0f, 16_000_000, 16_000_000)
            val easing = motion.easing(distance, 20_000_000)
            assertTrue(easing.transform(.0001f) / .0001f <= .76f)
            var previous = 0f
            for (frame in 0..1000) {
                val value = easing.transform(frame / 1000f)
                assertTrue(value in previous..1f)
                previous = value
            }
        }
    }

    @Test
    fun staleOppositeAndResetSamplesKeepOriginalReturn() {
        val motion = HomePageReturnMotion()
        assertSame(FastOutSlowInEasing, motion.easing(-100f, 10))
        motion.sample(100f, 0, 0)
        motion.sample(90f, 20_000_000, 20_000_000)
        assertSame(FastOutSlowInEasing, motion.easing(-100f, 81_000_000))
        motion.reset()
        assertSame(FastOutSlowInEasing, motion.easing(-100f, 30_000_000))
        motion.sample(100f, 40_000_000, 40_000_000)
        motion.sample(110f, 60_000_000, 60_000_000)
        assertSame(FastOutSlowInEasing, motion.easing(-100f, 70_000_000))
    }
}
