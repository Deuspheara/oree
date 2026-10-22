package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.ReflectionColors
import fr.deuspheara.oree.core.model.reflectionColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReflectionColorsTest {
    @Test
    fun regionsRetainTheirColorsAndIgnoreTransparentBackgrounds() {
        val colors = reflectionColors { x, y ->
            if (y == 0) {
                0
            } else {
                when (x / 8) {
                    0 -> 0xffff0000.toInt()
                    1 -> 0xff00ff00.toInt()
                    else -> 0xff0000ff.toInt()
                }
            }
        }
        assertEquals(
            ReflectionColors(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt()),
            colors,
        )
        assertEquals(0xff3f7f3f.toInt(), colors!!.highlight)
    }

    @Test
    fun transparentArtworkHasNoTint() {
        assertNull(reflectionColors { _, _ -> 0 })
    }
}
