package fr.deuspheara.oree

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.rememberHazeState
import fr.deuspheara.oree.core.designsystem.glass.LocalGlassSource
import fr.deuspheara.oree.core.designsystem.glass.LocalLiveGlass
import fr.deuspheara.oree.core.designsystem.glass.WallpaperBackground
import fr.deuspheara.oree.core.designsystem.theme.LauncherTheme
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.feature.home.presentation.screen.HomeScreen
import fr.deuspheara.oree.feature.home.presentation.state.HomeControl
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeUiState
import fr.deuspheara.oree.feature.profiles.presentation.screen.ProfilePanel
import fr.deuspheara.oree.feature.settings.presentation.screen.VisualExperienceScreen
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Documentation scenes use real screens, project-owned art and in-memory sample data only. */
class PublicScreenshotsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun captureProjectOnlyScenes() {
        var scene by mutableIntStateOf(0)
        val preferences = LauncherPreferences(liveGlass = true, glassIntensity = 1f)
        val profile = LocalProfile("sample", "Sample", preferences = preferences)
        val apps = listOf("Cinema", "Music", "Photos", "Radio", "Library", "Games").map {
            TvApp("sample.$it", "sample.$it", it, 0)
        }
        compose.setContent {
            val source = rememberHazeState()
            val wallpaper = remember {
                BitmapFactory.decodeResource(
                    InstrumentationRegistry.getInstrumentation().targetContext.resources,
                    fr.deuspheara.oree.core.platform.R.drawable.landscape_forest,
                ).asImageBitmap()
            }
            LauncherTheme(preferences) {
                CompositionLocalProvider(LocalGlassSource provides source, LocalLiveGlass provides true) {
                    Box(Modifier.fillMaxSize()) {
                        WallpaperBackground(wallpaper, reduced = true)
                        if (scene < 2) {
                            HomeScreen(
                                state = HomeUiState(apps, false),
                                resume = 0,
                                reduceMotion = true,
                                onAction = {},
                                favorites = apps.take(4).map { it.id },
                                profile = profile,
                                controls = setOf(HomeControl.SEARCH, HomeControl.COMMANDS, HomeControl.PROFILES),
                            )
                            if (scene == 1) {
                                ProfilePanel(ProfileSnapshot(profile.id, listOf(profile)), true, false, false, false, {
                                }, { _, _, _ -> }, {}, {})
                            }
                        } else {
                            VisualExperienceScreen(preferences, {}, welcome = false)
                        }
                    }
                }
            }
        }
        for ((index, name) in listOf("home", "profiles", "live-glass").withIndex()) {
            compose.runOnIdle { scene = index }
            compose.waitForIdle()
            android.os.SystemClock.sleep(800)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            try {
                File(instrumentation.targetContext.cacheDir, "public-$name.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } finally {
                bitmap.recycle()
            }
        }
    }
}
