package fr.deuspheara.oree.core.model

/** Tiny artwork-derived lighting metadata; no pixels or Android objects are retained. */
data class ReflectionColors(val left: Int, val center: Int, val right: Int) {
    val highlight: Int =
        (0xff shl 24) or
            (
                ((left ushr 16 and 255) + 2 * (center ushr 16 and 255) + (right ushr 16 and 255)) /
                    4 shl 16
                ) or
            (
                ((left ushr 8 and 255) + 2 * (center ushr 8 and 255) + (right ushr 8 and 255)) / 4 shl
                    8
                ) or
            (((left and 255) + 2 * (center and 255) + (right and 255)) / 4)
}

/** Alpha-weighted region means avoid choosing transparent icon backgrounds as black. */
fun reflectionColors(sample: (column: Int, row: Int) -> Int): ReflectionColors? {
    val colors = IntArray(3)
    var visible = false
    for (region in 0..2) {
        var alpha = 0L
        var red = 0L
        var green = 0L
        var blue = 0L
        for (row in 0 until 6) {
            for (column in region * 8 until (region + 1) * 8) {
                val pixel = sample(column, row)
                val weight = pixel ushr 24
                alpha += weight
                red += (pixel ushr 16 and 255) * weight
                green += (pixel ushr 8 and 255) * weight
                blue += (pixel and 255) * weight
            }
        }
        if (alpha == 0L) {
            colors[region] = -1
        } else {
            visible = true
            colors[region] =
                (0xff shl 24) or
                ((red / alpha).toInt() shl 16) or
                ((green / alpha).toInt() shl 8) or
                (blue / alpha).toInt()
        }
    }
    return if (visible) ReflectionColors(colors[0], colors[1], colors[2]) else null
}
