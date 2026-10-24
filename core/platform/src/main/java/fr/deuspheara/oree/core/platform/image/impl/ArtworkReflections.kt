package fr.deuspheara.oree.core.platform.image.impl

import android.graphics.Bitmap
import android.os.Trace
import android.util.LruCache
import coil3.BitmapImage
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.rendering.ArtworkPalette
import fr.deuspheara.oree.core.domain.rendering.ReflectionKey
import fr.deuspheara.oree.core.model.ReflectionColors
import fr.deuspheara.oree.core.model.reflectionColors
import fr.deuspheara.oree.core.platform.image.ReflectionRequestKey
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shares the decoded Coil image, serializes bounded scratch work, and retains only 3 colors. */
class ArtworkReflections(private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default)) :
    Interceptor,
    ArtworkPalette {
    private data class Prepared(val colors: ReflectionColors?)

    private val cache = LruCache<ReflectionKey, Prepared>(64)
    private val preparation = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val pending = mutableMapOf<ReflectionKey, Deferred<Unit>>()

    override fun colors(key: ReflectionKey): ReflectionColors? = cache.get(key)?.colors

    override suspend fun awaitColors(key: ReflectionKey): ReflectionColors? {
        val task = synchronized(cache) { pending[key] }
        try {
            task?.await()
        } catch (_: kotlinx.coroutines.CancellationException) {
            // A trim may cancel optional metadata while the observing card remains active.
            currentCoroutineContext().ensureActive()
        }
        return colors(key)
    }

    private var generation = 0L

    fun clear() = synchronized(cache) {
        generation++
        cache.evictAll()
        pending.values.toList().forEach { it.cancel() }
        pending.clear()
    }

    private fun store(key: ReflectionKey, colors: ReflectionColors?, expectedGeneration: Long) {
        synchronized(cache) {
            if (generation == expectedGeneration) cache.put(key, Prepared(colors))
        }
    }

    override suspend fun intercept(chain: Interceptor.Chain): coil3.request.ImageResult {
        val key = chain.request.extras[ReflectionRequestKey] ?: return chain.proceed()
        val expectedGeneration = synchronized(cache) { generation }
        val result = chain.proceed()
        if (result !is SuccessResult) return result
        if (cache.get(key) != null) {
            Trace.beginSection("TVL:reflectionCacheHit")
            Trace.endSection()
            return result
        }
        val bitmap = (result.image as? BitmapImage)?.bitmap ?: return result
        if (
            bitmap.width.toLong() * bitmap.height > 480L * 270 || key.width <= 0 || key.height <= 0
        ) {
            return result
        }
        val task = synchronized(cache) {
            // Bound retained decoded references as well as scratch concurrency. Decoration is
            // optional: a full queue never delays or fails successful artwork delivery.
            if (generation != expectedGeneration || cache.get(key) != null || key in pending || pending.size >= 8) return result
            scope.async(start = CoroutineStart.LAZY) {
                prepare(key, bitmap, expectedGeneration)
            }.also { deferred ->
                pending[key] = deferred
                deferred.invokeOnCompletion {
                    synchronized(cache) {
                        if (pending[key] === deferred) pending.remove(key)
                    }
                }
            }
        }
        task.start()
        return result
    }

    private suspend fun prepare(key: ReflectionKey, bitmap: Bitmap, expectedGeneration: Long) {
        preparation.withLock {
            if (cache.get(key) == null) {
                coroutineContext.ensureActive()
                Trace.beginSection("TVL:reflectionMetadata")
                try {
                    // A hardware bitmap cannot be sampled by the CPU. Copy only the already
                    // bounded decode, never the source; release scratch before returning.
                    val scratch =
                        if (bitmap.config == Bitmap.Config.HARDWARE) {
                            Trace.beginSection("TVL:reflectionHardwareCopy")
                            try {
                                bitmap.copy(Bitmap.Config.ARGB_8888, false)
                            } finally {
                                Trace.endSection()
                            }
                        } else {
                            null
                        }
                    try {
                        val readable = scratch ?: bitmap
                        val targetAspect = key.width.toFloat() / key.height
                        val sourceAspect = readable.width.toFloat() / readable.height
                        val cropWidth =
                            if (key.crop && sourceAspect > targetAspect) {
                                readable.height * targetAspect
                            } else {
                                readable.width.toFloat()
                            }
                        val cropHeight =
                            if (key.crop && sourceAspect < targetAspect) {
                                readable.width / targetAspect
                            } else {
                                readable.height.toFloat()
                            }
                        val x0 = (readable.width - cropWidth) / 2
                        val y0 = (readable.height - cropHeight) / 2
                        // The highlight lives near the upper edge: sample its upper 40%,
                        // split into three horizontal bands, rather than whole-image dominance.
                        val columns = IntArray(24) { column ->
                            (x0 + (column + .5f) * cropWidth / 24).toInt().coerceIn(0, readable.width - 1)
                        }
                        val left = columns.first()
                        val sampleWidth = columns.last() - left + 1
                        // Six bounded row copies replace 144 individual JNI pixel reads.
                        val samples = IntArray(sampleWidth * 6)
                        for (row in 0 until 6) {
                            coroutineContext.ensureActive()
                            val y = (y0 + (row + .5f) * cropHeight * .4f / 6).toInt().coerceIn(0, readable.height - 1)
                            readable.getPixels(samples, row * sampleWidth, sampleWidth, left, y, sampleWidth, 1)
                        }
                        val colors = reflectionColors { column, row ->
                            samples[row * sampleWidth + columns[column] - left]
                        }
                        coroutineContext.ensureActive()
                        store(key, colors, expectedGeneration)
                    } finally {
                        scratch?.recycle()
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    store(key, null, expectedGeneration)
                } finally {
                    Trace.endSection()
                }
            }
        }
    }
}
