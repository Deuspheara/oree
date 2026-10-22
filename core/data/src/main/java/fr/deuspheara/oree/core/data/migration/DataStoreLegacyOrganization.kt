package fr.deuspheara.oree.core.data.migration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.deuspheara.oree.core.data.serialization.decodeFavoriteApps
import fr.deuspheara.oree.core.domain.repository.LegacyOrganizationRepository
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.setFavoriteApp
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray

private val Context.organizationStore by preferencesDataStore("organization")

/** Small ordered IDs fit DataStore; future folder/profile relations need a separate schema. */
class DataStoreLegacyOrganization constructor(private val store: DataStore<Preferences>) : LegacyOrganizationRepository {
    constructor(context: Context) : this(context.applicationContext.organizationStore)

    private val favoritesKey = stringPreferencesKey("favorite_components_v1")
    override val organization =
        store.data
            .map { data ->
                try {
                    AppOrganization(decodeFavoriteApps(data[favoritesKey]))
                } catch (_: IOException) {
                    AppOrganization(failed = true)
                }
            }
            .catch { if (it is IOException) emit(AppOrganization(failed = true)) else throw it }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)

    suspend fun setFavorite(id: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        store.edit { data ->
            // An unreadable layout cannot be silently overwritten by the next action.
            val current = decodeFavoriteApps(data[favoritesKey])
            val next =
                try {
                    setFavoriteApp(current, id, favorite)
                } catch (error: IllegalArgumentException) {
                    throw IOException("Invalid favorite change", error)
                }
            val encoded = JSONArray(next).toString()
            if (encoded.length > 300_000) throw IOException("Favorite data too large")
            data[favoritesKey] = encoded
        }
    }
}
