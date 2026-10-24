package fr.deuspheara.oree

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import fr.deuspheara.oree.core.domain.rendering.ReflectionKey
import fr.deuspheara.oree.core.platform.image.ReflectionRequestKey
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkReflectionsTest {
    @Test
    fun cropVersionEvictionAndColdCacheKeepArtworkUsable() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cache = ArtworkReflections()
        val loader = ImageLoader.Builder(context).diskCache(null).components { add(cache) }.build()
        val bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        suspend fun load(key: ReflectionKey) {
            val result =
                loader.execute(
                    ImageRequest.Builder(context)
                        .data(bitmap)
                        .size(160, 160)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .apply { extras[ReflectionRequestKey] = key }
                        .build(),
                )
            assertTrue("Metadata preparation must not fail artwork", result is SuccessResult)
            assertSame(
                "Preparation leaves the decoded bitmap unchanged",
                bitmap,
                ((result as SuccessResult).image as coil3.BitmapImage).bitmap,
            )
            cache.awaitColors(key)
        }
        try {
            for (y in 0 until 160) {
                for (x in 0 until 160) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (y < 40) {
                            Color.BLUE
                        } else if (y < 120) {
                            Color.RED
                        } else {
                            Color.GREEN
                        },
                    )
                }
            }
            val first = ReflectionKey("fixture@1", 160, 90, true)
            load(first)
            val color = cache.colors(first)!!.center
            assertTrue(
                "Sample upper reflection region after center crop",
                Color.red(color) > Color.blue(color),
            )
            assertEquals("Bottom green must not dominate the reflection", 0, Color.green(color))
            bitmap.eraseColor(Color.GREEN)
            load(first)
            assertEquals(
                "Unchanged identity reuses prepared metadata",
                color,
                cache.colors(first)!!.center,
            )
            val revised = first.copy(artwork = "fixture@2")
            load(revised)
            assertEquals(Color.GREEN, cache.colors(revised)!!.center)
            for (i in 3..66) load(first.copy(artwork = "fixture@$i"))
            assertNull("Metadata has bounded LRU eviction", cache.colors(first))
            cache.clear()
            assertNull(cache.colors(revised))
            load(first)
            assertEquals(
                "Cold metadata cache remains correct",
                Color.GREEN,
                cache.colors(first)!!.center,
            )
            val hardware = bitmap.copy(Bitmap.Config.HARDWARE, false)!!
            try {
                val hardwareKey = first.copy(artwork = "hardware@1")
                val result =
                    loader.execute(
                        ImageRequest.Builder(context)
                            .data(hardware)
                            .size(160, 160)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .apply { extras[ReflectionRequestKey] = hardwareKey }
                            .build(),
                    ) as SuccessResult
                assertSame(
                    "Readback retains the original hardware artwork",
                    hardware,
                    (result.image as coil3.BitmapImage).bitmap,
                )
                assertEquals(Color.GREEN, cache.awaitColors(hardwareKey)!!.center)
            } finally {
                hardware.recycle()
            }
        } finally {
            cache.clear()
            loader.shutdown()
            bitmap.recycle()
        }
    }
}
