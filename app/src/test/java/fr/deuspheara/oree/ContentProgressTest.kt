package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.ContentEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentProgressTest {
    @Test
    fun onlyValidSuppliedPlaybackMetadataProducesProgress() {
        val entry = ContentEntry("id", "Title", "org.example.player", null, "player://item", 1L)
        assertNull(entry.playbackProgress)
        for ((duration, position) in listOf(0L to 1L, -1L to 0L, 10L to -1L, 10L to 11L)) {
            assertNull(entry.copy(durationMillis = duration, playbackPositionMillis = position).playbackProgress)
        }
        assertEquals(0f, entry.copy(durationMillis = 100, playbackPositionMillis = 0).playbackProgress)
        assertEquals(.25f, entry.copy(durationMillis = 100, playbackPositionMillis = 25).playbackProgress)
        assertEquals(1f, entry.copy(durationMillis = 100, playbackPositionMillis = 100).playbackProgress)
    }
}
