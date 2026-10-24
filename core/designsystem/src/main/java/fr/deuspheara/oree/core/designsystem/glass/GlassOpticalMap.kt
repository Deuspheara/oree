package fr.deuspheara.oree.core.designsystem.glass

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Geometry only: scrolling, source pixels, opacity and intensity never change the map. */
internal data class GlassOpticalGeometry(val width: Int, val height: Int, val radius: Float, val density: Float) {
    val band = min(28f * density, min(width, height) * .5f)
    val capX = min(width * .5f, radius + band + 2f)
    val capY = min(height * .5f, radius + band + 2f)
    private val scale = min(.5f, 256f / (2f * max(capX, capY)))
    val mapWidth = ceil(2f * capX * scale).toInt().coerceAtLeast(2)
    val mapHeight = ceil(2f * capY * scale).toInt().coerceAtLeast(2)
    val displacement = 12f * density

    // The field bends inward. When a half extent contains its maximum displacement,
    // sampling cannot cross the opposite edge, so no outside input support is needed.
    val inputPadding: Float
        get() = if (min(width, height) >= 2f * (displacement + 2f)) 0f else ceil(displacement + 2f)

    // One map texel of support keeps bilinear filtering outside the non-neutral edge band.
    val neutralInset = band + max(2f * capX / mapWidth, 2f * capY / mapHeight)

    companion object {
        fun create(width: Int, height: Int, radius: Float, density: Float): GlassOpticalGeometry? {
            if (width <= 0 || height <= 0 || !radius.isFinite() || radius < 0f || !density.isFinite() || density <= 0f) return null
            val padding = ceil(12f * density + 2f).toLong()
            val rasterWidth = width.toLong() + padding * 2
            val rasterHeight = height.toLong() + padding * 2
            if (rasterWidth > 4096 || rasterHeight > 4096 || rasterWidth * rasterHeight > 2_097_152L) return null
            return GlassOpticalGeometry(width, height, min(radius, min(width, height) * .5f), density)
        }
    }
}

internal data class GlassOpticalPixels(val geometry: GlassOpticalGeometry, val argb: IntArray)

/** A small size-independent Snell/Schlick curve, evaluated once, never in the fragment shader. */
private object RefractionProfile {
    private val values = FloatArray(65) { index ->
        val t = index / 64f
        val u = 1f - t
        val height = sqrt(max(0f, 1f - u * u))
        val slope = u / max(height, .0001f)
        val cosIncident = 1f / sqrt(1f + slope * slope)
        val sinIncident = slope * cosIncident
        val sinTransmitted = sinIncident / 1.5f
        val cosTransmitted = sqrt(1f - sinTransmitted * sinTransmitted)
        val tangent = (sinIncident * cosTransmitted - cosIncident * sinTransmitted) /
            (cosIncident * cosTransmitted + sinIncident * sinTransmitted)
        val grazing = 1f - cosIncident
        val reflectance = .04f + .96f * grazing * grazing * grazing * grazing * grazing
        (.15f + .35f * height) * tangent * (1f - reflectance)
    }.let { curve ->
        val peak = curve.max()
        curve.apply { for (i in indices) this[i] /= peak }
    }

    fun sample(t: Float): Float {
        val position = t.coerceIn(0f, 1f) * 64f
        val low = position.toInt()
        return values[low] + (values[min(64, low + 1)] - values[low]) * (position - low)
    }
}

/**
 * Bakes a compressed nine-patch field. Only the optically neutral interior is collapsed;
 * corners and edge widths keep physical coordinates. RG = signed bend, B = signed rim light.
 * Alpha is always one: numeric channels must be sampled as a raw, linearly filtered buffer.
 */
internal fun bakeGlassOptics(geometry: GlassOpticalGeometry, ensureActive: () -> Unit = {}): GlassOpticalPixels {
    val width = geometry.mapWidth
    val height = geometry.mapHeight
    val pixels = IntArray(width * height)
    val virtualWidth = geometry.capX * 2f
    val virtualHeight = geometry.capY * 2f
    for (y in 0 until height) {
        ensureActive()
        val py = (y + .5f) * virtualHeight / height
        for (x in 0 until width) {
            val px = (x + .5f) * virtualWidth / width
            val qx = abs(px - geometry.capX) - (geometry.capX - geometry.radius)
            val qy = abs(py - geometry.capY) - (geometry.capY - geometry.radius)
            val ox = max(qx, 0f)
            val oy = max(qy, 0f)
            val outside = sqrt(ox * ox + oy * oy)
            val distance = max(0f, geometry.radius - outside - min(max(qx, qy), 0f))
            val sx = if (px < geometry.capX) -1f else 1f
            val sy = if (py < geometry.capY) -1f else 1f
            val nx = if (outside > .0001f) {
                sx * ox / outside
            } else if (qx > qy) {
                sx
            } else {
                0f
            }
            val ny = if (outside > .0001f) {
                sy * oy / outside
            } else if (qx > qy) {
                0f
            } else {
                sy
            }
            val bend = RefractionProfile.sample(distance / geometry.band)
            // A fixed upper-left key and a weaker opposite catch; no idle light animation.
            val facing = (-nx * .6f - ny * .8f).coerceAtLeast(0f)
            val opposite = (nx * .6f + ny * .8f).coerceAtLeast(0f)
            val rim = exp(-distance / (1.25f * geometry.density))
            val glow = exp(-distance / (4f * geometry.density))
            val light = if (distance >= geometry.band) {
                0f
            } else {
                (.5f * rim + .1f * glow) * (.2f + .8f * facing) + .12f * rim * opposite - .06f * glow * opposite
            }
            pixels[y * width + x] = (0xff shl 24) or (encode(-nx * bend) shl 16) or (encode(-ny * bend) shl 8) or encode(light)
        }
    }
    return GlassOpticalPixels(geometry, pixels)
}

private fun encode(value: Float): Int = (128f + 127f * value.coerceIn(-1f, 1f)).roundToInt().coerceIn(1, 255)
