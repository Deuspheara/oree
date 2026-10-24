package fr.deuspheara.oree.core.data

import fr.deuspheara.oree.core.platform.image.processing.blurArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperBlurTest {
    @Test
    fun runningSumsMatchClampedReferenceIncludingPerAxisRounding() {
        val random = kotlin.random.Random(240)
        for ((width, height) in listOf(1 to 1, 1 to 7, 9 to 1, 3 to 5, 17 to 11)) {
            for (radius in 1..8) {
                val source = IntArray(width * height) { random.nextInt() }
                val actual = source.copyOf()
                val horizontal = IntArray(source.size)
                val expected = IntArray(source.size)
                fun reference(input: IntArray, x: Int, y: Int, horizontalAxis: Boolean): Int {
                    var red = 0
                    var green = 0
                    var blue = 0
                    for (offset in -radius..radius) {
                        val xx = if (horizontalAxis) (x + offset).coerceIn(0, width - 1) else x
                        val yy = if (horizontalAxis) y else (y + offset).coerceIn(0, height - 1)
                        val color = input[yy * width + xx]
                        red += color shr 16 and 255
                        green += color shr 8 and 255
                        blue += color and 255
                    }
                    val count = radius * 2 + 1
                    return (255 shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
                }
                for (y in 0 until height) {
                    for (x in 0 until width) horizontal[y * width + x] = reference(source, x, y, true)
                }
                for (y in 0 until height) {
                    for (x in 0 until width) expected[y * width + x] = reference(horizontal, x, y, false)
                }
                blurArgb(actual, width, height, radius)
                org.junit.Assert.assertArrayEquals("${width}x$height radius=$radius", expected, actual)
            }
        }
    }

    @Test
    fun uniformColorAndSinglePixelStayStable() {
        val color = 0xff224466.toInt()
        val pixels = IntArray(15) { color }
        repeat(3) { blurArgb(pixels, 5, 3, 2) }
        assertTrue(pixels.all { it == color })
        val single = intArrayOf(color)
        blurArgb(single, 1, 1, 2)
        assertEquals(color, single.single())
    }

    @Test
    fun impulseSpreadsWithoutColorOrAlphaOverflow() {
        val pixels = IntArray(25) { 0xff000000.toInt() }
        pixels[12] = 0xffffffff.toInt()
        blurArgb(pixels, 5, 5, 1)
        assertEquals(0xff1c1c1c.toInt(), pixels[12])
        assertEquals(pixels[12], pixels[6])
        assertEquals(0xff000000.toInt(), pixels[0])
        assertTrue(pixels.all { it ushr 24 == 255 })
    }
}
