package fr.deuspheara.oree.core.designsystem.glass

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens

val LocalSettingsPage = androidx.compose.runtime.compositionLocalOf { false }

/** Native modal focus/Back, without dimming a scene that Haze samples before window composition. */
@Composable
fun GlassDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    val page = LocalSettingsPage.current
    val density = LocalDensity.current
    val resources = LocalResources.current
    val direction = LocalLayoutDirection.current
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
        }
        if (page) {
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalResources provides resources,
                LocalLayoutDirection provides direction,
            ) {
                Box(Modifier.fillMaxSize().background(DesignTokens.background)) { content() }
            }
        } else {
            content()
        }
    }
}
