package fr.deuspheara.oree.core.platform.image.processing

/** Separable bounded box pass on a small wallpaper. Called on IO, never during drawing. */
fun blurArgb(pixels: IntArray, width: Int, height: Int, radius: Int) {
    require(width > 0 && height > 0 && pixels.size == width * height && radius in 1..8)
    val scratch = IntArray(pixels.size)
    fun average(x: Int, y: Int, horizontal: Boolean, source: IntArray): Int {
        var r = 0
        var g = 0
        var b = 0
        for (d in -radius..radius) {
            val xx = if (horizontal) (x + d).coerceIn(0, width - 1) else x
            val yy = if (horizontal) y else (y + d).coerceIn(0, height - 1)
            val c = source[yy * width + xx]
            r += (c shr 16) and 255
            g += (c shr 8) and 255
            b += c and 255
        }
        val n = radius * 2 + 1
        return (255 shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            scratch[y * width + x] = average(x, y, true, pixels)
        }
    }
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = average(x, y, false, scratch)
        }
    }
}
