package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.MAX_FAVORITE_APPS
import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.moveFavoriteApp
import fr.deuspheara.oree.core.model.orderFavoriteApps
import fr.deuspheara.oree.core.model.setFavoriteApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FavoriteAppsTest {
    @Test
    fun moveSkipsMissingAppsPreservesSlotsAndClampsEdges() {
        val original = listOf("A", "missing", "B", "C")
        val visible = setOf("A", "B", "C")
        val moved = moveFavoriteApp(original, "A", 1, visible)
        assertEquals(listOf("B", "missing", "A", "C"), moved)
        assertEquals(original, moveFavoriteApp(moved, "A", -1, visible))
        assertEquals(original, moveFavoriteApp(original, "A", -1, visible))
        assertEquals(original, moveFavoriteApp(original, "C", 1, visible))
        assertEquals(original, moveFavoriteApp(original, "missing", 1, visible))
    }

    @Test
    fun addIsIdempotentAndRemovePreservesOtherPositions() {
        val first = setFavoriteApp(listOf("B", "A"), "C", true)
        assertEquals(listOf("B", "A", "C"), first)
        assertEquals(first, setFavoriteApp(first, "A", true))
        assertEquals(listOf("B", "C"), setFavoriteApp(first, "A", false))
    }

    @Test
    fun missingPackageDoesNotChangeStoredOrderOrCreatePhantomCard() {
        val stored = listOf("B", "missing", "A", "B")
        val apps = listOf("A", "B", "C").map { TvApp(it, it, it, 0) }
        assertEquals(listOf("B", "A"), orderFavoriteApps(apps, stored).map { it.id })
        assertEquals(listOf("B", "missing", "A", "B"), stored)
    }

    @Test
    fun fullListCanRemoveOrRepeatButCannotGrow() {
        val full = (1..MAX_FAVORITE_APPS).map { "app$it" }
        assertEquals(full, setFavoriteApp(full, "app1", true))
        assertEquals(MAX_FAVORITE_APPS - 1, setFavoriteApp(full, "app1", false).size)
        assertThrows(IllegalArgumentException::class.java) { setFavoriteApp(full, "extra", true) }
        assertThrows(IllegalArgumentException::class.java) { setFavoriteApp(full, "\n", true) }
    }
}
