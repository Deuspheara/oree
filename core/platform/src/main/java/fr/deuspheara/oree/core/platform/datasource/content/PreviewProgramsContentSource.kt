package fr.deuspheara.oree.core.platform.datasource.content

import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.os.CancellationSignal
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.source.ContentSource
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentFeed
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.platform.launch.resolveContentOnWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Public app-published previews. Ranking is provider weight, never invented engagement. */
class PreviewProgramsContentSource(
    private val context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
    private val query: ((Array<String>, CancellationSignal) -> Cursor?)? = null,
) : ContentSource {
    override val id = ContentSourceId.PREVIEW
    override val changes = providerChanges(context, TvContract.PreviewPrograms.CONTENT_URI, dispatchers.io)

    override suspend fun loadSnapshot(): ContentFeed = withContext(dispatchers.io) {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val result =
                try {
                    val fields =
                        arrayOf(
                            "_id",
                            "title",
                            "package_name",
                            "poster_art_uri",
                            "intent_uri",
                            "weight",
                        )
                    val rows = mutableListOf<Pair<ContentEntry, Long>>()
                    val metadataFields = fields + "thumbnail_uri"
                    fun read(projection: Array<String>) = if (query != null) {
                        query.invoke(projection, signal)
                    } else {
                        context.contentResolver.query(
                            TvContract.PreviewPrograms.CONTENT_URI,
                            projection,
                            null,
                            null,
                            "weight DESC, _id DESC",
                            signal,
                        )
                    }
                    val cursor = try {
                        read(metadataFields)
                    } catch (_: IllegalArgumentException) {
                        read(fields)
                    }
                    var seen = 0
                    cursor?.use { c ->
                        while (seen++ < 200 && c.moveToNext()) {
                            signal.throwIfCanceled()
                            val publisher = c.getString(2).orEmpty()
                            val title =
                                c.getString(1)
                                    ?.filterNot(Char::isISOControl)
                                    ?.take(200)
                                    ?.trim()
                                    .orEmpty()
                            val link = c.getString(4).orEmpty()
                            if (
                                title.isEmpty() ||
                                c.getLong(0) < 0 ||
                                resolveContentOnWorker(context, link, publisher) == null
                            ) {
                                continue
                            }
                            val art = contentArtworkUri(c.getString(3))
                                ?: c.getColumnIndex("thumbnail_uri").takeIf { it >= 0 && !c.isNull(it) }
                                    ?.let { contentArtworkUri(c.getString(it)) }
                            rows +=
                                ContentEntry(
                                    "preview:$publisher:${c.getLong(0)}",
                                    title,
                                    publisher,
                                    art,
                                    link,
                                    null,
                                    ContentSourceId.PREVIEW,
                                ) to c.getLong(5)
                        }
                    } ?: throw IllegalArgumentException("Provider unavailable")
                    val entries =
                        rows
                            .sortedByDescending { it.second }
                            .map { it.first }
                            .distinctBy { it.id }
                            .take(200)
                    ContentFeed(
                        if (entries.isEmpty()) {
                            ContentAvailability.EMPTY
                        } else {
                            ContentAvailability.AVAILABLE
                        },
                        entries,
                    )
                } catch (_: SecurityException) {
                    ContentFeed(ContentAvailability.DENIED)
                } catch (_: IllegalArgumentException) {
                    ContentFeed(ContentAvailability.UNSUPPORTED)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    ContentFeed(ContentAvailability.ERROR)
                }
            if (continuation.isActive) continuation.resumeWith(Result.success(result))
        }
    }
}
