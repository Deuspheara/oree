package fr.deuspheara.oree

import android.util.Log
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import coil3.EventListener
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

/** Request-level diagnostic on actual Home; debug instrumentation, never a timing benchmark. */
@OptIn(ExperimentalTestApi::class)
class ArtworkCacheDiagnosticTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val phase = AtomicReference("cold")
    private val counts = ConcurrentHashMap<String, Counts>()
    private val pending = AtomicInteger()
    private lateinit var original: coil3.ImageLoader
    private lateinit var loader: coil3.ImageLoader

    @get:Rule
    val rules: RuleChain =
        RuleChain.outerRule(
            object : ExternalResource() {
                override fun before() {
                    installRecordingLoader()
                }

                override fun after() {
                    SingletonImageLoader.setUnsafe(original)
                    loader.shutdown()
                }
            },
        )
            .around(compose)

    private class Counts {
        val started = AtomicInteger()
        val success = AtomicInteger()
        val memory = AtomicInteger()
        val decoded = AtomicInteger()
        val errors = AtomicInteger()
        val canceled = AtomicInteger()

        fun json() = JSONObject()
            .put("started", started.get())
            .put("success", success.get())
            .put("memoryCacheSuccess", memory.get())
            .put("decodeStarts", decoded.get())
            .put("errors", errors.get())
            .put("canceled", canceled.get())
            .put(
                "memoryCacheSuccessFraction",
                if (success.get() > 0) {
                    memory.get().toDouble() / success.get()
                } else {
                    JSONObject.NULL
                },
            )
    }

    private fun installRecordingLoader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as LauncherApplication
        original = SingletonImageLoader.get(context)
        loader =
            original
                .newBuilder()
                .memoryCache { MemoryCache.Builder().maxSizeBytes(12L * 1024 * 1024).build() }
                .eventListenerFactory { request ->
                    if (!request.data.toString().startsWith("android.resource://")) {
                        EventListener.NONE
                    } else {
                        val counter = counts.getOrPut(phase.get()) { Counts() }
                        object : EventListener() {
                            override fun onStart(request: ImageRequest) {
                                counter.started.incrementAndGet()
                                pending.incrementAndGet()
                            }

                            override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                                counter.success.incrementAndGet()
                                if (result.dataSource == DataSource.MEMORY_CACHE) {
                                    counter.memory.incrementAndGet()
                                }
                                pending.decrementAndGet()
                            }

                            override fun onCancel(request: ImageRequest) {
                                counter.canceled.incrementAndGet()
                                pending.decrementAndGet()
                            }

                            override fun onError(request: ImageRequest, result: ErrorResult) {
                                counter.errors.incrementAndGet()
                                pending.decrementAndGet()
                            }

                            override fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {
                                counter.decoded.incrementAndGet()
                            }
                        }
                    }
                }
                .build()
        app.dependencies.koin.get<ArtworkReflections>().clear()
        SingletonImageLoader.setUnsafe(loader)
    }

    @Test
    fun realHomeRequestsReportColdWarmAndTrimRecovery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(
            "Diagnostic is debug instrumentation, not optimized timing",
            context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE !=
                0,
        )
        val memory = requireNotNull(loader.memoryCache)
        assertEquals(12L * 1024 * 1024, memory.maxSize)
        assertNull(loader.diskCache)
        fun settled() {
            compose.waitForIdle()
            compose.waitUntil(30_000) { pending.get() == 0 }
            Thread.sleep(300) // Request quiescence only; no latency or frame claim.
            compose.waitForIdle()
            assertEquals(0, pending.get())
            assertTrue(memory.size <= memory.maxSize)
        }
        fun route() {
            compose.onNodeWithTag("commands").performSemanticsAction(
                SemanticsActions.RequestFocus,
            ) {
                it()
            }
            compose.waitForIdle()
            repeat(3) {
                repeat(5) {
                    compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionDown) }
                    compose.waitForIdle()
                }
                repeat(5) {
                    compose.onNode(isFocused()).performKeyInput { pressKey(Key.DirectionUp) }
                    compose.waitForIdle()
                }
            }
            settled()
            compose.onNodeWithTag("commands").assertIsFocused()
        }
        val snapshots = JSONArray()
        fun snapshot(label: String) {
            snapshots.put(
                JSONObject()
                    .put(
                        "phase",
                        label,
                    )
                    .put("strongCacheBytes", memory.size)
                    .put("maxCacheBytes", memory.maxSize)
                    .put("cacheKeys", memory.keys.size),
            )
        }
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("home-ready").fetchSemanticsNodes().isNotEmpty()
        }
        settled()
        snapshot("cold")
        phase.set("priming")
        route()
        snapshot("primed")
        phase.set("warm")
        route()
        snapshot("warm")
        val beforeKeys = memory.keys.size
        memory.trimToSize(memory.maxSize / 4)
        assertTrue(memory.size <= memory.maxSize / 4)
        snapshot("trimmed")
        phase.set("trimRecovery")
        route()
        snapshot("recovered")
        val phases = JSONObject()
        counts.toSortedMap().forEach { (name, counter) ->
            assertEquals("No failed installed-resource request in $name", 0, counter.errors.get())
            assertEquals(
                counter.started.get(),
                counter.success.get() + counter.errors.get() + counter.canceled.get(),
            )
            phases.put(name, counter.json())
        }
        assertTrue(
            "Warm navigation must issue observable image requests",
            requireNotNull(counts["warm"]).success.get() > 0,
        )
        val report =
            JSONObject()
                .put(
                    "scope",
                    "Debug physical production MainActivity/Home with existing catalog/favorites/preferences; cloned shared loader components, fresh bounded12MiB memory cache, no disk cache",
                )
                .put("phases", phases)
                .put("snapshots", snapshots)
                .put("keysBeforeExplicitTrim", beforeKeys)
                .put(
                    "limits",
                    JSONArray(
                        listOf(
                            "Request result ratio, not bitmap residency or GPU texture reuse.",
                            "Explicit cache trim is not actual system memory pressure. MemoryCache.keys unions strong and weak keys; it is not a strong-retention count. Weakly held images may still produce memory hits after strong-cache eviction.",
                            "Debug production activity with settled test input is not optimized movement/latency evidence.",
                            "Factory phase is captured per request; cancellations are reported separately from successful hit ratio.",
                        ),
                    ),
                )
        File(context.filesDir, "artwork-cache-diagnostic.json").writeText(report.toString(2))
        Log.i(
            "ArtworkCacheDiagnostic",
            "Completed request-level cache diagnostic; report saved privately",
        )
    }
}
