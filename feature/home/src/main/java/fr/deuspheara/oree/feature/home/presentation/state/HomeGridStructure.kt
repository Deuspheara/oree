package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.ui.unit.IntSize
import fr.deuspheara.oree.core.designsystem.theme.MotionTokens
import kotlin.math.ceil

/** Bucket native pixels including maximum focus zoom, within the existing decode cap. */
fun homeArtworkSize(size: IntSize): IntSize {
    if (size.width <= 0 || size.height <= 0) return IntSize.Zero
    val width = (ceil(size.width.toDouble() * MotionTokens.FOCUS_SCALE).toLong() + 63) / 64 * 64
    val bounded = width.coerceIn(64, 480).toInt()
    return IntSize(bounded, bounded * 9 / 16)
}

/** Real lazy-item index plus logical row edges; headings/status occupy full lines. */
data class HomeGridSlot(val itemIndex: Int, val atStart: Boolean, val atEnd: Boolean)

fun homeGridSlots(ids: List<String>, favoriteCount: Int, columns: Int, statusRow: Boolean): Map<String, HomeGridSlot> {
    require(columns >= 1 && favoriteCount in 0..ids.size)
    return ids.mapIndexed { index, id ->
        val inFavorites = index < favoriteCount
        val sectionIndex =
            if (inFavorites || favoriteCount == 0) index else index - favoriteCount
        val sectionSize = if (inFavorites) favoriteCount else ids.size - favoriteCount
        val rowStart = sectionIndex / columns * columns
        val rowSize = minOf(columns, sectionSize - rowStart)
        val column = sectionIndex % columns
        id to
            HomeGridSlot(
                itemIndex =
                    index +
                        1 +
                        (if (statusRow) 1 else 0) +
                        (if (favoriteCount > 0 && !inFavorites) 1 else 0),
                atStart = column == 0,
                atEnd = column == rowSize - 1,
            )
    }
        .toMap()
}
