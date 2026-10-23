package fr.deuspheara.oree.feature.home.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.deuspheara.oree.core.designsystem.glass.GlassTreatment
import fr.deuspheara.oree.core.designsystem.glass.glassSurface

/** Live refractive glass follows its window position, without settled capture replacement. */
@Composable
internal fun DockSurface(modifier: Modifier = Modifier) {
    Box(
        modifier.testTag("liquid-dock-surface")
            .glassSurface(radius = 30.dp, treatment = GlassTreatment.CLEAR),
    )
}
