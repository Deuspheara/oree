package fr.deuspheara.oree.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/** Same APK/cold/readiness path as coldStartup, with ART compilation reset to None. */
class LauncherProfileComparison {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test
    fun coldStartupWithoutBaselineCompilation() = benchmark.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        startLauncher()
    }

    @Test
    fun warmCatalogWithoutBaselineCompilation() = benchmark.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 3,
        setupBlock = {
            startLauncher(clearTask = true)
            prepareCatalogNavigation()
        },
    ) {
        catalogTour()
    }
}
