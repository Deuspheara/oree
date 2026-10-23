package fr.deuspheara.oree.core.model

/** Relative visual workloads, not a claim about any particular TV's measured capacity. */
enum class VisualExperience(val motion: Float, val glass: Float, val blur: Float) {
    SIMPLE(0f, 0f, 0f),
    LIGHT(.55f, 0f, 0f),
    BALANCED(.75f, .45f, .5f),
    RICH(1f, .75f, .8f),
    FULL(1f, 1f, 1f),
    ;

    fun applyTo(value: LauncherPreferences): LauncherPreferences = value.copy(
        quality = if (this == SIMPLE) QualityTier.ECONOMY else QualityTier.BALANCED,
        reduceMotion = this == SIMPLE,
        reduceTransparency = glass == 0f,
        liquidGlass = glass > 0f,
        glassIntensity = glass.coerceAtLeast(.25f),
        backdropBlur = blur,
        motionIntensity = motion,
    )

    companion object {
        fun nearest(value: LauncherPreferences): VisualExperience = when {
            value.quality == QualityTier.ECONOMY || value.reduceMotion -> SIMPLE
            !value.liquidGlass || value.reduceTransparency -> LIGHT
            value.glassIntensity < .6f -> BALANCED
            value.glassIntensity < .9f -> RICH
            else -> FULL
        }
    }
}
