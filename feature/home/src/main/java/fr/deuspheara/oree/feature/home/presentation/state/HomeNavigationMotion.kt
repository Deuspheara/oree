package fr.deuspheara.oree.feature.home.presentation.state

import androidx.compose.ui.geometry.Offset

/** Local directional impulse; native focus never enters shared business state. */
class HomeNavigationMotion {
    var direction = Offset.Zero
}
