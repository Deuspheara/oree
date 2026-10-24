package fr.deuspheara.oree.core.designsystem.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassOpticalMapTest {
    @Test
    fun rejectsInvalidAndOversizedInputsBeforeAllocation() {
        assertNull(GlassOpticalGeometry.create(0, 120, 26f, 1f))
        assertNull(GlassOpticalGeometry.create(Int.MAX_VALUE, Int.MAX_VALUE, 26f, 1f))
        assertNull(GlassOpticalGeometry.create(1600, 1400, 26f, 1f))
        assertNull(GlassOpticalGeometry.create(120, 120, Float.NaN, 1f))
        assertNull(GlassOpticalGeometry.create(120, 120, -1f, 1f))
        assertNull(GlassOpticalGeometry.create(120, 120, 26f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun wideDockKeepsANeutralCenterAndSmallOpaqueMap() {
        val geometry = checkNotNull(GlassOpticalGeometry.create(1920, 180, 39f, 1.5f))
        val pixels = bakeGlassOptics(geometry).argb
        assertTrue(geometry.mapWidth < 100)
        assertTrue(pixels.size * 4 < 32768)
        assertEquals(0xff808080.toInt(), pixels[(geometry.mapHeight / 2) * geometry.mapWidth + geometry.mapWidth / 2])
        assertTrue(pixels.all { it ushr 24 == 255 })
    }

    @Test
    fun lensDisplacementIsInwardAndMirrorSymmetric() {
        val geometry = checkNotNull(GlassOpticalGeometry.create(600, 180, 39f, 1.5f))
        val pixels = bakeGlassOptics(geometry).argb
        val w = geometry.mapWidth
        val h = geometry.mapHeight
        fun x(pixel: Int) = (pixel ushr 16 and 255) - 128
        fun y(pixel: Int) = (pixel ushr 8 and 255) - 128
        for (row in 0 until h) {
            for (column in 0 until w) {
                val pixel = pixels[row * w + column]
                assertEquals(-x(pixel), x(pixels[row * w + w - column - 1]))
                assertEquals(-y(pixel), y(pixels[(h - row - 1) * w + column]))
            }
        }
        assertTrue(x(pixels[(h / 2) * w + 3]) > 0)
        assertTrue(y(pixels[3 * w + w / 2]) > 0)
    }

    @Test
    fun inwardBilinearSamplesStayInsideUnpaddedMaterials() {
        val cases = listOf(
            GlassOpticalGeometry(1792, 222, 60f, 2f),
            GlassOpticalGeometry(128, 128, 52f, 2f),
            GlassOpticalGeometry(52, 52, 26f, 2f),
            GlassOpticalGeometry(80, 240, 39f, 1.5f),
            GlassOpticalGeometry(320, 80, 20f, .75f),
        )
        for (geometry in cases) {
            assertEquals(0f, geometry.inputPadding)
            val pixels = bakeGlassOptics(geometry).argb
            fun sample(x: Float, y: Float, shift: Int): Float {
                val texelX = x - .5f
                val texelY = y - .5f
                val left = kotlin.math.floor(texelX).toInt()
                val top = kotlin.math.floor(texelY).toInt()
                val fx = texelX - left
                val fy = texelY - top
                fun channel(column: Int, row: Int): Float = (
                    (
                        pixels[
                            row.coerceIn(0, geometry.mapHeight - 1) * geometry.mapWidth +
                                column.coerceIn(0, geometry.mapWidth - 1),
                        ] ushr shift
                        ) and 255
                    ).toFloat()
                val upper = channel(left, top) * (1f - fx) + channel(left + 1, top) * fx
                val lower = channel(left, top + 1) * (1f - fx) + channel(left + 1, top + 1) * fx
                return upper * (1f - fy) + lower * fy
            }
            for (y in 0 until geometry.height) {
                val py = y + .5f
                val edgeY = minOf(py, geometry.height - py, geometry.capY)
                val mapY = (if (py >= geometry.height * .5f) 2f * geometry.capY - edgeY else edgeY) *
                    geometry.mapHeight / (2f * geometry.capY)
                for (x in 0 until geometry.width) {
                    val px = x + .5f
                    val edgeX = minOf(px, geometry.width - px, geometry.capX)
                    val mapX = (if (px >= geometry.width * .5f) 2f * geometry.capX - edgeX else edgeX) *
                        geometry.mapWidth / (2f * geometry.capX)
                    val sampleX = px + (sample(mapX, mapY, 16) - 128f) * geometry.displacement / 127f
                    val sampleY = py + (sample(mapX, mapY, 8) - 128f) * geometry.displacement / 127f
                    assertTrue("Horizontal sample left material at $x,$y", sampleX >= .5f && sampleX <= geometry.width - .5f)
                    assertTrue("Vertical sample left material at $x,$y", sampleY >= .5f && sampleY <= geometry.height - .5f)
                }
            }
        }
        assertEquals(26f, GlassOpticalGeometry(40, 40, 20f, 2f).inputPadding)
    }

    @Test
    fun obsoleteGeometryBakeStopsAtARowBoundary() {
        val geometry = checkNotNull(GlassOpticalGeometry.create(600, 180, 26f, 1f))
        var rows = 0
        val cancelled = kotlinx.coroutines.CancellationException("obsolete geometry")
        try {
            bakeGlassOptics(geometry) { if (++rows == 3) throw cancelled }
            throw AssertionError("Bake must be cancellable")
        } catch (failure: kotlinx.coroutines.CancellationException) {
            assertTrue(failure === cancelled)
            assertEquals(3, rows)
        }
    }
}
