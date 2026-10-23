package fr.deuspheara.oree.core.platform.datasource.content

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn

fun providerChanges(context: Context, uri: Uri, io: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO): Flow<Unit> = callbackFlow {
    val observer =
        object :
            android.database.ContentObserver(
                android.os.Handler(android.os.Looper.getMainLooper()),
            ) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
    var registered = false
    try {
        context.contentResolver.registerContentObserver(uri, true, observer)
        registered = true
    } catch (_: SecurityException) {} catch (_: IllegalArgumentException) {}
    trySend(Unit)
    awaitClose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
}
    .conflate()
    .flowOn(io)

/** Only public provider artwork; remote sources are opt-in, HTTPS, without embedded credentials. */
internal fun contentArtworkUri(raw: String?): String? {
    if (raw.isNullOrBlank() || raw.length > 4096 || raw.any(Char::isISOControl)) return null
    val uri = Uri.parse(raw)
    return when (uri.scheme) {
        "content", "android.resource" -> raw.takeIf { !uri.authority.isNullOrBlank() }
        "https" -> raw.takeIf { !uri.host.isNullOrBlank() && uri.userInfo == null }
        else -> null
    }
}
