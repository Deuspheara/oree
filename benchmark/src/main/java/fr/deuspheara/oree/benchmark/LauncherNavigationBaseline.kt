package fr.deuspheara.oree.benchmark

import android.app.UiAutomation
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.PerfettoConfig
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import java.io.File
import java.util.regex.Pattern
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Configured-device journeys: no preferences, favorites, HOME or cold-cache manipulation. */
@OptIn(
    androidx.benchmark.ExperimentalBenchmarkConfigApi::class,
    androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi::class,
)
class LauncherNavigationBaseline {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Before
    fun requireNonDestructiveCompilationReset() {
        check(Build.VERSION.SDK_INT >= 34) {
            "Pinned Macrobenchmark may uninstall its target below API 34; use a disposable fixture there"
        }
    }

    @Test
    fun continuationVisible() = measure("continuation-visible", continuation = true, traversal = false)

    @Test
    fun continuationTraversal() = measure("continuation-traversal", continuation = true, traversal = true)

    @Test
    fun gridVisible() = measure("grid-visible", continuation = false, traversal = false)

    @Test
    fun gridTraversal() = measure("grid-traversal", continuation = false, traversal = true)

    @Test
    fun continuationStablePair() = measure("continuation-stable-pair", continuation = true, traversal = false, stablePair = true)

    @Test
    fun fullPageFirst() = measureFullPage(warmed = false)

    @Test
    fun fullPageWarm() = measureFullPage(warmed = true)

    @Test
    fun fullPageRapidWarm() = measureFullPage(warmed = true, periodMillis = 180)

