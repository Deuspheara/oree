package fr.deuspheara.oree.core.platform.datasource.content

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.os.CancellationSignal
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.source.CapabilityDataSource
import fr.deuspheara.oree.core.model.ContentCapabilities
import fr.deuspheara.oree.core.model.ContentCapability
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.ProbeAvailability
import fr.deuspheara.oree.core.platform.launch.resolveContentOnWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Explicit ordinary-caller inspection. Results contain counts, never personal titles/links. */
class AndroidContentCapabilitySource(
    private val context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
    private val query: ((Uri, Array<String>, CancellationSignal) -> Cursor?)? = null,
) : CapabilityDataSource {
    override suspend fun inspect(): ContentCapabilities {
        val providers = ContentSourceId.entries.map { inspect(it) }
        return withContext(dispatchers.io) {
            var candidates = 0
            var exported = 0
            var searchState = ProbeAvailability.UNSUPPORTED
            try {
                val search = context.getSystemService(SearchManager::class.java)
                val sources = search?.searchablesInGlobalSearch.orEmpty().take(100)
                candidates = sources.size
                if (search != null) {
                    searchState =
                        if (candidates == 0) {
                            ProbeAvailability.EMPTY
                        } else {
                            ProbeAvailability.AVAILABLE
                        }
                }
                exported = sources.count { source ->
                    val authority = source.suggestAuthority ?: return@count false

                    @Suppress("DEPRECATION")
                    val provider =
                        context.packageManager.resolveContentProvider(authority, 0)
                            ?: return@count false
                    provider.exported &&
                        provider.enabled &&
                        provider.applicationInfo.enabled &&
                        (
                            provider.readPermission == null ||
                                context.checkSelfPermission(provider.readPermission) ==
                                PackageManager.PERMISSION_GRANTED
                            )
                }
            } catch (_: SecurityException) {
                searchState = ProbeAvailability.DENIED
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                searchState = ProbeAvailability.ERROR
            }
            ContentCapabilities(
                providers,
                context.checkSelfPermission("android.permission.READ_TV_LISTINGS") ==
                    PackageManager.PERMISSION_GRANTED,
                resolveSystemSearch(context) != null,
                candidates,
                exported,
                searchState,
            )
        }
    }

    suspend fun inspect(source: ContentSourceId): ContentCapability = withContext(dispatchers.io) {
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            val uri =
                when (source) {
                    ContentSourceId.CHANNELS -> TvContract.Channels.CONTENT_URI
                    ContentSourceId.PREVIEW -> TvContract.PreviewPrograms.CONTENT_URI
                    ContentSourceId.WATCH_NEXT -> TvContract.WatchNextPrograms.CONTENT_URI
                    ContentSourceId.PROGRAMS -> TvContract.Programs.CONTENT_URI
                }
            val title = if (source == ContentSourceId.CHANNELS) "display_name" else "title"
            val art =
                if (source == ContentSourceId.CHANNELS) {
                    "app_link_poster_art_uri"
                } else {
                    "poster_art_uri"
                }
            val link =
                when (source) {
                    ContentSourceId.CHANNELS -> "app_link_intent_uri"
                    ContentSourceId.PROGRAMS -> null
                    else -> "intent_uri"
                }
            val fields =
                (listOf("_id", "package_name", title, art) + listOfNotNull(link)).toTypedArray()
            val result =
                try {
                    var rows = 0
                    var metadata = 0
                    var artwork = 0
                    var launchable = 0
                    var cross = 0
                    var readable: Boolean? = null
                    val cursor =
                        if (query != null) {
                            query.invoke(uri, fields, signal)
                        } else {
                            context.contentResolver.query(uri, fields, null, null, null, signal)
                        }
                    cursor?.use { c ->
                        fun value(column: String): String? = c.getColumnIndex(column)
                            .takeIf { it >= 0 }
                            ?.let { index ->
                                c.getString(index)?.takeIf {
                                    it.length <= 4096 && it.none(Char::isISOControl)
                                }
                            }
                        while (rows < 40 && c.moveToNext()) {
                            signal.throwIfCanceled()
                            rows++
                            val publisher = value("package_name").orEmpty()
                            if (
                                publisher.matches(
                                    Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"),
                                ) && publisher != context.packageName
                            ) {
                                cross++
                            }
                            if (!value(title).isNullOrBlank()) metadata++
                            val local =
                                value(art)?.let(Uri::parse)?.takeIf {
                                    it.scheme in listOf("content", "android.resource")
                                }
                            if (local != null) {
                                artwork++
                                if (readable == null) {
                                    readable =
                                        try {
                                            context.contentResolver
                                                .openAssetFileDescriptor(local, "r", signal)
                                                ?.use { true } ?: false
                                        } catch (_: java.io.IOException) {
                                            false
                                        } catch (_: SecurityException) {
                                            false
                                        } catch (_: IllegalArgumentException) {
                                            false
                                        }
                                }
                            }
                            if (
                                link != null &&
                                value(link)?.let {
                                    resolveContentOnWorker(context, it, publisher)
                                } != null
                            ) {
                                launchable++
                            }
                        }
                    } ?: throw IllegalArgumentException("Provider unavailable")
                    ContentCapability(
                        source,
                        if (rows == 0) ProbeAvailability.EMPTY else ProbeAvailability.AVAILABLE,
                        rows,
                        metadata,
                        artwork,
                        readable,
                        launchable,
                        cross,
                        rows == 40,
                    )
                } catch (_: SecurityException) {
                    ContentCapability(source, ProbeAvailability.DENIED)
                } catch (_: IllegalArgumentException) {
                    ContentCapability(source, ProbeAvailability.UNSUPPORTED)
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    ContentCapability(source, ProbeAvailability.ERROR)
                }
            if (continuation.isActive) continuation.resumeWith(Result.success(result))
        }
    }
}

/** Query public handlers on IO; always revalidate at dispatch. No package-specific adapter. */
suspend fun resolveSystemSearch(context: Context, query: String = ""): Intent? = withContext(Dispatchers.IO) {
    try {
        val request =
            Intent(SearchManager.INTENT_ACTION_GLOBAL_SEARCH)
                .putExtra(SearchManager.QUERY, query.take(120))

        @Suppress("DEPRECATION")
        val target =
            context.packageManager
                .resolveActivity(request, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo ?: return@withContext null
        if (
            !target.exported ||
            !target.enabled ||
            !target.applicationInfo.enabled ||
            target.permission?.let {
                context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            } == true
        ) {
            return@withContext null
        }
        request.setComponent(ComponentName(target.packageName, target.name))
    } catch (_: SecurityException) {
        null
    }
}
