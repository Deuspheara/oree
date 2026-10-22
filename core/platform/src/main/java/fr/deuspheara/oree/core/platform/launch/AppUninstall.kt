package fr.deuspheara.oree.core.platform.launch

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Eligibility is checked again immediately before handing control to Android. */
class AppUninstall(private val context: Context) {
    suspend fun intent(packageName: String): Intent? = withContext(Dispatchers.IO) {
        try {
            if (
                packageName == context.packageName ||
                packageName in
                setOf("com.omeda.arc", "com.omeda.arc.local", "com.spocky.projengmenu")
            ) {
                return@withContext null
            }
            val pm = context.packageManager

            @Suppress("DEPRECATION")
            val info = pm.getApplicationInfo(packageName, 0)
            if (
                info.flags and
                (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) !=
                0
            ) {
                return@withContext null
            }
            @Suppress("DEPRECATION")
            val homes =
                pm.queryIntentActivities(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                    android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS,
                )
            if (homes.any { it.activityInfo.packageName == packageName }) {
                return@withContext null
            }
            @Suppress("DEPRECATION")
            val request =
                Intent(
                    Intent.ACTION_UNINSTALL_PACKAGE,
                    Uri.fromParts("package", packageName, null),
                )
                    .putExtra(Intent.EXTRA_RETURN_RESULT, true)
            if (request.resolveActivity(pm) == null) null else request
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
