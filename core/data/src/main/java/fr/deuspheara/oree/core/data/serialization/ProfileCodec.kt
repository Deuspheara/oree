package fr.deuspheara.oree.core.data.serialization

import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.AppFolder
import fr.deuspheara.oree.core.model.AvatarIds
import fr.deuspheara.oree.core.model.CatalogSort
import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.ControlSize
import fr.deuspheara.oree.core.model.HomeContentLayout
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.MAX_APP_FOLDERS
import fr.deuspheara.oree.core.model.MAX_FAVORITE_APPS
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.model.validFolderComponent
import fr.deuspheara.oree.core.model.validFolderName
import java.io.IOException
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.none
import org.json.JSONArray
import org.json.JSONObject

fun validName(name: String): String = name.trim().also {
    if (it.isEmpty() || it.length > 40 || it.any(Char::isISOControl)) {
        throw IOException("Invalid profile name")
    }
}

fun encodeProfiles(snapshot: ProfileSnapshot): String {
    val profiles = JSONArray()
    snapshot.profiles.forEach { p ->
        val v = p.preferences.normalized()
        val settings =
            JSONObject()
                .put("theme", v.theme.name)
                .put("quality", v.quality.name)
                .put("motion", v.reduceMotion)
                .put("transparency", v.reduceTransparency)
                .put("contrast", v.highContrast)
                .put("accent", v.accent.name)
                .put(
                    "controls",
                    JSONObject().apply {
                        v.controls.forEach { (id, size) ->
                            put(id.name, JSONArray(listOf(size.columns, size.rows)))
                        }
                    },
                )
                .put(
                    "controlPositions",
                    JSONObject().apply {
                        v.controlPositions.forEach { (id, position) ->
                            put(id.name, JSONArray(listOf(position.column, position.row)))
                        }
                    },
                )
                .put("liquidGlass", v.liquidGlass)
                .put("glassIntensity", v.glassIntensity)
                .put("backdropBlur", v.backdropBlur)
                .put("motionIntensity", v.motionIntensity)
                .put("setupComplete", v.setupComplete)
                .put("homeContentLayout", v.homeContentLayout.name)
                .put("text", v.textScale)
                .put("sideloaded", v.showSideloaded)
                .put("context", v.showContext)
                .put("sort", v.sort.name)
        profiles.put(
            JSONObject()
                .put("id", p.id)
                .put("name", p.name)
                .put("avatarId", p.avatar)
                .put("favorites", JSONArray(p.favorites))
                .put(
                    "folders",
                    JSONArray().apply {
                        p.folders.forEach { folder ->
                            put(
                                JSONObject()
                                    .put("id", folder.id)
                                    .put("name", folder.name)
                                    .put("apps", JSONArray(folder.apps)),
                            )
                        }
                    },
                )
                .put("settings", settings)
                .put("wallpaper", p.wallpaper.name)
                .put(
                    "landscape",
                    p.landscape,
                )
                .put("image", p.image ?: JSONObject.NULL)
                .put("content", p.contentEnabled)
                .put("watchNext", p.watchNextEnabled)
                .put("appPreviews", p.appPreviewsEnabled)
                .put("ambientPhotos", p.ambientPhotos)
                .put("ambientClock", p.ambientClock),
        )
    }
    val encoded =
        JSONObject()
            .put("version", 3)
            .put("active", snapshot.activeId)
            .put("profiles", profiles)
            .toString()
    decodeProfiles(encoded) // Apply the same bounds to writes and reads.
    return encoded
}