    private fun measureFullPage(warmed: Boolean, periodMillis: Long = 500) {
        val tour = List(6) { KeyEvent.KEYCODE_DPAD_DOWN } + List(6) { KeyEvent.KEYCODE_DPAD_UP }
        val records = mutableListOf<NavigationSample>()
        var prepared: NavigationSample? = null
        val traceConfig = InstrumentationRegistry.getInstrumentation().context.assets
            .open("navigation-cpu.pbtxt").bufferedReader().use { it.readText() }
        try {
            benchmark.measureRepeated(
                packageName = LAUNCHER_PACKAGE,
                metrics = listOf(FrameTimingMetric()),
                perfettoConfig = PerfettoConfig.Text(traceConfig),
                compilationMode = CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Require,
                    warmupIterations = 3,
                ),
                iterations = 3,
                setupBlock = {
                    // Fresh presentation resources, with OS/driver/image caches retained.
                    killProcess()
                    startLauncher()
                    header("commands", closePanels = false)
                    val anchor = InstrumentationRegistry.getArguments().getString("fullPageHeroTag")
                    if (anchor != null) prepareFullPageHero(anchor)
                    SystemClock.sleep(1_000)
                    if (warmed) {
                        DeviceNavigationBurst(tour, periodMillis = periodMillis).run()
                        SystemClock.sleep(1_000)
                        check(focusedTag() == "commands") { "Full-page priming missed header" }
                    }
                    prepared = NavigationSample(
                        iteration,
                        checkNotNull(focusedTag()),
                        DeviceNavigationBurst(if (warmed) List(3) { tour }.flatten() else tour, periodMillis = periodMillis),
                    ).also { sample ->
                        sample.scene = JSONArray(
                            device.findObjects(By.res(Pattern.compile("content:.*"))).map { node ->
                                JSONObject().put("tag", node.resourceName)
                                    .put("bounds", node.visibleBounds.toShortString())
                                    .put("texts", JSONArray(node.findObjects(By.clazz("android.widget.TextView")).map { it.text }))
                            },
                        )
                    }
                },
            ) {
                val sample = checkNotNull(prepared)
                records += sample
                sample.burst.run()
                SystemClock.sleep(700)
            }
        } finally {
            val context = InstrumentationRegistry.getInstrumentation().context
            val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                ?.let(::File) ?: File(context.getExternalFilesDir(null), "navigation-evidence")
            directory.mkdirs()
            val name = if (periodMillis != 500L) {
                "full-page-rapid-warm"
            } else if (warmed) {
                "full-page-warm"
            } else {
                "full-page-first"
            }
            File(directory, "$name-${System.currentTimeMillis()}.json").writeText(
                JSONObject().put("scenario", name)
                    .put("compilation", "Partial Require baseline, three warmups; process restart, retained OS/driver/image caches")
                    .put("requestedKeyPeriodMs", periodMillis)
                    .put("settlingTailMs", 700)
                    .put("primingTours", if (warmed) 1 else 0)
                    .put("heroAnchor", InstrumentationRegistry.getArguments().getString("fullPageHeroTag") ?: "first available")
                    .put(
                        "firstExposure",
                        "Process restart and header/hero-anchor setup; first catalog tour, retained OS/driver/image caches",
                    )
                    .put("traceConfig", traceConfig)
                    .put("samples", JSONArray(records.map { it.toJson() })).toString(2),
            )
        }
    }

    private fun measure(name: String, continuation: Boolean, traversal: Boolean, stablePair: Boolean = false) {
        val right = KeyEvent.KEYCODE_DPAD_RIGHT
        val left = KeyEvent.KEYCODE_DPAD_LEFT
        val keys = when {
            !traversal -> List(15) { listOf(right, left) }.flatten()

            continuation -> List(7) { listOf(right, right, left, left) }.flatten() + listOf(right, left)

            else -> List(6) {
                List(4) { right } + KeyEvent.KEYCODE_DPAD_DOWN + List(4) { left } + KeyEvent.KEYCODE_DPAD_UP
            }.flatten()
        }
        val records = mutableListOf<NavigationSample>()
        var last: NavigationSample? = null
        var prepared: NavigationSample? = null
        val traceConfig = InstrumentationRegistry.getInstrumentation().context.assets
            .open("navigation-cpu.pbtxt").bufferedReader().use { it.readText() }
        try {
            benchmark.measureRepeated(
                packageName = LAUNCHER_PACKAGE,
                metrics = listOf(FrameTimingMetric()),
                perfettoConfig = PerfettoConfig.Text(traceConfig),
                compilationMode = CompilationMode.Partial(
                    baselineProfileMode = BaselineProfileMode.Require,
                    warmupIterations = 3,
                ),
                // Deliberately no StartupMode.COLD: it drops shader/kernel caches.
                iterations = 3,
                setupBlock = {
                    last?.takeIf { it.iteration != null }?.let { it.endFocus = focusedTag() }
                    backgroundThroughHome()
                    startLauncher()
                    SystemClock.sleep(1_000) // Untimed native foreground handoff, before all priming.
                    val activityState = device.executeShellCommand("dumpsys activity activities")
                    check(
                        activityState.lineSequence().any {
                            it.contains("topResumedActivity=") && it.contains("$LAUNCHER_PACKAGE/.MainActivity")
                        },
                    ) { "Explicit launcher lost native foreground before priming" }
                    prepareNavigationAnchor(continuation, traversal)
                    if (stablePair) {
                        val anchor = checkNotNull(InstrumentationRegistry.getArguments().getString("fullPageHeroTag"))
                        var found = false
                        for (step in 0 until 20) {
                            if (focusedTag() == anchor) {
                                found = true
                                break
                            }
                            val before = focusedTag()
                            remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT, pauseMillis = 250)
                            check(focusedTag() != before) { "Stable continuation anchor unavailable" }
                        }
                        check(found) { "Stable continuation anchor unavailable" }
                        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT, pauseMillis = 350)
                        check(focusedTag()?.startsWith("content:") == true && focusedTag() != anchor) {
                            "Stable continuation pair unavailable"
                        }
                        remoteKey(KeyEvent.KEYCODE_DPAD_LEFT, pauseMillis = 350)
                        check(focusedTag() == anchor) { "Stable continuation pair did not return" }
                    }
                    DeviceNavigationBurst(keys).run()
                    SystemClock.sleep(500)
                    prepared = NavigationSample(iteration, checkNotNull(focusedTag()), DeviceNavigationBurst(keys)).also { sample ->
                        sample.scene = JSONArray(
                            device.findObjects(By.res(Pattern.compile("content:.*"))).map { node ->
                                JSONObject().put("tag", node.resourceName)
                                    .put("bounds", node.visibleBounds.toShortString())
                                    .put("texts", JSONArray(node.findObjects(By.clazz("android.widget.TextView")).map { it.text }))
                            },
                        )
                    }
                },
            ) {
                val sample = checkNotNull(prepared)
                records += sample
                last = sample
                // No hierarchy access, per-event logging IO or idle acknowledgement in this burst.
                sample.burst.run()
                SystemClock.sleep(500) // Separate bounded response-settling tail; exported explicitly.
            }
            // Pinned Macrobenchmark kills the target at phase completion. Its final endpoint
            // must be verified from the trace, not from the default HOME shown after cleanup.
            records.filter { it.iteration != null && it.endFocus != null }.forEach {
                check(it.startFocus == it.endFocus) { "Navigation did not return to its anchor" }
            }
        } finally {
            val context = InstrumentationRegistry.getInstrumentation().context
            val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
                ?.let(::File) ?: File(context.getExternalFilesDir(null), "navigation-evidence")
            directory.mkdirs()
            File(directory, "$name-${System.currentTimeMillis()}.json").writeText(
                JSONObject().put("scenario", name)
                    .put("compilation", "Partial Require baseline, three warmup iterations; no cold-cache reset")
                    .put("requestedKeyPeriodMs", 150)
                    .put("settlingTailMs", 500)
                    .put("traceConfig", traceConfig)
                    .put(
                        "destinationValidation",
                        "Validate every delivered key and destination from traces; final endFocus is unavailable after benchmark process cleanup",
                    )
                    .put("samples", JSONArray(records.map { it.toJson() })).toString(2),
            )
        }
    }
}

