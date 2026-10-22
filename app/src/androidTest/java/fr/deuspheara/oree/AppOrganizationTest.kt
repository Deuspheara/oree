package fr.deuspheara.oree

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.data.migration.DataStoreLegacyOrganization
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppOrganizationTest {
    @Test
    fun orderedFavoritesPersistAcrossStoreReopening() = runBlocking {
        val file =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "${UUID.randomUUID()}.preferences_pb",
            )
        var job = SupervisorJob()
        var store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { file },
            )
        try {
            val repo = DataStoreLegacyOrganization(store)
            repo.setFavorite("example/Bravo", true)
            repo.setFavorite("example/Alpha", true)
            repo.setFavorite("example/Bravo", true)
            job.cancelAndJoin()
            job = SupervisorJob()
            store =
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(job + Dispatchers.IO),
                    produceFile = { file },
                )
            assertEquals(
                listOf("example/Bravo", "example/Alpha"),
                DataStoreLegacyOrganization(store).organization.first().favorites,
            )
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }

    @Test
    fun malformedStoredLayoutStaysIntactWhenAChangeIsAttempted() = runBlocking {
        val file =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
                "${UUID.randomUUID()}.preferences_pb",
            )
        val job = SupervisorJob()
        val store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { file },
            )
        val key = stringPreferencesKey("favorite_components_v1")
        try {
            store.edit { it[key] = "[broken" }
            val repo = DataStoreLegacyOrganization(store)
            assertTrue(repo.organization.first().failed)
            try {
                repo.setFavorite("example/Alpha", true)
                fail("An unreadable layout must not be overwritten")
            } catch (_: IOException) {}
            assertEquals("[broken", store.data.first()[key])
        } finally {
            job.cancelAndJoin()
            file.delete()
        }
    }
}
