package fr.deuspheara.oree.core.model

enum class TvInputConnection {
    CONNECTED,
    STANDBY,
    DISCONNECTED,
    UNKNOWN,
}

data class TvInput(val id: String, val label: String, val connection: TvInputConnection)

data class TvInputs(val entries: List<TvInput> = emptyList(), val unavailable: Boolean = false)

fun restoreInputId(previous: String?, previousIndex: Int, inputs: List<TvInput>): String? = previous?.takeIf { id ->
    inputs.any { it.id == id }
} ?: inputs.getOrNull(previousIndex.coerceIn(0, (inputs.size - 1).coerceAtLeast(0)))?.id
