package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ArtworkIdentityTest {
    @Test
    fun packageUpdateInvalidatesSameResourceId() {
        val original = TvApp("a/.Main", "a", "App", 123, 100)
        val updated = original.copy(artworkVersion = 101)
        assertNotEquals(original.artworkCacheKey, updated.artworkCacheKey)
        assertEquals(original.id, updated.id)
    }

    @Test
    fun unrelatedLabelChangeDoesNotDuplicateArtwork() {
        val original = TvApp("a/.Main", "a", "App", 123, 100)
        assertEquals(original.artworkCacheKey, original.copy(label = "Translated").artworkCacheKey)
    }
}