fun decodeProfiles(raw: String): ProfileSnapshot {
    try {
        if (raw.length > 4_000_000) throw IOException("Profiles too large")
        val root = JSONObject(raw)
        val version = root.getInt("version")
        if (version !in 1..3) throw IOException("Unsupported profile version")
        val rows = root.getJSONArray("profiles")
        if (rows.length() !in 1..12) throw IOException("Invalid profile count")
        val profiles =
            (0 until rows.length()).map { i ->
                val p = rows.getJSONObject(i)
                val v = p.getJSONObject("settings")
                val id = p.getString("id")
                if (!id.matches(Regex("[a-zA-Z0-9-]{1,64}"))) {
                    throw IOException("Invalid profile ID")
                }
                val image = if (p.isNull("image")) null else p.getString("image")
                if (image != null && !image.matches(Regex("wallpaper-[a-f0-9-]+[.]jpg"))) {
                    throw IOException("Invalid image reference")
                }
                val avatar =
                    if (version == 1) {
                        AvatarIds.fromLegacy(p.getInt("avatar"))
                            ?: throw IOException("Invalid legacy avatar")
                    } else {
                        p.getString("avatarId")
                    }
                val landscape = p.getInt("landscape")
                if (!AvatarIds.isKnown(avatar) || landscape !in 0..1) {
                    throw IOException("Invalid appearance")
                }
                LocalProfile(
                    id,
                    validName(p.getString("name")),
                    avatar,
                    decodeFavoriteApps(p.getJSONArray("favorites").toString()),
                    LauncherPreferences(
                        ThemeMode.valueOf(v.getString("theme")),
                        QualityTier.valueOf(v.getString("quality")),
                        v.getBoolean("motion"),
                        v.getBoolean("transparency"),
                        v.getBoolean("contrast"),
                        v.getDouble("text").toFloat(),
                        v.getBoolean("sideloaded"),
                        v.getBoolean("context"),
                        CatalogSort.valueOf(v.getString("sort")),
                        liquidGlass = v.optBoolean("liquidGlass", true),
                        glassIntensity = v.optDouble("glassIntensity", .75).toFloat(),
                        backdropBlur = v.optDouble("backdropBlur", 1.0).toFloat(),
                        motionIntensity = v.optDouble("motionIntensity", 1.0).toFloat(),
                        setupComplete = v.optBoolean("setupComplete", true),
                        homeContentLayout = HomeContentLayout.entries.firstOrNull {
                            it.name == v.optString("homeContentLayout")
                        } ?: HomeContentLayout.BELOW_DOCK,
                        accent =
                            AccentColor.entries.firstOrNull {
                                it.name == v.optString("accent")
                            } ?: AccentColor.BLUE,
                        controls =
                            ControlId.entries
                                .mapNotNull { control ->
                                    v.optJSONObject("controls")
                                        ?.optJSONArray(control.name)
                                        ?.let { size ->
                                            control to
                                                ControlSize(
                                                    size.optInt(0, 1),
                                                    size.optInt(1, 1),
                                                )
                                                    .normalized()
                                        }
                                }
                                .toMap(),
                        controlPositions =
                            ControlId.entries
                                .mapNotNull { control ->
                                    v.optJSONObject("controlPositions")
                                        ?.optJSONArray(control.name)
                                        ?.let { position ->
                                            control to
                                                ControlPosition(
                                                    position.optInt(0, 0),
                                                    position.optInt(1, 0),
                                                )
                                        }
                                }
                                .toMap(),
                    )
                        .normalized(),
                    WallpaperMode.valueOf(p.getString("wallpaper")),
                    landscape,
                    image,
                    p.getBoolean("content"),
                    p.optBoolean("watchNext", false),
                    if (version < 3) emptyList() else decodeFolders(p.getJSONArray("folders")),
                    p.optBoolean("appPreviews", false),
                    p.optBoolean("ambientPhotos", false),
                    p.optBoolean("ambientClock", true),
                )
                    .also { profile ->
                        if (profile.folders.flatMap { it.apps }.any { it in profile.favorites }) {
                            throw IOException("Duplicate Home membership")
                        }
                    }
            }
        val active = root.getString("active")
        if (
            profiles.map { it.id }.distinct().size != profiles.size ||
            profiles.none { it.id == active }
        ) {
            throw IOException("Invalid active profile")
        }
        return ProfileSnapshot(active, profiles)
    } catch (e: org.json.JSONException) {
        throw IOException("Malformed profiles", e)
    } catch (e: IllegalArgumentException) {
        throw IOException("Invalid profiles", e)
    }
}

private fun decodeFolders(rows: JSONArray): List<AppFolder> {
    if (rows.length() > MAX_APP_FOLDERS) throw IOException("Too many folders")
    val folders =
        (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val id = row.getString("id")
            if (!id.matches(Regex("[a-zA-Z0-9-]{1,64}"))) throw IOException("Invalid folder ID")
            val members = row.getJSONArray("apps")
            val apps = decodeFavoriteApps(members.toString())
            if (apps.size != members.length()) throw IOException("Duplicate folder component")
            if (apps.any { !validFolderComponent(it) }) {
                throw IOException("Invalid folder component")
            }
            AppFolder(id, validFolderName(row.getString("name")), apps)
        }
    val members = folders.flatMap { it.apps }
    if (
        folders
            .map {
                it.id
            }
            .distinct()
            .size != folders.size ||
        members.size > MAX_FAVORITE_APPS ||
        members.distinct().size != members.size
    ) {
        throw IOException("Invalid folder membership")
    }
    return folders
}
