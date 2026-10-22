package fr.deuspheara.oree.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherProfileGenerator {
    @get:Rule val profile = BaselineProfileRule()

    @Test
    fun startup() = profile.collect(
        packageName = LAUNCHER_PACKAGE,
        includeInStartupProfile = true,
        strictStability = true,
        maxIterations = 30,
    ) {
        startLauncher()
    }

    @Test
    fun catalog() = profile.collect(
        packageName = LAUNCHER_PACKAGE,
        strictStability = true,
        maxIterations = 30,
    ) {
        startLauncher()
        prepareCatalogNavigation()
        settledContextSweep()
        profileVerticalArtworkSweep()
        catalogTour()
    }

    @Test
    fun secondaryScreens() = profile.collect(
        packageName = LAUNCHER_PACKAGE,
        strictStability = true,
        maxIterations = 30,
    ) {
        startLauncher()
        settingsAndCommandsJourney()
        withActiveFavorites { activeFavoritesTour() }
    }
}
