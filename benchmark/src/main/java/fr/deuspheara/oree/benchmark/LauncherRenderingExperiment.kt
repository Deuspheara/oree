package fr.deuspheara.oree.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Rule
import org.junit.Test

/** Diagnostic A/B only: Full compilation removes changed profile coverage as a confound. */
class LauncherRenderingExperiment {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test
    fun catalogFullCompilation() = benchmark.measureRepeated(
        packageName = LAUNCHER_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Full(),
        iterations = 3,
        setupBlock = {
            startLauncher(clearTask = true)
            prepareCatalogNavigation()
        },
    ) {
        catalogTour()
    }

    @Test
    fun favoritesFullCompilation() {
        var fixture: ActiveFavoritesFixture? = null
        try {
            benchmark.measureRepeated(
                packageName = LAUNCHER_PACKAGE,
                metrics = listOf(FrameTimingMetric()),
                compilationMode = CompilationMode.Full(),
                iterations = 3,
                setupBlock = {
                    startLauncher(clearTask = true)
                    if (fixture == null) fixture = prepareActiveFavorites()
                    fixture?.positionForNavigation()
                },
            ) {
                activeFavoritesTour()
            }
        } finally {
            fixture?.close()
        }
    }
}
