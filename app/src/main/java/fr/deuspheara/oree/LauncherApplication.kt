package fr.deuspheara.oree

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import fr.deuspheara.oree.di.launcherModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication

class LauncherApplication :
    Application(),
    SingletonImageLoader.Factory {
    /** Isolated application-owned container, never a global mutable service locator. */
    val dependencies by lazy {
        koinApplication {
            androidContext(this@LauncherApplication)
            modules(launcherModule)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = dependencies.koin.get()

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            dependencies.koin.get<ArtworkReflections>().clear()
        }
    }
}
