package fr.deuspheara.oree.core.model

enum class AccentColor {
    BLUE,
    PURPLE,
    PINK,
    RED,
    ORANGE,
    GREEN,
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

enum class QualityTier {
    ECONOMY,
    BALANCED,
}

enum class CatalogSort {
    ALPHABETICAL,
    RECENT,
}

enum class HomeContentLayout { BELOW_DOCK, ABOVE_DOCK }

/** Small, bounded preferences. Layout/profile relations have separate ownership. */
data class LauncherPreferences(
    val theme: ThemeMode = ThemeMode.DARK,
    val quality: QualityTier = QualityTier.BALANCED,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val highContrast: Boolean = false,
    val textScale: Float = 1f,
    val showSideloaded: Boolean = false,
    val showContext: Boolean = true,
    val sort: CatalogSort = CatalogSort.ALPHABETICAL,
    val liquidGlass: Boolean = true,
    val glassIntensity: Float = .75f,
    val accent: AccentColor = AccentColor.BLUE,
    val controls: Map<ControlId, ControlSize> = emptyMap(),
    val controlPositions: Map<ControlId, ControlPosition> = emptyMap(),
    val homeContentLayout: HomeContentLayout = HomeContentLayout.BELOW_DOCK,
    val backdropBlur: Float = 1f,
    val motionIntensity: Float = 1f,
    val setupComplete: Boolean = false,
) {
    fun normalized(): LauncherPreferences = copy(
        motionIntensity = if (motionIntensity.isFinite()) motionIntensity.coerceIn(0f, 1f) else 1f,
        backdropBlur = if (backdropBlur.isFinite()) backdropBlur.coerceIn(0f, 1f) else 1f,
        controlPositions =
            controlPositions.mapValues { (id, position) ->
                position.normalized(controls[id] ?: defaultControlSize(id))
            },
        controls = controls.mapValues { it.value.normalized() },
        glassIntensity =
            if (glassIntensity.isFinite()) glassIntensity.coerceIn(.25f, 1f) else .75f,
        textScale = if (textScale.isFinite()) textScale.coerceIn(1f, 2f) else 1f,
    )
}
