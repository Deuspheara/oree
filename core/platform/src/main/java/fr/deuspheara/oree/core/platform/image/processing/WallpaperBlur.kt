package fr.deuspheara.oree.core.platform.image.processing

/** Separable running-sum box pass. Keeps clamped edges and integer rounding after each axis. */
fun blurArgb(pixels: IntArray, width: Int, height: Int, radius: Int) {
    require(width > 0 && height > 0 && pixels.size.toLong() == width.toLong() * height && radius in 1..8)
    val scratch = IntArray(pixels.size)
    val count = radius * 2 + 1
    fun pass(source: IntArray, target: IntArray, lines: Int, length: Int, stride: Int, lineStride: Int) {
        for (line in 0 until lines) {
            val base = line * lineStride
            var red = 0
            var green = 0
            var blue = 0
            for (offset in -radius..radius) {
                val color = source[base + offset.coerceIn(0, length - 1) * stride]
                red += color shr 16 and 255
                green += color shr 8 and 255
                blue += color and 255
            }
            for (position in 0 until length) {
                target[base + position * stride] =
                    (255 shl 24) or ((red / count) shl 16) or ((green / count) shl 8) or (blue / count)
                if (position == length - 1) continue
                val leaving = source[base + (position - radius).coerceAtLeast(0) * stride]
                val entering = source[base + (position + radius + 1).coerceAtMost(length - 1) * stride]
                red += (entering shr 16 and 255) - (leaving shr 16 and 255)
                green += (entering shr 8 and 255) - (leaving shr 8 and 255)
                blue += (entering and 255) - (leaving and 255)
            }
        }
    }
    pass(pixels, scratch, height, width, 1, width)
    pass(scratch, pixels, width, height, width, 1)
}
