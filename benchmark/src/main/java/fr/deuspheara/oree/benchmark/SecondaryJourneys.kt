package fr.deuspheara.oree.benchmark

import android.app.UiAutomation
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.File

/** Ordinary remote events; no touch/accessibility action substitutes for D-pad activation. */
fun remoteKey(code: Int, longPress: Boolean = false, pauseMillis: Long = 120) {
    val automation =
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    val down = SystemClock.uptimeMillis()
    fun inject(action: Int, repeat: Int = 0, flags: Int = 0) {
        check(
            automation.injectInputEvent(
                KeyEvent(
                    down,
                    SystemClock.uptimeMillis(),
                    action,
                    code,
                    repeat,
                    0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    flags,
                    InputDevice.SOURCE_DPAD,
                ),
                true,
            ),
        )
    }
    inject(KeyEvent.ACTION_DOWN)
    if (longPress) {
        SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong() + 100)
        inject(KeyEvent.ACTION_DOWN, 1, KeyEvent.FLAG_LONG_PRESS)
    }
    inject(KeyEvent.ACTION_UP)
    SystemClock.sleep(pauseMillis)
}

private fun MacrobenchmarkScope.visible(tag: String): UiObject2 =
    checkNotNull(device.wait(Until.findObject(By.res(tag)), 5_000)) { "Missing $tag" }

private fun MacrobenchmarkScope.focusedApp(): String? = focusedTag()?.takeIf { it.startsWith("app:") }

private fun MacrobenchmarkScope.awaitFocusedApp(): String {
    val deadline = SystemClock.uptimeMillis() + 5_000
    do {
        try {
            focusedApp()?.let { return it }
        } catch (
            _: androidx.test.uiautomator.StaleObjectException,
        ) { /* Reacquire on the next bounded attempt. */ }
        SystemClock.sleep(50)
    } while (SystemClock.uptimeMillis() < deadline)
    runCatching {
        val context = InstrumentationRegistry.getInstrumentation().context
        val directory =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: File(
                    context.getExternalFilesDir(null) ?: context.cacheDir,
                    "journey-diagnostics",
                )
        directory.mkdirs()
        device.takeScreenshot(File(directory, "secondary-focus-failure.png"))
        device.dumpWindowHierarchy(File(directory, "secondary-focus-failure.xml"))
    }
        .onFailure { Log.w("LauncherJourneys", "Focus diagnostics could not be saved", it) }
    error(
        "No app focus; focused IDs=" + device.findObjects(By.focused(true)).map { it.resourceName },
    )
}

/** Bounded fresh hierarchy traversal; never infer input focus from a cached selector or pixels. */
private fun MacrobenchmarkScope.focusedTag(): String? {
    val automation = InstrumentationRegistry.getInstrumentation()
        .getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
    val pending = java.util.ArrayDeque<android.view.accessibility.AccessibilityNodeInfo>()
    automation.rootInActiveWindow?.let(pending::add)
    var visited = 0
    fun release(node: android.view.accessibility.AccessibilityNodeInfo) {
        if (android.os.Build.VERSION.SDK_INT < 33) node.recycle()
    }
    try {
        while (pending.isNotEmpty() && visited++ < 1_000) {
            val node = pending.removeFirst()
            try {
                if (!node.refresh()) continue
                if (node.isFocused) return node.viewIdResourceName
                repeat(node.childCount) { index -> node.getChild(index)?.let(pending::add) }
            } finally {
                release(node)
            }
        }
        return null
    } finally {
        while (pending.isNotEmpty()) release(pending.removeFirst())
    }
}

private fun MacrobenchmarkScope.waitForFocus(tags: Set<String>, timeout: Long): String? {
    val deadline = SystemClock.uptimeMillis() + timeout
    do {
        focusedTag()?.takeIf { it in tags }?.let { return it }
        SystemClock.sleep(50)
    } while (SystemClock.uptimeMillis() < deadline)
    return null
}

private fun MacrobenchmarkScope.awaitFocused(tag: String) {
    if (waitForFocus(setOf(tag), 5_000) == null) {
        val context = InstrumentationRegistry.getInstrumentation().context
        val directory = File(context.getExternalFilesDir(null), "journey-diagnostics").apply { mkdirs() }
        device.dumpWindowHierarchy(File(directory, "await-focus-failure.xml"))
        device.takeScreenshot(File(directory, "await-focus-failure.png"))
        error("$tag did not receive focus")
    }
}

