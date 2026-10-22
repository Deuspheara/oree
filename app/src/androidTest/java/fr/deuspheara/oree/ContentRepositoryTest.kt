package fr.deuspheara.oree

import android.database.MatrixCursor
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.ContentAvailability
import fr.deuspheara.oree.core.platform.datasource.content.WatchNextContentSource
import fr.deuspheara.oree.core.platform.launch.AppUninstall
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentRepositoryTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun row(id: Long, time: Long = System.currentTimeMillis(), kind: Int = 0, art: String = "https://example.org/poster") =
        arrayOf<Any?>(
            id,
            "Episode $id",
            "org.example.player",
            art,
            time,
            kind,
            "player://episode/$id",
        )

    @Test
    fun boundedFeedSortsRecentContinueNextAndRetainsRemoteTitles() = runBlocking {
        val now = System.currentTimeMillis()
        val repo =
            WatchNextContentSource(context) { fields, _ ->
                MatrixCursor(fields).apply {
                    for (i in 1L..220L) addRow(row(i, now - i * 1000))
                }
            }
        val feed = repo.loadFeed(true)
        assertEquals(ContentAvailability.AVAILABLE, feed.availability)
        assertEquals(20, feed.entries.size)
        assertEquals("org.example.player:1", feed.entries.first().id)
        assertNull(feed.entries.first().artwork)
        assertEquals("Episode 1", feed.entries.first().title)
    }

    @Test
    fun filtersStaleFutureAndNonPlaybackRowsAndAcceptsLocalArt() = runBlocking {
        val now = System.currentTimeMillis()
        val repo =
            WatchNextContentSource(context) { fields, _ ->
                MatrixCursor(fields).apply {
                    addRow(row(1, now - 31L * 24 * 60 * 60 * 1000))
                    addRow(row(2, now + 600_000))
                    addRow(row(3, kind = 2))
                    addRow(row(4, kind = 1, art = "content://example/poster"))
                }
            }
        val feed = repo.loadFeed(true)
        assertEquals(listOf("org.example.player:4"), feed.entries.map { it.id })
        assertEquals("content", android.net.Uri.parse(feed.entries.single().artwork!!).scheme)
    }

    @Test
    fun permissionEmptyUnsupportedErrorAndDisabledAreHonest() = runBlocking {
        val denied = WatchNextContentSource(context) { _, _ -> throw SecurityException() }
        assertEquals(ContentAvailability.DISABLED, denied.loadFeed(false).availability)
        assertEquals(ContentAvailability.DENIED, denied.loadFeed(true).availability)
        assertEquals(
            ContentAvailability.UNSUPPORTED,
            WatchNextContentSource(context) { _, _ -> null }.loadFeed(true).availability,
        )
        assertEquals(
            ContentAvailability.ERROR,
            WatchNextContentSource(context) { _, _ ->
                throw IllegalStateException()
            }
                .loadFeed(true)
                .availability,
        )
        assertEquals(
            ContentAvailability.EMPTY,
            WatchNextContentSource(context) { f, _ -> MatrixCursor(f) }.loadFeed(true).availability,
        )
    }

    @Test
    fun cancellationCancelsTheProviderSignalWithoutPublishing() = runBlocking {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        var published = false
        val repo =
            WatchNextContentSource(context) { _, signal ->
                signal.setOnCancelListener { cancelled.countDown() }
                started.countDown()
                check(cancelled.await(5, TimeUnit.SECONDS))
                null
            }
        val job =
            launch(Dispatchers.Default) {
                repo.loadFeed(true)
                published = true
            }
        assertTrue(started.await(5, TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertEquals(0, cancelled.count)
        assertFalse(published)
    }

    @Test
    fun selfAndProtectedHomePackagesAreNeverUninstallable() = runBlocking {
        val uninstall = AppUninstall(context)
        for (
        pkg in
        listOf(
            context.packageName,
            "android",
            "com.omeda.arc",
            "com.omeda.arc.local",
            "com.spocky.projengmenu",
        )
        ) {
            assertNull(pkg, uninstall.intent(pkg))
        }
        assertNotNull(
            "Separate instrumentation fixture is an ordinary removable app",
            uninstall.intent(InstrumentationRegistry.getInstrumentation().context.packageName),
        )
    }
}
