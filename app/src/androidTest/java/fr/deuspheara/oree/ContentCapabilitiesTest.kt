package fr.deuspheara.oree

import android.database.MatrixCursor
import androidx.test.platform.app.InstrumentationRegistry
import fr.deuspheara.oree.core.model.ContentSourceId
import fr.deuspheara.oree.core.model.ProbeAvailability
import fr.deuspheara.oree.core.platform.datasource.content.AndroidContentCapabilitySource
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentCapabilitiesTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyDeniedUnsupportedAndFailedQueriesRemainDistinct() = runBlocking {
        val source = ContentSourceId.WATCH_NEXT
        assertEquals(
            ProbeAvailability.EMPTY,
            AndroidContentCapabilitySource(context) { _, fields, _ ->
                MatrixCursor(fields)
            }
                .inspect(source)
                .availability,
        )
        assertEquals(
            ProbeAvailability.DENIED,
            AndroidContentCapabilitySource(context) { _, _, _ ->
                throw SecurityException()
            }
                .inspect(source)
                .availability,
        )
        assertEquals(
            ProbeAvailability.UNSUPPORTED,
            AndroidContentCapabilitySource(context) { _, _, _ ->
                null
            }
                .inspect(source)
                .availability,
        )
        assertEquals(
            ProbeAvailability.ERROR,
            AndroidContentCapabilitySource(context) { _, _, _ ->
                throw IllegalStateException()
            }
                .inspect(source)
                .availability,
        )
    }

    @Test
    fun samplesAreBoundedAndUnsafeArtworkAndDestinationsAreRejected() = runBlocking {
        var closed = false
        val probe =
            AndroidContentCapabilitySource(context) { _, fields, _ ->
                object : MatrixCursor(fields) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
                    .apply {
                        repeat(80) { index ->
                            addRow(
                                arrayOf<Any>(
                                    index,
                                    "org.example.source",
                                    "Title $index",
                                    "file:///outside.jpg",
                                    "javascript:bad",
                                ),
                            )
                        }
                    }
            }
        val report = probe.inspect(ContentSourceId.WATCH_NEXT)
        assertEquals(ProbeAvailability.AVAILABLE, report.availability)
        assertEquals(40, report.rows)
        assertEquals(40, report.metadata)
        assertEquals(40, report.crossPublisher)
        assertEquals(0, report.localArtwork)
        assertEquals(0, report.launchable)
        assertNull(report.artworkReadable)
        assertTrue(report.bounded)
        assertTrue(closed)
    }

    @Test
    fun actualOrdinaryCallerReportContainsOnlyAggregateEvidence() = runBlocking {
        assertEquals("fr.deuspheara.oree", context.packageName)
        assertEquals(context.applicationInfo.uid, android.os.Process.myUid())
        val report = AndroidContentCapabilitySource(context).inspect()
        assertEquals(ContentSourceId.entries.toSet(), report.providers.map { it.source }.toSet())
        val json =
            JSONObject()
                .put("sdk", android.os.Build.VERSION.SDK_INT)
                .put("model", android.os.Build.MODEL)
                .put("hardware", android.os.Build.HARDWARE)
                .put("caller", "ordinary target application UID")
                .put("listingsPermission", report.listingsPermission)
                .put("systemSearch", report.systemSearch)
                .put("searchableCandidates", report.searchableCandidates)
                .put("exportedSearchCandidates", report.exportedSearchCandidates)
                .put("searchableAvailability", report.searchableAvailability.name)
                .put(
                    "sources",
                    JSONArray().apply {
                        report.providers.forEach { p ->
                            put(
                                JSONObject()
                                    .put("source", p.source.name)
                                    .put("availability", p.availability.name)
                                    .put("rows", p.rows)
                                    .put("metadata", p.metadata)
                                    .put("localArtwork", p.localArtwork)
                                    .put("artworkReadable", p.artworkReadable ?: JSONObject.NULL)
                                    .put("launchable", p.launchable)
                                    .put("crossPublisher", p.crossPublisher)
                                    .put("bounded", p.bounded),
                            )
                        }
                    },
                )
        File(context.cacheDir, "content-capability-report.json").writeText(json.toString(2))
    }
}
