package fr.deuspheara.oree.core.data.migration

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.deuspheara.oree.core.domain.repository.LegacyPreferencesRepository
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.CatalogSort
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore("launcher")

class DataStoreLegacyPreferences(context: Context) : LegacyPreferencesRepository {
    private val store = context.applicationContext.preferences

    private object Keys {
        val accent = stringPreferencesKey("accent")
        val glass = booleanPreferencesKey("liquid_glass")
        val intensity = floatPreferencesKey("glass_intensity")
        val motion = booleanPreferencesKey("reduce_motion")
        val theme = stringPreferencesKey("theme")
        val quality = stringPreferencesKey("quality")
        val transparency = booleanPreferencesKey("reduce_transparency")
        val contrast = booleanPreferencesKey("high_contrast")
        val text = floatPreferencesKey("text_scale")
        val sideloaded = booleanPreferencesKey("show_sideloaded")
        val context = booleanPreferencesKey("show_context")
        val sort = stringPreferencesKey("catalog_sort")
    }

    override val preferences =
        store.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { data ->
                LauncherPreferences(
                    accent = enumOrDefault(data[Keys.accent], AccentColor.BLUE),
                    liquidGlass = data[Keys.glass] ?: true,
                    glassIntensity = data[Keys.intensity] ?: .75f,
                    theme = enumOrDefault(data[Keys.theme], ThemeMode.DARK),
                    quality = enumOrDefault(data[Keys.quality], QualityTier.BALANCED),
                    reduceMotion = data[Keys.motion] ?: false,
                    reduceTransparency = data[Keys.transparency] ?: false,
                    highContrast = data[Keys.contrast] ?: false,
                    textScale = data[Keys.text] ?: 1f,
                    showSideloaded = data[Keys.sideloaded] ?: false,
                    showContext = data[Keys.context] ?: true,
                    sort = enumOrDefault(data[Keys.sort], CatalogSort.ALPHABETICAL),
                )
                    .normalized()
            }
            .distinctUntilChanged()
    val reduceMotion = preferences.map { it.reduceMotion }.distinctUntilChanged()

    suspend fun update(value: LauncherPreferences) {
        val safe = value.normalized()
        store.edit { data ->
            data[Keys.accent] = safe.accent.name
            data[Keys.glass] = safe.liquidGlass
            data[Keys.intensity] = safe.glassIntensity
            data[Keys.theme] = safe.theme.name
            data[Keys.quality] = safe.quality.name
            data[Keys.motion] = safe.reduceMotion
            data[Keys.transparency] = safe.reduceTransparency
            data[Keys.contrast] = safe.highContrast
            data[Keys.text] = safe.textScale
            data[Keys.sideloaded] = safe.showSideloaded
            data[Keys.context] = safe.showContext
            data[Keys.sort] = safe.sort.name
        }
    }

    suspend fun setReduceMotion(value: Boolean) {
        store.edit { it[Keys.motion] = value }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default
}
