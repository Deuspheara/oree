@file:OptIn(dev.chrisbanes.haze.ExperimentalHazeApi::class)

package fr.deuspheara.oree.feature.settings.presentation.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.designsystem.component.LauncherIcon
import fr.deuspheara.oree.core.designsystem.component.LauncherSymbol
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassIntensity
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiquidGlass
import fr.deuspheara.oree.core.designsystem.glass.LocalOpaquePanels
import fr.deuspheara.oree.core.designsystem.glass.LocalReducedTransparency
import fr.deuspheara.oree.core.designsystem.glass.glassSurface
import fr.deuspheara.oree.core.designsystem.theme.DesignTokens
import fr.deuspheara.oree.core.designsystem.theme.accentColor
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier

@Composable
fun AccentSelector(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(6.dp),
    ) {
        items(AccentColor.entries, key = { it.name }) { accent ->
            var focused by remember { mutableStateOf(false) }
            val name =
                stringResource(
                    when (accent) {
                        AccentColor.BLUE -> R.string.accent_blue
                        AccentColor.PURPLE -> R.string.accent_purple
                        AccentColor.PINK -> R.string.accent_pink
                        AccentColor.RED -> R.string.accent_red
                        AccentColor.ORANGE -> R.string.accent_orange
                        AccentColor.GREEN -> R.string.accent_green
                    },
                )
            Button(
                onClick = { onSelect(accent) },
                modifier =
                    Modifier.size(42.dp)
                        .onFocusChanged { focused = it.isFocused }
                        .testTag("accent:${accent.name}")
                        .semantics {
                            contentDescription = name
                            this.selected = selected == accent
                        },
                shape = ButtonDefaults.shape(shape = CircleShape),
                contentPadding = PaddingValues(7.dp),
                scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
                border = ButtonDefaults.border(border = Border.None, focusedBorder = Border.None),
                colors =
                    ButtonDefaults.colors(
                        containerColor = Color.Transparent,
                        focusedContainerColor = DesignTokens.foreground.copy(alpha = .12f),
                    ),
            ) {
                Box(
                    Modifier.fillMaxSize()
                        .clip(CircleShape)
                        .background(accentColor(accent))
                        .border(if (focused) 3.dp else 0.dp, DesignTokens.foreground, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected == accent) Text("✓", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

/** Isolated source/effect: settings chrome stays opaque, only this sample renders glass. */
@Composable
fun GlassIntensityPreview(preferences: LauncherPreferences) {
    val source = rememberHazeState()
    val opaque =
        preferences.reduceTransparency ||
            preferences.highContrast ||
            preferences.quality == QualityTier.ECONOMY
    CompositionLocalProvider(
        LocalOpaquePanels provides false,
        LocalGlassSource provides source,
        LocalLiquidGlass provides preferences.liquidGlass,
        LocalGlassIntensity provides preferences.glassIntensity,
        LocalReducedTransparency provides opaque,
    ) {
        Box(
            Modifier.fillMaxWidth()
                .height(154.dp)
                .clip(RoundedCornerShape(18.dp))
                .testTag("glass-preview"),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(fr.deuspheara.oree.core.platform.R.drawable.landscape_forest),
                null,
                Modifier.fillMaxSize()
                    .then(
                        if (preferences.liquidGlass && !opaque) {
                            Modifier.hazeSource(source)
                        } else {
                            Modifier
                        },
                    ),
                contentScale = ContentScale.Crop,
            )
            Row(
                Modifier.glassSurface(radius = 28.dp).padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LauncherIcon(LauncherSymbol.NETWORK, DesignTokens.accent)
                Text(
                    stringResource(R.string.glass_preview),
                    color = DesignTokens.foreground,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
