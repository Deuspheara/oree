package fr.deuspheara.oree

import fr.deuspheara.oree.core.model.TvApp
import fr.deuspheara.oree.core.model.restoreAppId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusRestorationTest {
    private fun app(id: String) = TvApp(id, id, id, 0)

    @Test
    fun stableIdentitySurvivesReordering() {
        assertEquals("b", restoreAppId("b", 1, listOf(app("b"), app("a"))))
    }

    @Test
    fun deletedItemUsesPreviousPosition() {
        assertEquals("c", restoreAppId("b", 1, listOf(app("a"), app("c"))))
    }

    @Test
    fun removedTailChoosesNearestSurvivor() {
        assertEquals("b", restoreAppId("c", 2, listOf(app("a"), app("b"))))
    }

    @Test
    fun emptyCatalogHasNoTarget() {
        assertNull(restoreAppId("a", 0, emptyList()))
    }

    @Test
    fun initialSelectionUsesFirstApp() {
        assertEquals("a", restoreAppId(null, 0, listOf(app("a"), app("b"))))
    }

    @Test
    fun invalidNegativeIndexIsClamped() {
        assertEquals("a", restoreAppId("gone", -5, listOf(app("a"), app("b"))))
    }
}
