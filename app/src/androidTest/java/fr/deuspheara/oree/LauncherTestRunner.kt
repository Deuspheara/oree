package fr.deuspheara.oree

import android.app.UiAutomation
import android.os.ParcelFileDescriptor
import androidx.test.runner.AndroidJUnitRunner

/** Test remote behavior with existing user accessibility services still running. */
class LauncherTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        // Normal remote wake exits Ambient Mode; do not alter timeout/energy policy.
        ParcelFileDescriptor.AutoCloseInputStream(
            getUiAutomation().executeShellCommand("input keyevent KEYCODE_WAKEUP"),
        )
            .use { it.readBytes() }
        super.onStart()
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
