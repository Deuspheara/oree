package fr.deuspheara.oree

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.repository.DataStoreProfileRepository
import fr.deuspheara.oree.core.model.AccentColor
import fr.deuspheara.oree.core.model.AppOrganization
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.model.ControlId
import fr.deuspheara.oree.core.model.ControlPosition
import fr.deuspheara.oree.core.model.ControlSize
import fr.deuspheara.oree.core.model.LauncherPreferences
import fr.deuspheara.oree.core.model.LocalProfile
import fr.deuspheara.oree.core.model.QualityTier
import fr.deuspheara.oree.core.model.ThemeMode
import fr.deuspheara.oree.core.model.WallpaperMode
import fr.deuspheara.oree.core.platform.datasource.content.WatchNextContentSource
import fr.deuspheara.oree.core.platform.image.impl.AndroidWallpaperStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProfilesAndWallpaperTest {
    private suspend fun isolated(
        test:
        suspend (
            DataStoreProfileRepository,
            androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        ) -> Unit,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file =
            File(context.cacheDir, "profile-test-${java.util.UUID.randomUUID()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        try {
            test(DataStoreProfileRepository(store), store)
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }

    @Test
    fun migrationIsIdempotentAndProfilesAreIsolated() = runBlocking {
        isolated { repo, _ ->
            val legacy =
                LauncherPreferences(
                    theme = ThemeMode.SYSTEM,
                    quality = QualityTier.BALANCED,
                    textScale = 1.25f,
                )
            repo.initialize(legacy, AppOrganization(listOf("canal/.TV", "netflix/.Home")))
            repo.initialize(LauncherPreferences(), AppOrganization())
            val original = repo.snapshots.first()!!.active!!
            assertEquals(legacy, original.preferences)
            assertEquals(listOf("canal/.TV", "netflix/.Home"), original.favorites)
            repo.create("Second", "avatar_03")
            val second = repo.snapshots.first()!!.active!!
            assertTrue(second.favorites.isEmpty())
            repo.setFavorite(second.id, "other/.TV", true)
            repo.setWallpaper(second.id, WallpaperMode.APPLICATION)
            repo.setPreferences(second.id, second.preferences.copy(reduceMotion = true))
            repo.select(original.id)
            assertEquals(original, repo.snapshots.first()!!.active)
            repo.delete(second.id)
            assertEquals(1, repo.snapshots.first()!!.profiles.size)
            try {
                repo.delete(original.id)
                fail("Last profile deletion must fail")
            } catch (_: IOException) {}
            assertEquals(original, repo.snapshots.first()!!.active)
        }
    }

    @Test
    fun avatarIdsMigratePersistAndRejectUnknownAtomically() = runBlocking {
        isolated { repo, store ->
            repo.initialize(LauncherPreferences(), AppOrganization())
            val first = repo.snapshots.first()!!.activeId
            val key = stringPreferencesKey("profiles_v1")
            // Actual old on-disk record; loading is idempotent and does not rewrite until an edit.
            store.edit { values ->
                val json = org.json.JSONObject(values[key]!!).put("version", 1)
                json.getJSONArray("profiles").getJSONObject(0).apply {
                    remove("avatarId")
                    put("avatar", 5)
                }
                values[key] = json.toString()
            }
            assertEquals("avatar_06", repo.snapshots.first()!!.active!!.avatar)
            repo.rename(first, "First", "avatar_08")
            assertEquals(3, org.json.JSONObject(store.data.first()[key]!!).getInt("version"))
            assertFalse(
                org.json
                    .JSONObject(store.data.first()[key]!!)
                    .getJSONArray("profiles")
                    .getJSONObject(0)
                    .has("avatar"),
            )
            repo.create("Second", "avatar_07")
            val second = repo.snapshots.first()!!.activeId
            repo.select(first)
            assertEquals(
                "avatar_08",
                DataStoreProfileRepository(store).snapshots.first()!!.active!!.avatar,
            )
            repo.select(second)
            assertEquals(
                "avatar_07",
                DataStoreProfileRepository(store).snapshots.first()!!.active!!.avatar,
            )
            val before = store.data.first()[key]
            try {
                repo.rename(second, "Wrong", "../../portrait")
                fail("Unknown avatar accepted")
            } catch (_: IOException) {}
            assertEquals(before, store.data.first()[key])
        }
    }

    @Test
    fun glassSettingsRoundTripAndOldProfilesRetainDefaults() = runBlocking {
        isolated { repo, store ->
            repo.initialize(LauncherPreferences(), AppOrganization())
            val id = repo.snapshots.first()!!.activeId
            repo.setPreferences(
                id,
                repo.snapshots
                    .first()!!
                    .active!!
                    .preferences
                    .copy(
                        liquidGlass = false,
                        glassIntensity = .25f,
                        accent = AccentColor.GREEN,
                        controls = mapOf(ControlId.NETWORK to ControlSize(1, 2)),
                        controlPositions = mapOf(ControlId.NETWORK to ControlPosition(3, 2)),
                    ),
            )
            val saved = repo.snapshots.first()!!.active!!.preferences
            assertFalse(saved.liquidGlass)
            assertEquals(.25f, saved.glassIntensity)
            assertEquals(ControlPosition(3, 2), saved.controlPositions[ControlId.NETWORK])
            assertEquals(AccentColor.GREEN, saved.accent)
            assertEquals(ControlSize(1, 2), saved.controls[ControlId.NETWORK])
            store.edit { values ->
                val key = stringPreferencesKey("profiles_v1")
                val json = org.json.JSONObject(values[key]!!)
                val settings =
                    json.getJSONArray("profiles").getJSONObject(0).getJSONObject("settings")
                settings.remove("liquidGlass")
                settings.remove("glassIntensity")
                settings.remove("accent")
                settings.remove("controls")
                settings.remove("controlPositions")
                values[key] = json.toString()
            }
            assertTrue(repo.snapshots.first()!!.active!!.preferences.liquidGlass)
            assertEquals(AccentColor.BLUE, repo.snapshots.first()!!.active!!.preferences.accent)
            assertTrue(repo.snapshots.first()!!.active!!.preferences.controls.isEmpty())
            assertTrue(repo.snapshots.first()!!.active!!.preferences.controlPositions.isEmpty())
            assertEquals(.75f, repo.snapshots.first()!!.active!!.preferences.glassIntensity)
        }
    }

    @Test
    fun unreadableAndFutureSchemasCannotBeOverwritten() = runBlocking {
        isolated { repo, store ->
            val raw = "{\"version\":99}"
            store.edit { it[stringPreferencesKey("profiles_v1")] = raw }
            assertTrue(repo.snapshots.first()!!.failed)
            try {
                repo.create("New", "avatar_01")
                fail("Corrupt storage must be retained")
            } catch (_: IOException) {}
            assertEquals(raw, store.data.first()[stringPreferencesKey("profiles_v1")])
        }
    }

    @Test
    fun invalidProfileEditsAreAtomic() = runBlocking {
        isolated { repo, _ ->
            repo.initialize(LauncherPreferences(), AppOrganization())
            val before = repo.snapshots.first()!!
            try {
                repo.rename(before.activeId, " ", "avatar_01")
                fail("Empty name")
            } catch (_: IOException) {}
            assertEquals(before, repo.snapshots.first())
            try {
                repo.setImage(before.activeId, "../outside.jpg")
                fail("Unsafe image reference")
            } catch (_: IOException) {}
            assertEquals(before, repo.snapshots.first())
        }
    }

    @Test
    fun wallpaperFallbackAndInvalidImportKeepExistingProfile() = runBlocking {
        isolated { repo, _ ->
            repo.initialize(LauncherPreferences(), AppOrganization())
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val wallpapers = AndroidWallpaperStore(context)
            val profile = repo.snapshots.first()!!.active!!
            try {
                wallpapers.importImage(android.net.Uri.parse("file:///untrusted.jpg"))
                fail("Document URI required")
            } catch (_: IOException) {}
            val image =
                wallpapers.load(
                    profile.copy(wallpaper = WallpaperMode.PERSONAL, image = "wallpaper-000.jpg"),
                )
            assertTrue(image.image.width <= 1280)
            assertTrue(image.image.height <= 720)
            assertEquals(profile, repo.snapshots.first()!!.active)
        }
    }

    @Test
    fun importsAreValidatedAndCopiedToPrivateStorage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wallpapers = AndroidWallpaperStore(context)
        try {
            wallpapers.importImage(
                android.net.Uri.parse("content://fr.deuspheara.oree.fixture/invalid"),
            )
            fail("Invalid image accepted")
        } catch (_: IOException) {}
        val name =
            wallpapers.importImage(
                android.net.Uri.parse("content://fr.deuspheara.oree.fixture/valid"),
            )
        try {
            assertTrue(File(context.filesDir, name).isFile)
            val frame =
                wallpapers.load(
                    LocalProfile(
                        "fixture",
                        "Fixture",
                        wallpaper = WallpaperMode.PERSONAL,
                        image = name,
                    ),
                )
            assertEquals(192, frame.image.width)
            assertEquals(108, frame.image.height)
            assertFalse(frame.image === frame.frost)
            val pixel = frame.image.getPixel(40, 40)
            assertTrue(kotlin.math.abs(android.graphics.Color.red(pixel) - 30) <= 4)
            assertTrue(kotlin.math.abs(android.graphics.Color.green(pixel) - 80) <= 4)
            assertTrue(kotlin.math.abs(android.graphics.Color.blue(pixel) - 120) <= 4)
        } finally {
            wallpapers.discard(name)
        }
        assertFalse(File(context.filesDir, name).exists())
    }

    @Test
    fun probeWatchNextFromOrdinaryApplicationCaller() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = WatchNextContentSource(context)
        assertEquals(ContentAvailability.DISABLED, repo.load(false).availability)
        val result = repo.load(true)
        android.util.Log.i(
            "LauncherContentProbe",
            "status=${result.availability}; localArt=${result.artwork != null}",
        )
        if (result.availability == ContentAvailability.AVAILABLE) {
            assertNotNull(result.engagedAt)
            assertTrue(
                result.artwork?.let(android.net.Uri::parse)?.scheme in
                    listOf("content", "android.resource"),
            )
        }
    }

    @Test
    fun homeWatchNextIsIndependentPerProfileAndLegacyDefaultsOff() = runBlocking {
        isolated { repo, store ->
            repo.initialize(LauncherPreferences(), AppOrganization())
            val id = repo.snapshots.first()!!.activeId
            assertFalse(repo.snapshots.first()!!.active!!.watchNextEnabled)
            repo.setWatchNextEnabled(id, true)
            assertTrue(repo.snapshots.first()!!.active!!.watchNextEnabled)
            assertFalse(repo.snapshots.first()!!.active!!.contentEnabled)
            repo.create("Other", "avatar_02")
            assertFalse(repo.snapshots.first()!!.active!!.watchNextEnabled)
            repo.select(id)
            assertTrue(repo.snapshots.first()!!.active!!.watchNextEnabled)
            store.edit { data ->
                val key = stringPreferencesKey("profiles_v1")
                val root = org.json.JSONObject(data[key]!!)
                val rows = root.getJSONArray("profiles")
                for (i in 0 until rows.length()) rows.getJSONObject(i).remove("watchNext")
                data[key] = root.toString()
            }
            assertFalse(repo.snapshots.first()!!.active!!.watchNextEnabled)
        }
    }
}
