package fr.deuspheara.oree.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/** Functional gate for the actual optimized app; one sample is not a performance result. */
class LauncherJourneySmokeTests {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @OptIn(ExperimentalMacrobenchmarkApi::class)
    @Test
    fun secondaryJourneysRestoreTheirState() = benchmark.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode =
            CompilationMode.Ignore(), // Functional gate preserves installed compilation.
        iterations = 1,
        setupBlock = { startLauncher() },
    ) {
        // Exercise profile-only scrolling before spending time on strict collection.
        profileVerticalArtworkSweep()
        settingsAndCommandsJourney()
        withActiveFavorites { activeFavoritesTour() }
    }
}
