package fr.deuspheara.oree.core.model

enum class ControlId {
    SETTINGS,
    WALLPAPER,
    NETWORK,
    AUDIO,
}

data class ControlSize(val columns: Int = 1, val rows: Int = 1) {
    fun normalized() = ControlSize(columns.coerceIn(1, 2), rows.coerceIn(1, 2))
}

data class ControlPosition(val column: Int = 0, val row: Int = 0) {
    fun normalized(size: ControlSize = ControlSize()) = ControlPosition(
        column.coerceIn(0, 4 - size.normalized().columns),
        row.coerceIn(0, 4 - size.normalized().rows),
    )
}

fun defaultControlSize(id: ControlId) = if (id == ControlId.NETWORK || id == ControlId.AUDIO) ControlSize(2, 1) else ControlSize()

data class ControlPlacement(val id: ControlId, val column: Int, val row: Int, val size: ControlSize)

/**
 * Bounded 4x4 packing. The edited card wins its cell; neighbours retain saved cells when possible.
 */
fun placeControls(
    ids: List<ControlId>,
    sizes: Map<ControlId, ControlSize>,
    positions: Map<ControlId, ControlPosition> = emptyMap(),
    pinned: ControlId? = null,
): List<ControlPlacement> {
    val order = ids.distinct().sortedBy { if (it == pinned) 0 else 1 }
    val occupied = mutableSetOf<Pair<Int, Int>>()
    val placed = mutableMapOf<ControlId, ControlPlacement>()
    fun pack(index: Int): Boolean {
        if (index == order.size) return true
        val id = order[index]
        val size = (sizes[id] ?: defaultControlSize(id)).normalized()
        val preferred = positions[id]?.normalized(size)
        val cells =
            (
                listOfNotNull(preferred) +
                    (0..3).flatMap { y -> (0..3).map { x -> ControlPosition(x, y) } }
                )
                .distinct()
        for (cell in cells) {
            if (cell.column + size.columns > 4 || cell.row + size.rows > 4) continue
            val covered =
                (cell.row until cell.row + size.rows).flatMap { y ->
                    (cell.column until cell.column + size.columns).map { x -> x to y }
                }
            if (covered.any { it in occupied }) continue
            occupied.addAll(covered)
            placed[id] = ControlPlacement(id, cell.column, cell.row, size)
            if (pack(index + 1)) return true
            occupied.removeAll(covered.toSet())
            placed.remove(id)
        }
        return false
    }
    check(pack(0)) // Four cards of at most 2x2 always fit in 4x4.
    return ids.distinct().map { placed.getValue(it) }
}
