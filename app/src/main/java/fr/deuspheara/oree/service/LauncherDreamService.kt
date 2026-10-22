package fr.deuspheara.oree.service

import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextClock
import fr.deuspheara.oree.LauncherApplication
import fr.deuspheara.oree.core.domain.repository.ProfileRepository
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.platform.image.WallpaperRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Android selects/starts this ordinary screensaver. No privileged permission is requested. */
class LauncherDreamService : DreamService() {
    private var work: CoroutineScope? = null
    private var image: ImageView? = null
    private var clock: TextClock? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        val scene = FrameLayout(this).apply { setBackgroundColor(android.graphics.Color.BLACK) }
        image = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        scene.addView(image, FrameLayout.LayoutParams(-1, -1))
        clock =
            TextClock(this).apply {
                setTextColor(android.graphics.Color.WHITE)
                visibility = View.GONE
            }
        scene.addView(
            clock,
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply {
                val margin = (32 * resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            },
        )
        setContentView(scene)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        work?.cancel()
        work =
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope ->
                scope.launch {
                    val graph = (application as LauncherApplication).dependencies.koin
                    val profile =
                        graph.get<ProfileRepository>().snapshots.first()?.active
                            ?: LocalProfile("default", "Home")
                    // Dynamic app/content wallpapers fall back locally; screensaver never queries
                    // history.
                    val local =
                        if (
                            profile.wallpaper in
                            listOf(WallpaperMode.APPLICATION, WallpaperMode.CONTENT)
                        ) {
                            profile.copy(wallpaper = WallpaperMode.LANDSCAPE)
                        } else {
                            profile
                        }
                    val frame = graph.get<WallpaperRenderer>().load(local)
                    image?.setImageBitmap(frame.image)
                    clock?.apply {
                        textSize = 24f * profile.preferences.textScale
                        visibility = if (profile.ambientClock) View.VISIBLE else View.GONE
                    }
                }
            }
    }

    override fun onDreamingStopped() {
        work?.cancel()
        work = null
        image?.setImageDrawable(null)
        clock?.visibility = View.GONE
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        work?.cancel()
        work = null
        image = null
        clock = null
        super.onDetachedFromWindow()
    }
}

suspend fun resolveDreamSettings(context: android.content.Context): android.content.Intent? = withContext(Dispatchers.IO) {
    try {
        val request = android.content.Intent(android.provider.Settings.ACTION_DREAM_SETTINGS)

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
        request.setComponent(android.content.ComponentName(target.packageName, target.name))
    } catch (_: SecurityException) {
        null
    }
}
