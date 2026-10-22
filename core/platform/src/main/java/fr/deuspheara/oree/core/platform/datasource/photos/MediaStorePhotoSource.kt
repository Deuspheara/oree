package fr.deuspheara.oree.core.platform.datasource.photos

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.source.PhotoDataSource
import fr.deuspheara.oree.core.model.LocalPhoto
import fr.deuspheara.oree.core.model.LocalPhotos
import fr.deuspheara.oree.core.model.PhotoAvailability
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Optional foreground gallery only. Access is requested by the user, never at startup. */
class MediaStorePhotoSource(
    private val context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : PhotoDataSource {
    override suspend fun load(): LocalPhotos = withContext(dispatchers.io) {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val result =
                try {
                    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val args =
                        Bundle().apply {
                            putInt(ContentResolver.QUERY_ARG_LIMIT, 200)
                            putString(
                                ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                                "date_added DESC, _id DESC",
                            )
                        }
                    val rows = mutableListOf<LocalPhoto>()
                    context.contentResolver
                        .query(collection, arrayOf("_id", "_display_name"), args, signal)
                        ?.use { cursor ->
                            while (rows.size < 200 && cursor.moveToNext()) {
                                val id = cursor.getLong(0)
                                if (id > 0) {
                                    rows +=
                                        LocalPhoto(
                                            ContentUris.withAppendedId(collection, id)
                                                .toString(),
                                            cursor
                                                .getString(1)
                                                ?.filterNot(Char::isISOControl)
                                                ?.take(160)
                                                .orEmpty(),
                                        )
                                }
                            }
                        } ?: throw IllegalStateException("No media cursor")
                    LocalPhotos(
                        if (rows.isEmpty()) {
                            PhotoAvailability.EMPTY
                        } else {
                            PhotoAvailability.AVAILABLE
                        },
                        rows,
                    )
                } catch (_: SecurityException) {
                    LocalPhotos(PhotoAvailability.DENIED)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    LocalPhotos(PhotoAvailability.ERROR)
                }
            if (continuation.isActive) continuation.resume(result)
        }
    }
}
