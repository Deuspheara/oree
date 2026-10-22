package fr.deuspheara.oree.core.data

import fr.deuspheara.oree.core.platform.image.processing.blurArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperBlurTest {
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
