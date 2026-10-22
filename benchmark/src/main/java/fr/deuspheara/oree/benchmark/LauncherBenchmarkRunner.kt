package fr.deuspheara.oree.benchmark

import android.app.UiAutomation
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.uiautomator.Configurator

/** Benchmark under the user's actual accessibility environment, including HOME helpers. */
class LauncherBenchmarkRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle?) {
        Configurator.getInstance().uiAutomationFlags =
            UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        super.onCreate(arguments)
    }

    override fun getUiAutomation(): UiAutomation = super.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    override fun getUiAutomation(flags: Int): UiAutomation = super.getUiAutomation(
        if (flags and UiAutomation.FLAG_DONT_USE_ACCESSIBILITY != 0) {
            flags
        } else {
            flags or UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES
        },
    )
}