private fun MacrobenchmarkScope.activate(tag: String) {
    awaitFocused(tag) // Never send OK to an unverified target.
    remoteKey(KeyEvent.KEYCODE_DPAD_CENTER)
}

fun MacrobenchmarkScope.returnToLauncherHome() {
    repeat(4) {
        val layer =
            listOf("app-menu", "command-panel", "launcher-settings").firstOrNull {
                device.hasObject(By.res(it))
            }
        if (layer == null) {
            visible("home")
            remoteKey(KeyEvent.KEYCODE_BACK) // End Edit apps if active; normal HOME consumes Back.
            device.waitForIdle(1_000)
            return
        }
        remoteKey(KeyEvent.KEYCODE_BACK)
        check(device.wait(Until.gone(By.res(layer)), 5_000)) { "$layer did not close" }
    }
    error("Launcher layers did not close")
}

private fun MacrobenchmarkScope.rowEdge(key: Int) {
    repeat(20) {
        val before = focusedTag()
        remoteKey(key)
        device.waitForIdle(1_000)
        if (focusedTag() == before) return
    }
    error("Row edge not reached within bounded navigation")
}

private fun MacrobenchmarkScope.header(tag: String) {
    returnToLauncherHome()
    val controls = setOf("search", "commands", "profiles")
    var reached = false
    for (step in 0 until 80) {
        if (focusedTag() in controls) {
            reached = true
            break
        }
        remoteKey(KeyEvent.KEYCODE_DPAD_UP)
        waitForFocus(controls, 250)
    }
    if (!reached) {
        val context = InstrumentationRegistry.getInstrumentation().context
        val directory = File(context.getExternalFilesDir(null), "journey-diagnostics").apply { mkdirs() }
        device.dumpWindowHierarchy(File(directory, "header-focus-failure.xml"))
        device.takeScreenshot(File(directory, "header-focus-failure.png"))
        error("Home header not reached; foreground=${device.currentPackageName}; focused=${focusedTag()}")
    }
    rowEdge(KeyEvent.KEYCODE_DPAD_LEFT)
    repeat(4) {
        if (focusedTag() == tag) return
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        if (waitForFocus(setOf(tag), 1_000) != null) return
    }
    error("Header control $tag not reached; focused=${focusedTag()}")
}

/** A repeatable top-row anchor, outside navigation timing. No layout/preferences reset. */
fun MacrobenchmarkScope.prepareCatalogNavigation() {
    header("commands")
    remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
    // An empty dock exposes its real action before the remaining catalog.
    if (focusedTag() == "choose-favorites") remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
    awaitFocusedApp()
    rowEdge(KeyEvent.KEYCODE_DPAD_LEFT)
    Log.i("LauncherJourneys", "Catalog anchor:${awaitFocusedApp()}")
}

/** Include deliberate settled previews as well as fast input in collection. */
fun MacrobenchmarkScope.settledContextSweep() {
    repeat(3) { remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT, pauseMillis = 350) }
    repeat(3) { remoteKey(KeyEvent.KEYCODE_DPAD_LEFT, pauseMillis = 350) }
}

/**
 * Profile-only coverage of scrolling, offscreen material and artwork reentry. Six rows cover the
 * reference catalog; this is not the legacy timed tour.
 */
fun MacrobenchmarkScope.profileVerticalArtworkSweep() {
    prepareCatalogNavigation()
    repeat(2) {
        repeat(6) { remoteKey(KeyEvent.KEYCODE_DPAD_DOWN) }
        repeat(6) { remoteKey(KeyEvent.KEYCODE_DPAD_UP) }
    }
    prepareCatalogNavigation()
}

fun MacrobenchmarkScope.settingsAndCommandsJourney() {
    header("commands")
    activate("commands")
    visible("command-panel")
    awaitFocused("command-settings")
    activate("command-settings")
    visible("launcher-settings")
    awaitFocused("settings-category:APPEARANCE")
    remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT)
    // Preferences now save immediately. Collect focus/layout without activating a value.
    awaitFocused("settings-theme")
    remoteKey(KeyEvent.KEYCODE_BACK)
    check(device.wait(Until.gone(By.res("launcher-settings")), 5_000))
    awaitFocused("commands")
    header("commands")
    activate("commands")
    visible("command-panel")
    awaitFocused("command-settings")
    remoteKey(KeyEvent.KEYCODE_BACK)
    check(device.wait(Until.gone(By.res("command-panel")), 5_000))
    awaitFocused("commands")
}

