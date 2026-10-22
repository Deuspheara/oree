package fr.deuspheara.oree.core.platform.image.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.repository.WallpaperRepository
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.platform.R
import fr.deuspheara.oree.core.platform.image.WallpaperFrame
import fr.deuspheara.oree.core.platform.image.WallpaperRenderer
import fr.deuspheara.oree.core.platform.image.processing.blurArgb
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only the active wallpaper is retained by composition; processing never runs on main. */
class AndroidWallpaperStore(
    private val context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : WallpaperRepository,
    WallpaperRenderer {
    override suspend fun importImage(uri: String): String = importImage(Uri.parse(uri))

    suspend fun importImage(uri: Uri): String = withContext(dispatchers.io) {
        if (uri.scheme != "content") throw IOException("Choose a document image")
        val bitmap = read(uri)
        val name = "wallpaper-${UUID.randomUUID()}.jpg"
        val target = File(context.filesDir, name)
        val temp = File(context.filesDir, "$name.tmp")
        try {
            temp.outputStream().use {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) {
                    throw IOException("Image encoding failed")
                }
            }
            if (!temp.renameTo(target)) throw IOException("Image commit failed")
            name
        } finally {
            temp.delete()
            bitmap.recycle()
        }
    }

    override suspend fun discard(name: String) = withContext(dispatchers.io) {
        if (name.matches(Regex("wallpaper-[a-f0-9-]+[.]jpg"))) {
            File(context.filesDir, name).delete()
        }
    }

    private fun read(uri: Uri): Bitmap {
        val bytes =
            context.contentResolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > 20 * 1024 * 1024) throw IOException("Image exceeds 20 MiB")
                    output.write(buffer, 0, n)
                }
                output.toByteArray()
            } ?: throw IOException("Image unavailable")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (
            bounds.outWidth !in 1..16000 ||
            bounds.outHeight !in 1..16000 ||
            bounds.outWidth.toLong() * bounds.outHeight > 64_000_000
        ) {
            throw IOException("Invalid image dimensions")
        }
        var sample = 1
        while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1080) sample *= 2
        val decoded =
            BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: throw IOException("Invalid image")
        return limit(decoded)
    }

    private fun limit(bitmap: Bitmap): Bitmap {
        val ratio = max(bitmap.width / 1280f, bitmap.height / 720f).coerceAtLeast(1f)
        if (ratio == 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width / ratio).toInt().coerceAtLeast(1),
            (bitmap.height / ratio).toInt().coerceAtLeast(1),
            true,
        )
            .also { if (it !== bitmap) bitmap.recycle() }
    }

    override suspend fun load(profile: LocalProfile, dynamic: Uri?): WallpaperFrame = withContext(dispatchers.io) {
        val source =
            try {
                when {
                    dynamic != null &&
                        dynamic.scheme in listOf("content", "android.resource") -> read(dynamic)

                    profile.wallpaper == WallpaperMode.PERSONAL && profile.image != null ->
                        limit(
                            BitmapFactory.decodeFile(File(context.filesDir, profile.image).path)
                                ?: throw IOException("Missing image"),
                        )

                    else -> landscape(profile.landscape)
                }
            } catch (_: IOException) {
                landscape(profile.landscape)
            } catch (_: SecurityException) {
                landscape(profile.landscape)
            } catch (_: IllegalArgumentException) {
                landscape(profile.landscape)
            }
        withContext(dispatchers.default) {
            val width = 192
            val height = 108
            val small =
                Bitmap.createScaledBitmap(source, width, height, true).let {
                    if (it === source) it.copy(Bitmap.Config.ARGB_8888, true) else it
                }
            val pixels = IntArray(width * height)
            small.getPixels(pixels, 0, width, 0, 0, width, height)
            repeat(3) { blurArgb(pixels, width, height, 2) }
            small.setPixels(pixels, 0, width, 0, 0, width, height)
            WallpaperFrame(source, small, pixels[(height / 2) * width + width / 2])
        }
    }

    private fun landscape(index: Int): Bitmap {
        val original =
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.landscape_forest,
                BitmapFactory.Options().apply { inScaled = false },
            )
        if (index == 0) return limit(original)
        // A second quiet palette from the same original landscape, prepared once.
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val matrix = android.graphics.ColorMatrix().apply { setSaturation(.45f) }
        val paint =
            android.graphics.Paint().apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
            }
        android.graphics.Canvas(result).drawBitmap(original, 0f, 0f, paint)
        original.recycle()
        return limit(result)
    }
}
