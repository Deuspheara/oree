package fr.deuspheara.oree.core.platform.image

import android.graphics.Bitmap
import android.net.Uri
import fr.deuspheara.oree.core.model.LocalProfile

data class WallpaperFrame(val image: Bitmap, val frost: Bitmap, val tint: Int)

interface WallpaperRenderer {
    suspend fun load(profile: LocalProfile, dynamic: Uri? = null): WallpaperFrame
}
