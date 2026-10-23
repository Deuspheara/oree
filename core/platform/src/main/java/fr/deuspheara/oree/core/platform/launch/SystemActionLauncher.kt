package fr.deuspheara.oree.core.platform.launch

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.TvInput

class SystemActionLauncher(private val activity: Activity) {
    private var launching = false

    fun resumed() {
        launching = false
    }

    fun open(app: TvApp): Boolean {
        if (launching) return true
        val component = ComponentName.unflattenFromString(app.id) ?: return false
        if (
            component.packageName != app.packageName ||
            component.packageName == activity.packageName
        ) {
            return false
        }
        val intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                .setComponent(component)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
                )
        return try {
            launching = true
            activity.startActivity(intent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            launching = false
            false
        } catch (_: SecurityException) {
            launching = false
            false
        }
    }

    fun content(request: Intent): Boolean {
        if (launching) return true
        return try {
            launching = true
            activity.startActivity(request)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            launching = false
            false
        } catch (_: SecurityException) {
            launching = false
            false
        }
    }

    fun input(input: TvInput): Boolean {
        if (launching) return true
        val uri = android.media.tv.TvContract.buildChannelUriForPassthroughInput(input.id)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return try {
            launching = true
            activity.startActivity(intent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            launching = false
            false
        } catch (_: SecurityException) {
            launching = false
            false
        }
    }

    private fun settingsRoute(action: String): Boolean {
        val intent = Intent(action)
        return try {
            activity.startActivity(intent)
            true
        } catch (_: android.content.ActivityNotFoundException) {
            settings()
        } catch (_: SecurityException) {
            settings()
        }
    }

    private fun quickPanel(panel: String, fallback: String): Boolean = try {
        activity.startActivity(Intent(panel))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        settingsRoute(fallback)
    } catch (_: SecurityException) {
        settingsRoute(fallback)
    }

    fun network(): Boolean = if (android.os.Build.VERSION.SDK_INT >= 29) {
        quickPanel(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS)
    } else {
        settingsRoute(Settings.ACTION_WIFI_SETTINGS)
    }

    fun adjustMediaVolume(direction: Int): Boolean = try {
        val audio = activity.getSystemService(android.media.AudioManager::class.java)
        if (audio == null || audio.isVolumeFixed || direction !in -1..1) {
            false
        } else {
            audio.adjustStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                when (direction) {
                    -1 -> android.media.AudioManager.ADJUST_LOWER
                    1 -> android.media.AudioManager.ADJUST_RAISE
                    else -> android.media.AudioManager.ADJUST_TOGGLE_MUTE
                },
                0,
            )
            true
        }
    } catch (_: SecurityException) {
        false
    }

    fun audio(): Boolean = if (android.os.Build.VERSION.SDK_INT >= 29) {
        quickPanel(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS)
    } else {
        settingsRoute(Settings.ACTION_SOUND_SETTINGS)
    }

    fun settings(): Boolean = try {
        activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        true
    } catch (_: android.content.ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

suspend fun resolveAppInformation(context: android.content.Context, app: TvApp): Intent? =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(app.packageName, 0)
            val request =
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.fromParts("package", app.packageName, null),
                )

            @Suppress("DEPRECATION")
            val target =
                context.packageManager
                    .resolveActivity(request, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo ?: return@withContext null
            if (
                !target.exported ||
                !target.enabled ||
                !target.applicationInfo.enabled ||
                target.permission?.let {
                    context.checkSelfPermission(it) !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                } == true
            ) {
                return@withContext null
            }
            request.setComponent(ComponentName(target.packageName, target.name))
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
