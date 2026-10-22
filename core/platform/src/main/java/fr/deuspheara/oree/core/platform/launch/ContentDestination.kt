package fr.deuspheara.oree.core.platform.launch

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Never forward provider-supplied extras, grants, selectors or task flags. */
fun contentIntent(raw: String, source: String): Intent? {
    if (
        raw.length !in 1..4096 ||
        raw.any(Char::isISOControl) ||
        !source.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))
    ) {
        return null
    }
    return try {
        val parsed =
            if (raw.startsWith("intent:")) {
                Intent.parseUri(raw, Intent.URI_INTENT_SCHEME or Intent.URI_ALLOW_UNSAFE)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(raw))
            }
        if (
            parsed.action != Intent.ACTION_VIEW ||
            parsed.selector != null ||
            parsed.clipData != null ||
            parsed.flags and
            (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                ) != 0 ||
            parsed.component?.packageName?.let { it != source } == true ||
            parsed.`package`?.let { it != source } == true
        ) {
            return null
        }
        val data = parsed.data ?: return null
        val scheme = data.scheme?.lowercase() ?: return null
        if (
            !scheme.matches(Regex("[a-z][a-z0-9+.-]*")) ||
            scheme in
            setOf(
                "file",
                "content",
                "android.resource",
                "intent",
                "javascript",
                "data",
                "package",
                "tel",
                "sms",
                "mailto",
            )
        ) {
            return null
        }
        if (scheme in setOf("https", "http") && data.host.isNullOrBlank()) return null
        Intent(Intent.ACTION_VIEW, data).setPackage(source).apply {
            parsed.component?.let(::setComponent)
        }
    } catch (_: java.net.URISyntaxException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

suspend fun resolveContent(context: Context, raw: String, source: String): Intent? = withContext(Dispatchers.IO) {
    resolveContentOnWorker(context, raw, source)
}

/** Caller must already be on a worker; shared by the bounded capability probe. */
fun resolveContentOnWorker(context: Context, raw: String, source: String): Intent? {
    val request = contentIntent(raw, source) ?: return null
    try {
        @Suppress("DEPRECATION")
        val resolved =
            context.packageManager.resolveActivity(request, 0)?.activityInfo ?: return null
        return if (
            resolved.packageName != source ||
            !resolved.exported ||
            !resolved.enabled ||
            !resolved.applicationInfo.enabled ||
            resolved.permission?.let {
                context.checkSelfPermission(it) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            } == true
        ) {
            null
        } else {
            request.setComponent(android.content.ComponentName(resolved.packageName, resolved.name))
        }
    } catch (_: SecurityException) {
        return null
    }
}