/** Optional real configured-content anchor, supplied privately; no provider mutation or fixture injection. */
private fun MacrobenchmarkScope.prepareFullPageHero(tag: String) {
    require(tag.startsWith("content:"))
    remoteKey(KeyEvent.KEYCODE_DPAD_DOWN, pauseMillis = 350)
    check(focusedTag()?.startsWith("content:") == true) { "Full-page route requires the real hero layout" }
    for (step in 0 until 30) {
        val before = focusedTag()
        remoteKey(KeyEvent.KEYCODE_DPAD_LEFT, pauseMillis = 200)
        if (focusedTag() == before) break
        check(step < 29) { "Hero left boundary not found" }
    }
    var found = false
    for (step in 0 until 20) {
        if (focusedTag() == tag) {
            found = true
            break
        }
        val before = focusedTag()
        remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT, pauseMillis = 250)
        check(focusedTag() != before) { "Requested real hero anchor unavailable" }
    }
    check(found) { "Requested real hero anchor unavailable" }
    remoteKey(KeyEvent.KEYCODE_DPAD_UP, pauseMillis = 500)
    check(focusedTag() == "commands") { "Hero setup failed to return to header" }
}

private fun MacrobenchmarkScope.prepareNavigationAnchor(continuation: Boolean, traversal: Boolean) {
    // startLauncher has already verified Home. Back has its own navigation semantics there.
    header("commands", closePanels = false)
    var reached = false
    for (step in 0 until 6) {
        remoteKey(KeyEvent.KEYCODE_DPAD_DOWN, pauseMillis = 350)
        val tag = focusedTag()
        val inDock = tag != null && device.findObject(By.res("favorites-dock"))?.findObject(By.res(tag)) != null
        if (if (continuation) tag?.startsWith("content:") == true else tag?.startsWith("app:") == true && !inDock) {
            reached = true
            break
        }
    }
    assumeTrue("Required real navigation section is unavailable", reached)
    for (step in 0 until 30) {
        val before = focusedTag()
        remoteKey(KeyEvent.KEYCODE_DPAD_LEFT)
        if (focusedTag() == before) break
        check(step < 29) { "Left boundary not reached" }
    }
    if (continuation) {
        assumeTrue(
            "Requires at least ${if (traversal) 3 else 2} authentic continuation cards",
            device.findObjects(By.res(Pattern.compile("content:.*"))).size >= if (traversal) 3 else 2,
        )
    }
}

private class NavigationSample(val iteration: Int?, val startFocus: String?, val burst: DeviceNavigationBurst) {
    var endFocus: String? = null
    var scene: JSONArray? = null

    fun toJson(): JSONObject = JSONObject()
        .put("iteration", iteration ?: JSONObject.NULL)
        .put("startFocus", startFocus ?: JSONObject.NULL)
        .put("endFocus", endFocus ?: JSONObject.NULL)
        .put("events", burst.toJson())
        .put("scene", scene ?: JSONObject.NULL)
}

/** Absolute device-side scheduling; late events are retained and never silently skipped. */
private class DeviceNavigationBurst(private val keys: List<Int>, private val periodMillis: Long = 150) {
    // index, action, key, repeat, down uptime, event uptime, send elapsed ns, return elapsed ns, accepted
    private val events = Array(keys.size * 2) { LongArray(9) }

    fun run() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val start = SystemClock.uptimeMillis()
        keys.forEachIndexed { index, key ->
            val remaining = start + index * periodMillis - SystemClock.uptimeMillis()
            if (remaining > 0) SystemClock.sleep(remaining)
            val down = SystemClock.uptimeMillis()
            var acceptedDown = false
            val acceptedUp: Boolean
            try {
                acceptedDown = inject(automation, index * 2, key, KeyEvent.ACTION_DOWN, down)
            } finally {
                acceptedUp = inject(automation, index * 2 + 1, key, KeyEvent.ACTION_UP, down)
            }
            check(acceptedDown && acceptedUp) { "Input injection failed at $index" }
        }
    }

    private fun inject(automation: UiAutomation, index: Int, key: Int, action: Int, down: Long): Boolean {
        val eventTime = SystemClock.uptimeMillis()
        val record = events[index]
        record[0] = index.toLong()
        record[1] = action.toLong()
        record[2] = key.toLong()
        record[3] = 0
        record[4] = down
        record[5] = eventTime
        record[6] = SystemClock.elapsedRealtimeNanos()
        val accepted = automation.injectInputEvent(
            KeyEvent(down, eventTime, action, key, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_DPAD),
            true,
        )
        record[7] = SystemClock.elapsedRealtimeNanos()
        record[8] = if (accepted) 1 else 0
        return accepted
    }

    fun toJson(): JSONArray = JSONArray(events.map { JSONArray(it.toList()) })
}
