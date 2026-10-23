package fr.deuspheara.oree

import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Precision
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.platform.image.LocalPhotoThumbnail
import fr.deuspheara.oree.core.platform.image.impl.LocalPhotoThumbnailFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPhotoThumbnailTest {
    @Test
    fun resourceFallbackStaysBoundedAndCannotReplaceFullscreenImage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizeBytes(12L * 1024 * 1024).build() }
            .diskCache(null)
            .components { add(LocalPhotoThumbnailFetcher.Factory(AppDispatchers(Dispatchers.IO, Dispatchers.Default))) }
            .build()
        try {
            val uri = "android.resource://${context.packageName}/${fr.deuspheara.oree.core.platform.R.drawable.landscape_forest}"
            val thumbnail = LocalPhotoThumbnail(uri)
            val small = loader.execute(
                ImageRequest.Builder(context).data(thumbnail).memoryCacheKey(thumbnail.cacheKey)
                    .size(256, 144).precision(Precision.EXACT).build(),
            )
            assertTrue(small is SuccessResult)
            small as SuccessResult
            assertTrue(small.image.width <= 256 && small.image.height <= 144)
            val full = loader.execute(
                ImageRequest.Builder(context).data(uri).size(1280, 720).precision(Precision.EXACT).build(),
            )
            assertTrue(full is SuccessResult)
            full as SuccessResult
            assertTrue("Fullscreen must not upscale the cached thumbnail", full.image.width >= 640)
            val cached = loader.execute(small.request)
            assertTrue(cached is SuccessResult)
            assertEquals(coil3.decode.DataSource.MEMORY_CACHE, (cached as SuccessResult).dataSource)
        } finally {
            loader.shutdown()
        }
    }

    @Test
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    fun providerThumbnailAvoidsOriginalReadAndFitsPortraitImage() = runBlocking {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val file = java.io.File.createTempFile("thumbnail-fixture", ".png", target.cacheDir)
        val bitmap = android.graphics.Bitmap.createBitmap(360, 480, android.graphics.Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        var thumbnailReads = 0
        var originalReads = 0
        val provider = object : android.content.ContentProvider() {
            override fun onCreate() = true
            override fun getType(uri: android.net.Uri) = "image/png"
            override fun query(
                uri: android.net.Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?,
            ): android.database.Cursor? = null
            override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? = null
            override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun update(
                uri: android.net.Uri,
                values: android.content.ContentValues?,
                selection: String?,
                selectionArgs: Array<out String>?,
            ) = 0
            override fun openTypedAssetFile(
                uri: android.net.Uri,
                mimeTypeFilter: String,
                opts: android.os.Bundle?,
                signal: android.os.CancellationSignal?,
            ): android.content.res.AssetFileDescriptor {
                thumbnailReads++
                val size = opts?.getParcelable<android.graphics.Point>(android.content.ContentResolver.EXTRA_SIZE)
                assertEquals(android.graphics.Point(256, 144), size)
                return android.content.res.AssetFileDescriptor(
                    android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY),
                    0,
                    android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH,
                )
            }
            override fun openAssetFile(uri: android.net.Uri, mode: String): android.content.res.AssetFileDescriptor {
                originalReads++
                throw java.io.IOException("Original must not be opened")
            }
        }
        provider.attachInfo(
            target,
            android.content.pm.ProviderInfo().apply {
                authority = "oree-thumbnail-test"
                exported = false
            },
        )
        val resolver = android.content.ContentResolver.wrap(provider)
        val context = object : android.content.ContextWrapper(target) {
            override fun getContentResolver() = resolver
        }
        val loader = ImageLoader.Builder(context).diskCache(null)
            .components { add(LocalPhotoThumbnailFetcher.Factory(AppDispatchers(Dispatchers.IO, Dispatchers.Default))) }
            .build()
        try {
            val result = loader.execute(
                ImageRequest.Builder(context).data(LocalPhotoThumbnail("content://oree-thumbnail-test/photo"))
                    .size(256, 144).build(),
            )
            assertTrue(result is SuccessResult)
            result as SuccessResult
            assertTrue(result.image.width <= 256 && result.image.height <= 144)
            assertEquals(1, thumbnailReads)
            assertEquals(0, originalReads)
        } finally {
            loader.shutdown()
            file.delete()
        }
    }

    @Test
    fun arbitraryFileSchemeDoesNotFallBackToOriginalDecode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ImageLoader.Builder(context).diskCache(null)
            .components { add(LocalPhotoThumbnailFetcher.Factory(AppDispatchers(Dispatchers.IO, Dispatchers.Default))) }
            .build()
        try {
            val result = loader.execute(
                ImageRequest.Builder(context).data(LocalPhotoThumbnail("file:///unavailable-photo.jpg"))
                    .size(256, 144).build(),
            )
            assertTrue(result is ErrorResult)
            assertEquals("Photo unavailable", (result as ErrorResult).throwable.message)
        } finally {
            loader.shutdown()
        }
    }
}
