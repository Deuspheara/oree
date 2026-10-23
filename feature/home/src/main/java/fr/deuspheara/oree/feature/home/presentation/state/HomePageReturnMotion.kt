package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/** A short-lived velocity sample for handing a moving page to its explicit header return. */
internal class HomePageReturnMotion {
    private var position: Float? = null
    private var frameNanos = 0L
    private var sampledAtNanos = 0L
    private var velocity = 0f

    fun reset() {
        position = null
        velocity = 0f
        sampledAtNanos = 0L
    }

    fun sample(next: Float?, frameTimeNanos: Long, nowNanos: Long) {
        val previous = position
        val elapsed = frameTimeNanos - frameNanos
        velocity = if (previous != null && next != null && elapsed in 1..100_000_000L) {
            (next - previous) * 1_000_000_000f / elapsed
        } else {
            0f
        }
        position = next
        frameNanos = frameTimeNanos
        sampledAtNanos = nowNanos
    }

    fun easing(distance: Float, nowNanos: Long): Easing {
        if (nowNanos - sampledAtNanos !in 0..60_000_000L || distance >= 0f || velocity >= 0f) {
            return FastOutSlowInEasing
        }
        // Hermite endpoints are 0 and 1; tangents are the carried speed and zero.
        // A short row reveal can be much faster than the page return. Carry a gentle
        // starting speed, capped below the average return speed, without overshoot.
        val slope = (velocity * .420f / distance).coerceIn(0f, .75f)
        if (slope < .1f) return FastOutSlowInEasing
        return Easing { fraction ->
            val t = fraction.coerceIn(0f, 1f)
            val remaining = 1f - t
            (1f - remaining * remaining * (1f + (2f - slope) * t)).coerceIn(0f, 1f)
        }
    }
}
