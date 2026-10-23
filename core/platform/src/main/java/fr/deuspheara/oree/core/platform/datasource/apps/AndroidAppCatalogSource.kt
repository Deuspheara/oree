package fr.deuspheara.oree.core.platform.datasource.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import fr.deuspheara.oree.core.domain.concurrency.AppDispatchers
import fr.deuspheara.oree.core.domain.source.AppCatalogDataSource
import fr.deuspheara.oree.core.model.ArtworkKind
import fr.deuspheara.oree.core.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext

class AndroidAppCatalogSource(
    context: Context,
    private val dispatchers: AppDispatchers = AppDispatchers(Dispatchers.IO, Dispatchers.Default),
) : AppCatalogDataSource {
    private val context = context.applicationContext

    // API <33 registers only protected package broadcasts; the API 33 branch supplies flags.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    override val changes = callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    trySend(Unit)
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        trySend(Unit)
        awaitClose { context.unregisterReceiver(receiver) }
    }
        .conflate()

    override suspend fun load(): List<TvApp> = withContext(dispatchers.io) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val resolved =
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        val revisions = mutableMapOf<String, Long>()
        resolved
            .asSequence()
            .filter { it.activityInfo.exported && it.activityInfo.enabled }
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull {
                val info = it.activityInfo
                val revision =
                    try {
                        revisions.getOrPut(info.packageName) {
                            @Suppress("DEPRECATION")
                            pm.getPackageInfo(info.packageName, 0).lastUpdateTime
                        }
                    } catch (_: PackageManager.NameNotFoundException) {
                        return@mapNotNull null // Removed between the query and metadata read.
                    }
                TvApp(
                    android.content
                        .ComponentName(info.packageName, info.name)
                        .flattenToString(),
                    info.packageName,
                    it.loadLabel(pm).toString(),
                    info.banner.takeIf { id -> id != 0 }
                        ?: info.applicationInfo.banner.takeIf { id -> id != 0 }
                        ?: info.icon.takeIf { id -> id != 0 }
                        ?: info.applicationInfo.icon,
                    revision,
                    if (info.banner != 0 || info.applicationInfo.banner != 0) {
                        ArtworkKind.BANNER
                    } else {
                        ArtworkKind.ICON
                    },
                    icon = info.applicationInfo.icon.takeIf { id -> id != 0 } ?: info.icon,
                )
            }
            .distinctBy { it.id }
            .toList()
    }
}
