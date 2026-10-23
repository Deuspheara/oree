package fr.deuspheara.oree.core.data.serialization

import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.HomeContentLayout
import fr.deuspheara.oree.core.model.MAX_FAVORITE_APPS
import fr.deuspheara.oree.core.model.ProfileSnapshot
import fr.deuspheara.oree.core.model.validFolderComponent
import java.io.IOException
import org.json.JSONObject

const val MAX_BACKUP_BYTES = 4 * 1024 * 1024

fun encodeBackup(snapshot: ProfileSnapshot): String {
    if (snapshot.failed) throw IOException("Profiles unavailable")
    return JSONObject()
        .put("format", "fr.deuspheara.oree.backup")
        .put("version", 1)
        .put("profiles", JSONObject(encodeProfiles(snapshot)))
        .put("wallpaperAssets", "references-only")
        .toString()
        .also {
            if (it.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
                throw IOException("Backup too large")
            }
        }
}

fun decodeBackup(raw: String): ProfileSnapshot {
    if (raw.toByteArray(Charsets.UTF_8).size > MAX_BACKUP_BYTES) {
        throw IOException("Backup too large")
    }
    // Reject excessive nesting before the recursive platform JSON parser sees untrusted data.
    var depth = 0
    var quoted = false
    var escaped = false
    raw.forEach { char ->
        if (quoted) {
            if (escaped) {
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else if (char == '"') {
                quoted = false
            }
        } else {
            when (char) {
                '"' -> quoted = true

                '{',
                '[',
                -> {
                    depth++
                    if (depth > 24) throw IOException("Backup nesting too deep")
                }

                '}',
                ']',
                -> {
                    depth--
                    if (depth < 0) throw IOException("Malformed backup")
                }
            }
        }
    }
    try {
        val json = JSONObject(raw)
        if (
            json.getString("format") !in
            setOf("fr.deuspheara.oree.backup", "org.opensource.tvlauncher.backup") ||
            json.get("version") != 1 ||
            json.getString("wallpaperAssets") != "references-only"
        ) {
            throw IOException("Unsupported backup format")
        }
        val data = json.getJSONObject("profiles")
        if (data.get("version") !is Int) throw IOException("Invalid profile schema")
        validateImportedValues(data)
        val snapshot = decodeProfiles(data.toString())
        if (
            snapshot.profiles.any { profile -> profile.favorites.any { !validFolderComponent(it) } }
        ) {
            throw IOException("Invalid favorite component")
        }
        return snapshot
    } catch (error: org.json.JSONException) {
        throw IOException("Malformed backup", error)
    }
}

private fun validateImportedValues(data: JSONObject) {
    val rows = data.getJSONArray("profiles")
    if (rows.length() !in 1..12) throw IOException("Invalid profile count")
    fun number(objectValue: JSONObject, key: String, min: Double, max: Double, integer: Boolean = false) {
        if (!objectValue.has(key)) return
        val value =
            objectValue.get(key) as? Number ?: throw IOException("Invalid numeric preference")
        val number = value.toDouble()
        if (!number.isFinite() || number !in min..max || (integer && number % 1.0 != 0.0)) {
            throw IOException("Preference out of bounds")
        }
    }
    repeat(rows.length()) { index ->
        val profile = rows.getJSONObject(index)
        listOf("content", "watchNext", "appPreviews", "ambientPhotos", "ambientClock").forEach { key ->
            if (profile.has(key) && profile.get(key) !is Boolean) {
                throw IOException("Invalid source preference")
            }
        }
        number(profile, "landscape", 0.0, 1.0, true)
        val favorites = profile.getJSONArray("favorites")
        if (favorites.length() > MAX_FAVORITE_APPS) throw IOException("Too many favorites")
        val ids =
            (0 until favorites.length()).map {
                favorites.get(it) as? String ?: throw IOException("Invalid component")
            }
        if (ids.size != ids.distinct().size || ids.any { !validFolderComponent(it) }) {
            throw IOException("Invalid favorites")
        }
        val settings = profile.getJSONObject("settings")
        listOf("motion", "transparency", "contrast", "sideloaded", "context", "liquidGlass", "setupComplete")
            .forEach { key ->
                if (settings.has(key) && settings.get(key) !is Boolean) {
                    throw IOException("Invalid appearance preference")
                }
            }
        number(settings, "text", 1.0, 2.0)
        number(settings, "glassIntensity", .25, 1.0)
        number(settings, "backdropBlur", 0.0, 1.0)
        number(settings, "motionIntensity", 0.0, 1.0)
        if (settings.has("homeContentLayout") && HomeContentLayout.entries.none {
                it.name == settings.get("homeContentLayout")
            }
        ) {
            throw IOException("Invalid Home content layout")
        }
        listOf("controls", "controlPositions").forEach { field ->
            if (settings.has(field)) {
                val controls = settings.getJSONObject(field)
                controls.keys().forEach { key ->
                    if (ControlId.entries.none { it.name == key }) {
                        throw IOException("Unknown control")
                    }
                    val values = controls.getJSONArray(key)
                    if (values.length() != 2) throw IOException("Invalid control geometry")
                    repeat(2) { axis ->
                        val value =
                            values.get(axis) as? Int
                                ?: throw IOException("Invalid control coordinate")
                        if (value !in (if (field == "controls") 1..2 else 0..3)) {
                            throw IOException("Control coordinate out of bounds")
                        }
                    }
                }
            }
        }
    }
}
