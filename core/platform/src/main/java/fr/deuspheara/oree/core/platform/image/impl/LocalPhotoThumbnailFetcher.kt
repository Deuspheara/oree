package fr.deuspheara.oree.core.platform.image.impl

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Precision
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.platform.image.LocalPhotoThumbnail
import java.io.IOException
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Ask the local provider for a bounded thumbnail before opening the original image. */
class LocalPhotoThumbnailFetcher private constructor(
    private val data: LocalPhotoThumbnail,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val dispatchers: AppDispatchers,
) : Fetcher {
    override suspend fun fetch(): ImageFetchResult {
        val uri = Uri.parse(data.uri)
        if (uri.scheme !in setOf("content", "android.resource")) throw IOException("Photo unavailable")
        val bitmap = if (Build.VERSION.SDK_INT >= 29 && uri.scheme == "content") {
            try {
                providerThumbnail(uri)
            } catch (_: IOException) {
                currentCoroutineContext().ensureActive()
                null
            } catch (_: UnsupportedOperationException) {
                null
            }
        } else {
            null
        }
        if (bitmap != null) return ImageFetchResult(fit(bitmap).asImage(), true, DataSource.DISK)
        // Old Android/provider fallback. Cache once under the thumbnail identity in the outer request.
        val result = imageLoader.execute(
            ImageRequest.Builder(options.context)
                .data(uri)
                .size(256, 144)
                .precision(Precision.EXACT)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build(),
        )
        return when (result) {
            is SuccessResult -> ImageFetchResult(result.image, true, result.dataSource)
            is ErrorResult -> throw result.throwable
        }
    }

    private suspend fun fit(bitmap: Bitmap): Bitmap {
        var result = bitmap
        try {
            withContext(dispatchers.default) {
                val ratio = max(bitmap.width / 256f, bitmap.height / 144f).coerceAtLeast(1f)
                if (ratio > 1f) {
                    result = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width / ratio).toInt().coerceAtLeast(1),
                        (bitmap.height / ratio).toInt().coerceAtLeast(1),
                        true,
                    )
                    if (result !== bitmap) bitmap.recycle()
                }
            }
            return result
        } catch (cancelled: CancellationException) {
            result.recycle()
            throw cancelled
        }
    }

    @androidx.annotation.RequiresApi(29)
    private suspend fun providerThumbnail(uri: Uri): Bitmap = withContext(dispatchers.io) {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                val bitmap = options.context.contentResolver.loadThumbnail(uri, Size(256, 144), signal)
                continuation.resume(bitmap) { _, unclaimed, _ -> unclaimed.recycle() }
            } catch (error: Exception) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
    }

    class Factory(private val dispatchers: AppDispatchers) : Fetcher.Factory<LocalPhotoThumbnail> {
        override fun create(data: LocalPhotoThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            LocalPhotoThumbnailFetcher(data, options, imageLoader, dispatchers)
    }
}
