package fr.deuspheara.oree.core.designsystem.artwork

import androidx.compose.runtime.staticCompositionLocalOf
import fr.deuspheara.oree.core.domain.rendering.ArtworkPalette

private val emptyPalette = ArtworkPalette { null }
val LocalArtworkReflections = staticCompositionLocalOf<ArtworkPalette> { emptyPalette }
