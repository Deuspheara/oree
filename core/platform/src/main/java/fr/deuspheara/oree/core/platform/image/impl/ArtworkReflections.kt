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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Shares the decoded Coil image, serializes bounded scratch work, and retains only 3 colors. */
class ArtworkReflections(private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default)) :
    Interceptor,
    ArtworkPalette {
    private data class Prepared(val colors: ReflectionColors?)

    private val cache = LruCache<ReflectionKey, Prepared>(64)
    private val preparation = Mutex()

    override fun colors(key: ReflectionKey): ReflectionColors? = cache.get(key)?.colors

    private var generation = 0L

    fun clear() = synchronized(cache) {
        generation++
        cache.evictAll()
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
        withContext(dispatchers.default) {
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
                            val colors = reflectionColors { column, row ->
                                val x =
                                    (x0 + (column + .5f) * cropWidth / 24)
                                        .toInt()
                                        .coerceIn(0, readable.width - 1)
                                val y =
                                    (y0 + (row + .5f) * cropHeight * .4f / 6)
                                        .toInt()
                                        .coerceIn(0, readable.height - 1)
                                readable.getPixel(x, y)
                            }
                            coroutineContext.ensureActive()
                            store(key, colors, expectedGeneration)
                        } finally {
                            scratch?.recycle()
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: IllegalStateException) {
                        store(
                            key,
                            null,
                            expectedGeneration,
                        ) // A missing palette must never fail artwork.
                    } catch (_: IllegalArgumentException) {
                        store(key, null, expectedGeneration)
                    } finally {
                        Trace.endSection()
                    }
                }
            }
        }
        return result
    }
}
