package fr.deuspheara.oree.core.platform.datasource.content

import android.content.Context
import android.media.tv.TvContract
import android.os.CancellationSignal
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.concurrency.Clock
import fr.deuspheara.oree.core.domain.source.ContentSource
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ContentEntry
import fr.deuspheara.oree.core.model.ContentFeed
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.ContinuationKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Caller-scoped Watch Next only. No shell grants, private endpoints or network fetches. */
class WatchNextContentSource(
    private val context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
    private val clock: Clock = Clock(System::currentTimeMillis),
    private val query: ((Array<String>, CancellationSignal) -> android.database.Cursor?)? = null,
) : ContentSource {
    override val id = ContentSourceId.WATCH_NEXT
    override val changes = providerChanges(context, TvContract.WatchNextPrograms.CONTENT_URI, dispatchers.io)

    override suspend fun loadSnapshot(): ContentFeed = withContext(dispatchers.io) {
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
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
                            "last_engagement_time_utc_millis",
                            "watch_next_type",
                            "intent_uri",
                        )
                    val rows = mutableListOf<ContentEntry>()
                    var seen = 0
                    var stale = false
                    val now = clock.nowMillis()
                    val metadataFields = fields + arrayOf(
                        "episode_title",
                        "episode_display_number",
                        "season_display_number",
                        "duration_millis",
                        "last_playback_position_millis",
                        "thumbnail_uri",
                    )
                    fun read(projection: Array<String>): android.database.Cursor? = if (query != null) {
                        query.invoke(projection, signal)
                    } else {
                        context.contentResolver.query(
                            TvContract.WatchNextPrograms.CONTENT_URI,
                            projection,
                            null,
                            null,
                            "last_engagement_time_utc_millis DESC, _id DESC",
                            signal,
                        )
                    }
                    // Vendor providers may reject optional columns. Preserve the existing feed.
                    val cursor = try {
                        read(metadataFields)
                    } catch (_: IllegalArgumentException) {
                        read(fields)
                    }
                    cursor
                        ?.use { c ->
                            fun text(column: String): String? = c.getColumnIndex(column).takeIf { it >= 0 && !c.isNull(it) }
                                ?.let { c.getString(it) }?.filterNot(Char::isISOControl)?.take(200)?.trim()?.takeIf { it.isNotEmpty() }
                            fun millis(column: String): Long? = c.getColumnIndex(column).takeIf { it >= 0 && !c.isNull(it) }
                                ?.let { c.getLong(it) }?.takeIf { it in 0..604_800_000L }
                            while (seen++ < 200 && c.moveToNext()) {
                                val source = c.getString(2).orEmpty()
                                if (c.getInt(5) !in 0..1) continue
                                val time = c.getLong(4)
                                if (
                                    time <= 0 ||
                                    time > now + 300_000 ||
                                    now - time > 30L * 24 * 60 * 60 * 1000
                                ) {
                                    stale = true
                                    continue
                                }
                                val title =
                                    c.getString(1)
                                        ?.filterNot(Char::isISOControl)
                                        ?.take(200)
                                        ?.trim()
                                        .orEmpty()
                                val link =
                                    c.getString(6)
                                        ?.takeIf {
                                            it.length <= 4096 && it.none(Char::isISOControl)
                                        }
                                        .orEmpty()
                                if (
                                    title.isEmpty() ||
                                    c.getLong(0) < 0 ||
                                    !source.matches(
                                        Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"),
                                    )
                                ) {
                                    continue
                                }
                                val artwork = contentArtworkUri(c.getString(3))
                                    ?: c.getColumnIndex("thumbnail_uri").takeIf { it >= 0 && !c.isNull(it) }
                                        ?.let { contentArtworkUri(c.getString(it)) }
                                // Keep titles for local-only wallpaper diagnostics even when a
                                // link is absent.
                                rows +=
                                    ContentEntry(
                                        "$source:${c.getLong(0)}",
                                        title,
                                        source,
                                        artwork,
                                        link,
                                        time,
                                        episodeTitle = text("episode_title"),
                                        episodeNumber = text("episode_display_number"),
                                        seasonNumber = text("season_display_number"),
                                        durationMillis = millis("duration_millis"),
                                        playbackPositionMillis = millis("last_playback_position_millis"),
                                        continuationKind = if (c.getInt(5) == 0) ContinuationKind.CONTINUE else ContinuationKind.NEXT,
                                    )
                            }
                        } ?: throw IllegalArgumentException("No TV provider")
                    val entries =
                        rows.distinctBy { it.id }.sortedByDescending { it.engagedAt }.take(200)
                    ContentFeed(
                        if (entries.isNotEmpty()) {
                            ContentAvailability.AVAILABLE
                        } else if (stale) {
                            ContentAvailability.STALE
                        } else {
                            ContentAvailability.EMPTY
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
