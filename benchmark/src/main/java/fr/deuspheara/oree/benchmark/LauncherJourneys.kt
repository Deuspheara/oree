package fr.deuspheara.oree.benchmark

import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

const val LAUNCHER_PACKAGE = "fr.deuspheara.oree"

/** Outside timing: allow the existing HOME handler and any redirect to settle. */
fun MacrobenchmarkScope.backgroundThroughHome() {
    device.executeShellCommand("input keyevent KEYCODE_WAKEUP")
    pressHome()
    val deadline = SystemClock.uptimeMillis() + 15_000
    var lastPackage: String? = null
    var stableSince = SystemClock.uptimeMillis()
    while (SystemClock.uptimeMillis() < deadline) {
        val current = device.currentPackageName
        if (current != lastPackage) {
            lastPackage = current
            stableSince = SystemClock.uptimeMillis()
        }
        if (
            current != null &&
            current != LAUNCHER_PACKAGE &&
            SystemClock.uptimeMillis() - stableSince >= 3_000
        ) {
            return
        }
        SystemClock.sleep(100)
    }
    error("Existing HOME handler did not settle outside the target package")
}

fun MacrobenchmarkScope.startLauncher(clearTask: Boolean = false) {
    // A normal wake input exits Dreaming too; am start alone can leave HOME hidden.
    // Never modify timeout/ambient policy or set KEEP_SCREEN_ON.
    device.executeShellCommand("input keyevent KEYCODE_WAKEUP")
    startActivityAndWait(
        Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(LAUNCHER_PACKAGE, "$LAUNCHER_PACKAGE.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (clearTask) addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        },
    )
    check(device.wait(Until.hasObject(By.res("home")), 5_000)) { "Home did not appear" }
    check(device.wait(Until.hasObject(By.res(java.util.regex.Pattern.compile("app:.*"))), 5_000)) {
        "Real catalog did not appear (reference benchmark requires installed TV applications)"
    }
    check(device.wait(Until.hasObject(By.res("home-ready")), 5_000)) {
        "Preferences/organization/catalog are not ready"
    }
    val focusDeadline = SystemClock.uptimeMillis() + 5_000
    var focusReady = false
    while (SystemClock.uptimeMillis() < focusDeadline) {
        val tag = focusedTag()
        if (tag?.startsWith("app:") == true || tag?.startsWith("content:") == true ||
            tag in setOf("overview", "search", "commands", "profiles", "choose-favorites")
        ) {
            focusReady = true
            break
        }
        SystemClock.sleep(50)
    }
    check(focusReady) { "Usable home focus did not appear" }
    device.waitForIdle()
}

fun MacrobenchmarkScope.catalogTour() {
    repeat(6) {
        repeat(4) { remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
        remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
        repeat(4) { remoteKey(KeyEvent.KEYCODE_DPAD_LEFT) }
        remoteKey(KeyEvent.KEYCODE_DPAD_UP)
    }
    device.waitForIdle(1_000)
}
