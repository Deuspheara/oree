package fr.deuspheara.oree.core.platform.image

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import fr.deuspheara.oree.core.platform.image.processing.blurArgb
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** One bounded window copy before opening a menu; never a live blur or a permission. */
suspend fun captureMenuBackdrop(activity: Activity): WallpaperFrame? {
    val bitmap =
        withTimeoutOrNull(700) {
            suspendCancellableCoroutine<Bitmap?> { continuation ->
                val target = Bitmap.createBitmap(384, 216, Bitmap.Config.ARGB_8888)
                try {
                    PixelCopy.request(
                        activity.window,
                        target,
                        { result ->
                            if (continuation.isActive) {
                                if (result == PixelCopy.SUCCESS) {
                                    continuation.resume(target)
                                } else {
                                    target.recycle()
                                    continuation.resume(null)
                                }
                            } else {
                                target.recycle()
                            }
                        },
                        Handler(Looper.getMainLooper()),
                    )
                } catch (_: IllegalArgumentException) {
                    target.recycle()
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } ?: return null
    return withContext(Dispatchers.Default) {
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            blurArgb(pixels, bitmap.width, bitmap.height, 1)
            val frost =
                Bitmap.createBitmap(pixels, bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            WallpaperFrame(bitmap, frost, pixels[pixels.size / 2])
        } catch (_: IllegalArgumentException) {
            bitmap.recycle()
            null
        }
    }
}