private fun MacrobenchmarkScope.focusApplication(tag: String) {
    if (focusedApp() == tag) return
    prepareCatalogNavigation()
    for (row in 0 until 80) {
        checkNotNull(focusedApp()) { "No application focused" }
        rowEdge(KeyEvent.KEYCODE_DPAD_LEFT)
        for (column in 0 until 20) {
            if (focusedApp() == tag) return
            val before = focusedTag()
            remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT)
            if (focusedTag() == before) break
        }
        val last = focusedTag()
        remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
        if (focusedTag() == last) break
    }
    error("Application vanished or navigation could not reach $tag")
}

private fun MacrobenchmarkScope.openApplicationMenu(tag: String) {
    focusApplication(tag)
    remoteKey(KeyEvent.KEYCODE_DPAD_CENTER, longPress = true)
    visible("app-menu")
    awaitFocused("menu-open")
}

private fun MacrobenchmarkScope.favoriteState(): Boolean {
    val add = device.hasObject(By.res("menu-favorite-add"))
    val remove = device.hasObject(By.res("menu-favorite-remove"))
    check(add xor remove) { "Favorite state is unavailable/ambiguous" }
    return remove
}

private fun MacrobenchmarkScope.closeMenu() {
    remoteKey(KeyEvent.KEYCODE_BACK)
    check(device.wait(Until.gone(By.res("app-menu")), 5_000))
}

private fun MacrobenchmarkScope.changeFavorite(favorite: Boolean) {
    check(favoriteState() != favorite)
    remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
    activate(if (favorite) "menu-favorite-add" else "menu-favorite-remove")
    check(device.wait(Until.gone(By.res("app-menu")), 5_000)) {
        "Favorite write failed or menu did not close"
    }
}

/**
 * Test-owned fixture. Existing memberships/order are never removed/re-added. New additions are
 * removed in reverse order through the real UI, even on failure.
 */
class ActiveFavoritesFixture
internal constructor(
    private val scope: MacrobenchmarkScope,
    private val original: LinkedHashMap<String, Boolean>,
) : AutoCloseable {
    fun positionForNavigation() = with(scope) { focusApplication(original.keys.last()) }

    override fun close() = with(scope) {
        startLauncher()
        returnToLauncherHome()
        for ((tag, wasFavorite) in original.entries.reversed()) {
            openApplicationMenu(tag)
            if (favoriteState() != wasFavorite) changeFavorite(wasFavorite) else closeMenu()
            openApplicationMenu(tag)
            check(favoriteState() == wasFavorite) {
                "Original favorite membership not restored for $tag"
            }
            closeMenu()
        }
    }
}

/** UI-only setup; requires two real TV apps. No file replacement,pm clear or HOME actions. */
fun MacrobenchmarkScope.prepareActiveFavorites(): ActiveFavoritesFixture {
    returnToLauncherHome()
    if (focusedApp() == null) {
        prepareCatalogNavigation()
    }
    val original = linkedMapOf<String, Boolean>()
    val fixture = ActiveFavoritesFixture(this, original)
    try {
        repeat(2) {
            val candidate = awaitFocusedApp()
            Log.i("LauncherJourneys", "Prepare favorite${original.size}:$candidate")
            check(candidate !in original) { "Fixture requires at least two distinct TV apps" }
            openApplicationMenu(candidate)
            val wasFavorite = favoriteState()
            original[candidate] = wasFavorite
            if (!wasFavorite) changeFavorite(true) else closeMenu()
            awaitFocused(candidate)
            if (original.size == 1) {
                remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT)
                device.waitForIdle(1_000)
                if (awaitFocusedApp() == candidate) {
                    remoteKey(KeyEvent.KEYCODE_DPAD_DOWN)
                    awaitFocusedApp()
                }
            }
        }
        Log.i(
            "LauncherJourneys",
            "Fixture original existing favorites:${original.values.count { it }}",
        )
        return fixture
    } catch (failure: Throwable) {
        try {
            fixture.close()
        } catch (cleanup: Throwable) {
            failure.addSuppressed(cleanup)
        }
        throw failure
    }
}

fun MacrobenchmarkScope.withActiveFavorites(block: MacrobenchmarkScope.() -> Unit) {
    prepareActiveFavorites().use { block() }
}

fun MacrobenchmarkScope.activeFavoritesTour() {
    checkNotNull(focusedApp())
    repeat(6) {
        for (
        key in
        listOf(
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
        )
        ) {
            remoteKey(key)
        }
    }
    checkNotNull(focusedApp()) { "Favorites tour lost app focus" }
    device.waitForIdle(1_000)
}
